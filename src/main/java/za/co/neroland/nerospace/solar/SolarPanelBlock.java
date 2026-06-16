package za.co.neroland.nerospace.solar;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * A solar panel that pools with adjacent same-tier panels into a {@link SolarArray}. Tier 1 is a single
 * 1×1 block; Tier 2 (2×2) and Tier 3 (3×3) are multiblocks: placing the item fills the whole N×N
 * footprint with one {@link #ANCHOR} cell at the clicked min-corner plus filler cells, all powered as a
 * single unit. Breaking any cell tears the whole unit down and returns one item.
 *
 * <p>The block is a flat slab (collision + base model); the tilting, sun-tracking, night-folding deck is
 * drawn by the block-entity renderer (only on the anchor for multiblocks). Energy is exposed on every
 * side (output ports), so any face feeds a pipe or machine from the shared array pool.</p>
 */
public class SolarPanelBlock extends BaseEntityBlock {

    public static final MapCodec<SolarPanelBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    SolarTier.CODEC.fieldOf("tier").forGetter(SolarPanelBlock::tier),
                    propertiesCodec()
            ).apply(instance, SolarPanelBlock::new));

    /** True on the unit's min-corner cell — the only cell that drops the item and renders the deck. */
    public static final BooleanProperty ANCHOR = BooleanProperty.create("anchor");

    /** Flat 4px slab — the panel housing; the tilting surface above it is renderer-only. */
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);

    /** Re-entrancy guard so cascading a multiblock teardown doesn't recurse or double-drop. */
    private static boolean tearingDown = false;

    private final SolarTier tier;

    @SuppressWarnings("this-escape") // registerDefaultState is the idiomatic constructor wiring
    public SolarPanelBlock(SolarTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        // Default to an anchor so a raw setBlock (e.g. the /nerospace gallery) places a working unit;
        // filler cells are explicitly set to false during multiblock placement.
        registerDefaultState(this.stateDefinition.any().setValue(ANCHOR, true));
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

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarPanelBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
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
        BlockState anchor = defaultBlockState().setValue(ANCHOR, true);
        int n = this.tier.footprint;
        if (n <= 1) {
            return anchor;
        }
        // The clicked cell is already validated by the item; require the rest of the N×N footprint clear.
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
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide() || this.tier.footprint <= 1 || !state.getValue(ANCHOR)) {
            return;
        }
        int n = this.tier.footprint;
        BlockState part = defaultBlockState().setValue(ANCHOR, false);
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
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
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
                if (player == null || !player.getAbilities().instabuild) {
                    Block.popResource(level, anchor, new ItemStack(this));
                }
            } finally {
                tearingDown = false;
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof SolarPanelBlockEntity panel) {
            serverPlayer.sendSystemMessage(Component.translatable("block.nerospace.solar_panel.readout",
                    panel.getEnergyHandler().getAmountAsInt(), panel.getEnergyHandler().getCapacityAsInt(),
                    panel.arraySize()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof SolarPanelBlockEntity panel ? panel.comparatorSignal() : 0;
    }
}
