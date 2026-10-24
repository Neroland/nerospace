package za.co.neroland.nerospace.block;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.IgniteRules;

/**
 * Flammable crust (Sulfur Crust, 002 §1.3): when a fire block appears next to it, each other flammable neighbour gets
 * fire above it (≤ 5 per trigger) — and the crust burns to {@code burnsTo}. No chaining through another crust in the
 * same tick (a crust neighbour is never a target). Flammability is registered by Stage 3 with the loader's flammable
 * hook (30 / 60, {@link IgniteRules}).
 */
public class IgnitingCrustBlock extends Block {

    public static final MapCodec<IgnitingCrustBlock> CODEC = BlockCodecs.simple(p -> new IgnitingCrustBlock(p, null));

    private final Supplier<BlockState> burnsTo;

    public IgnitingCrustBlock(Properties properties, Supplier<BlockState> burnsTo) {
        super(properties);
        this.burnsTo = burnsTo;
    }

    protected MapCodec<? extends IgnitingCrustBlock> codec() {
        return CODEC;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, orientation, movedByPiston);
        if (level.isClientSide()) {
            return;
        }
        boolean fire = false;
        for (Direction d : Direction.values()) {
            if (level.getBlockState(pos.relative(d)).is(BlockTags.FIRE)) {
                fire = true;
                break;
            }
        }
        if (!fire) {
            return;
        }
        boolean[] flammable = new boolean[6];
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            BlockState ns = level.getBlockState(n);
            flammable[d.get3DDataValue()] = !(ns.getBlock() instanceof IgnitingCrustBlock) && !ns.isAir()
                    && !ns.is(BlockTags.FIRE) && ns.ignitedByLava() && level.getBlockState(n.above()).isAir();
        }
        int mask = IgniteRules.targets(flammable, IgniteRules.MAX_TARGETS);
        for (Direction d : Direction.values()) {
            if ((mask & (1 << d.get3DDataValue())) != 0) {
                BlockPos above = pos.relative(d).above();
                level.setBlockAndUpdate(above, BaseFireBlock.getState(level, above));
            }
        }
        if (this.burnsTo != null) {
            level.setBlockAndUpdate(pos, this.burnsTo.get());
        }
    }
}
