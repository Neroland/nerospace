package za.co.neroland.nerospace.machine;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * A solar panel that pools with adjacent same-tier panels into a {@link SolarArray}. Tier 1 is a single
 * 1×1 block; Tier 2 (2×2) and Tier 3 (3×3) are multiblocks: placing the item fills the whole N×N
 * footprint with one {@link #ANCHOR} cell at the clicked min-corner plus filler cells, all powered as a
 * single unit (the renderer draws one big sun-tracking deck on the anchor). Breaking any cell tears the
 * whole unit down and returns one item. Energy is exposed on every side (filler cells forward to the
 * anchor's buffer), so any face feeds a pipe or machine from the shared array pool.
 *
 * <p>Cross-loader port: tier-aware (the standalone single-tier block is generalised); the multiblock
 * placement/teardown is plain vanilla block API.</p>
 */
public class SolarPanelBlock extends BaseEntityBlock {

    public static final MapCodec<SolarPanelBlock> CODEC = NerospaceCommon.requireNonNull(
            RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    SolarTier.CODEC.fieldOf("tier").forGetter(SolarPanelBlock::tier),
                    propertiesCodec()
            ).apply(instance, (tier, properties) -> new SolarPanelBlock(NerospaceCommon.requireNonNull(tier),
                    properties))));

    /** True on the unit's min-corner cell — the only cell that drops the item and renders the deck. */
    public static final BooleanProperty ANCHOR = BooleanProperty.create("anchor");

    /** Re-entrancy guard so cascading a multiblock teardown doesn't recurse or double-drop. */
    private static boolean tearingDown = false;

    private final SolarTier tier;

    @SuppressWarnings("this-escape") // registerDefaultState is the idiomatic constructor wiring
    public SolarPanelBlock(SolarTier tier, Properties properties) {
        super(NerospaceCommon.requireNonNull(properties));
        this.tier = NerospaceCommon.requireNonNull(tier);
        // Default to an anchor so a raw setBlock places a working unit; fillers are set false on placement.
        registerDefaultState(NerospaceCommon.requireNonNull(
                this.stateDefinition.any().setValue(ANCHOR, true)));
    }

    public SolarTier tier() {
        return this.tier;
    }

    @Override
    protected MapCodec<SolarPanelBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ANCHOR);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarPanelBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.SOLAR_PANEL.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    // --- Multiblock placement -------------------------------------------------

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState anchor = java.util.Objects.requireNonNull(
                defaultBlockState().setValue(ANCHOR, true));
        int n = this.tier.footprint;
        if (n <= 1) {
            return anchor;
        }
        // The clicked cell is validated by the item; require the rest of the N×N footprint clear.
        Level level = context.getLevel();
        BlockPos origin = context.getClickedPos();
        for (int dx = 0; dx < n; dx++) {
            for (int dz = 0; dz < n; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (!level.getBlockState(origin.offset(dx, 0, dz)).canBeReplaced(context)) {
                    return null; // footprint blocked — cancel the placement
                }
            }
        }
        return anchor;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos,
            BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide() || this.tier.footprint <= 1 || !state.getValue(ANCHOR)) {
            return;
        }
        int n = this.tier.footprint;
        BlockState part = java.util.Objects.requireNonNull(
                defaultBlockState().setValue(ANCHOR, false));
        for (int dx = 0; dx < n; dx++) {
            for (int dz = 0; dz < n; dz++) {
                BlockPos cell = pos.offset(dx, 0, dz);
                if (!cell.equals(pos)) {
                    BlockState existing = level.getBlockState(cell);
                    if (existing.getBlock() != this && existing.canBeReplaced()) {
                        level.setBlock(cell, part, Block.UPDATE_CLIENTS);
                    }
                }
                if (level.getBlockEntity(cell) instanceof SolarPanelBlockEntity be) {
                    be.setAnchor(pos); // anchor cell -> self; fillers -> the anchor
                }
            }
        }
    }

    // --- Multiblock teardown --------------------------------------------------

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state,
            Player player) {
        if (this.tier.footprint > 1 && !level.isClientSide() && !tearingDown) {
            BlockPos anchor = level.getBlockEntity(pos) instanceof SolarPanelBlockEntity be ? be.anchorPos() : pos;
            tearingDown = true;
            try {
                int n = this.tier.footprint;
                for (int dx = 0; dx < n; dx++) {
                    for (int dz = 0; dz < n; dz++) {
                        BlockPos cell = anchor.offset(dx, 0, dz);
                        if (!cell.equals(pos) && level.getBlockState(cell).getBlock() == this) {
                            level.removeBlock(cell, false); // sibling cell — no drops
                        }
                    }
                }
                // One item back for the whole unit (the broken cell's own loot is empty for multiblocks).
                if (!player.getAbilities().instabuild) {
                    Block.popResource(level, java.util.Objects.requireNonNull(anchor), new ItemStack(this));
                }
            } finally {
                tearingDown = false;
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos,
            Direction direction) {
        return level.getBlockEntity(pos) instanceof SolarPanelBlockEntity panel ? panel.comparatorSignal() : 0;
    }
}
