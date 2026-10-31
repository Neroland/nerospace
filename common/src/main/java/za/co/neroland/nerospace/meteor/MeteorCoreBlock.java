package za.co.neroland.nerospace.meteor;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * The Meteor Core (meteor-events design §5): the glowing block at the centre of a crater that holds
 * the meteor's RNG loot. Break-to-loot — the stored stacks spill when the core is removed, driven by
 * {@link MeteorCoreBlockEntity#preRemoveSideEffects} (the block has no loot table, so the rolled
 * contents survive rather than a fresh roll).
 */
public class MeteorCoreBlock extends BaseEntityBlock {

    public static final MapCodec<MeteorCoreBlock> CODEC = BlockCodecs.simple(MeteorCoreBlock::new);

    public MeteorCoreBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<MeteorCoreBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MeteorCoreBlockEntity(pos, state);
    }

    //? if <1.21.5 {
    /*// Before 1.21.5 there is no BlockEntity.preRemoveSideEffects hook: run it from the block when it is replaced.
    @Override
    protected void onRemove(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MeteorCoreBlockEntity be) {
            be.preRemoveSideEffects(pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
    *///?}
}
