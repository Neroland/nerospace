package za.co.neroland.nerospace.solar;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * A solar panel: a low, sun-tracking power generator that pools with adjacent same-tier panels into a
 * {@link SolarArray}. The block itself is a flat slab (collision + base model); the tilting,
 * sun-following, night-folding panel surface is drawn by the block-entity renderer. Energy is exposed
 * on every side (output ports), so any face feeds a pipe or machine from the shared array pool.
 */
public class SolarPanelBlock extends BaseEntityBlock {

    public static final MapCodec<SolarPanelBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    SolarTier.CODEC.fieldOf("tier").forGetter(SolarPanelBlock::tier),
                    propertiesCodec()
            ).apply(instance, SolarPanelBlock::new));

    /** Flat 4px slab — the panel housing; the tilting surface above it is renderer-only. */
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);

    private final SolarTier tier;

    public SolarPanelBlock(SolarTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public SolarTier tier() {
        return this.tier;
    }

    @Override
    protected MapCodec<SolarPanelBlock> codec() {
        return CODEC;
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
