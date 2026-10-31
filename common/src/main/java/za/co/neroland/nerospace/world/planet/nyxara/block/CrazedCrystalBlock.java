package za.co.neroland.nerospace.world.planet.nyxara.block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.PlacedFlagBlock;
import za.co.neroland.nerospace.block.rules.ShatterRules;
import za.co.neroland.nerospace.rocket.RocketEntity;

/**
 * Crazed Crystal (Nyxara dayside): heat-crazed safety glass. An impact from 3 or more blocks (a living entity, a
 * falling block or a rocket) shatters the natural block it lands on and the connected natural Crazed Crystal around
 * it, 8 blocks per impact at most, into shards; the faller takes half the fall damage. Player-placed blocks
 * ({@code PLACED}) never shatter, and the cascade never touches any other block.
 */
public class CrazedCrystalBlock extends PlacedFlagBlock {

    public static final MapCodec<CrazedCrystalBlock> CODEC = BlockCodecs.simple(CrazedCrystalBlock::new);

    public CrazedCrystalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends CrazedCrystalBlock> codec() {
        return CODEC;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        boolean impactor = entity instanceof LivingEntity || entity instanceof FallingBlockEntity
                || entity instanceof RocketEntity;
        if (!(level instanceof ServerLevel server)
                || !ShatterRules.shatters(fallDistance, impactor, playerPlaced(state))) {
            super.fallOn(level, state, pos, entity, fallDistance);
            return;
        }
        entity.causeFallDamage(fallDistance, ShatterRules.fallDamageFactor(), entity.damageSources().fall());
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 1.0,
                pos.getZ() + 0.5, ShatterRules.PARTICLES, 0.4, 0.1, 0.4, 0.05);
        server.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
        for (BlockPos p : cascade(server, pos)) {
            server.destroyBlock(p, true, entity);
        }
    }

    /** The natural Crazed Crystal broken by one impact at {@code start}: a flood fill, {@link ShatterRules#CAP} max. */
    private List<BlockPos> cascade(ServerLevel level, BlockPos start) {
        List<BlockPos> out = new ArrayList<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        out.add(start);
        queue.add(start);
        Direction[] dirs = Direction.values();
        while (!queue.isEmpty() && out.size() < ShatterRules.CAP) {
            BlockPos at = queue.poll();
            boolean[] crazed = new boolean[dirs.length];
            for (int i = 0; i < dirs.length; i++) {
                BlockPos n = at.relative(dirs[i]);
                BlockState s = level.getBlockState(n);
                crazed[i] = !out.contains(n) && s.is(this) && !playerPlaced(s);
            }
            boolean[] take = ShatterRules.cascade(crazed, ShatterRules.CAP - out.size());
            for (int i = 0; i < dirs.length; i++) {
                if (take[i]) {
                    BlockPos n = at.relative(dirs[i]).immutable();
                    out.add(n);
                    queue.add(n);
                }
            }
        }
        return out;
    }
}
