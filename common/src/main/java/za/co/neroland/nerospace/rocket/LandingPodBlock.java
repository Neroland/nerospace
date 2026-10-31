package za.co.neroland.nerospace.rocket;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * Planet/Home return-site crate with an inflated arrival shell. Like the launch pad and the Docking Ring, breaking it
 * unregisters its {@code POD} node, so stale nodes never burn the pod cap or the pod-spacing probe.
 */
public class LandingPodBlock extends ReturnSiteBlock {

    public static final MapCodec<LandingPodBlock> CODEC = BlockCodecs.simple(LandingPodBlock::new);

    public LandingPodBlock(Properties properties) {
        super(properties, true);
    }

    protected MapCodec<LandingPodBlock> codec() {
        return CODEC;
    }

    //? if >=1.21.5 {
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
    //?} else {
    /*@Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level world, BlockPos pos, BlockState newState,
            boolean movedByPiston) {
        super.onRemove(state, world, pos, newState, movedByPiston);
        if (!(world instanceof ServerLevel level) || state.is(newState.getBlock())) {
            return;
        }
    *///?}
        if (!(level.getBlockState(pos).getBlock() instanceof LandingPodBlock)) {
            PadRegistry registry = PadRegistry.get(level.getServer());
            PadRegistry.PadNode node = registry.at(level.dimension(), pos);
            if (node != null && node.kind() == PadKind.POD) {
                registry.unregisterAt(level.dimension(), pos);
            }
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
