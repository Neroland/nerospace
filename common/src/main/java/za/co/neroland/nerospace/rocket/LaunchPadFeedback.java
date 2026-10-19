package za.co.neroland.nerospace.rocket;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Tells a player the moment a hand-built launch pad is ready. A pad formation has no block entity and
 * no GUI, so without this the only way to learn a pad had formed was to right-click it empty-handed.
 *
 * <p>Called after a player places a Launch Pad, Station Wall or Launch Gantry. When that block raised
 * the tier of a pad it touches, the player gets a line above the hotbar saying the pad is ready and
 * which rockets it takes, plus a chime and a burst of particles over the pad. Placements that change
 * nothing stay silent, so laying out a 5x5 does not spam the hotbar.</p>
 */
public final class LaunchPadFeedback {

    private LaunchPadFeedback() {
    }

    /** Announce a newly formed pad tier, if the block just placed at {@code pos} completed one. */
    public static void onPartPlaced(Level level, BlockPos pos, @Nullable LivingEntity placer) {
        if (!(level instanceof ServerLevel server) || !(placer instanceof ServerPlayer player)) {
            return;
        }
        int best = 0;
        Set<BlockPos> bestPads = Set.of();
        Set<BlockPos> seen = new HashSet<>();
        // A pad block belongs to its own cluster; a wall or gantry on a ring touches the pad it serves at
        // most one block away (diagonally at a corner), so a 3x3 scan finds every cluster it could affect.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos start = pos.offset(dx, 0, dz);
                if (seen.contains(start) || !(level.getBlockState(start).getBlock() instanceof RocketLaunchPadBlock)) {
                    continue;
                }
                Set<BlockPos> pads = LaunchPadMultiblock.connectedPads(level, start);
                seen.addAll(pads);
                int now = LaunchPadMultiblock.padTier(level, pads);
                if (now > best && now > LaunchPadMultiblock.padTierWithout(level, pads, pos)) {
                    best = now;
                    bestPads = pads;
                }
            }
        }
        if (best == 0) {
            return;
        }
        player.sendOverlayMessage(Component.translatable("block.nerospace.rocket_launch_pad.formed",
                RocketLaunchPadBlock.tierLabel(best)).withStyle(ChatFormatting.GREEN));

        double x = 0.0D;
        double z = 0.0D;
        for (BlockPos pad : bestPads) {
            x += pad.getX() + 0.5D;
            z += pad.getZ() + 0.5D;
        }
        x /= bestPads.size();
        z /= bestPads.size();
        double y = bestPads.iterator().next().getY() + RocketLaunchPadBlock.SURFACE_HEIGHT + 0.2D;
        double spread = best >= 4 ? 2.0D : (best >= 2 ? 1.2D : 0.4D);
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 12 + best * 6, spread, 0.15D, spread, 0.0D);
        server.playSound(null, x, y, z, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6F, 1.0F + 0.1F * best);
    }
}
