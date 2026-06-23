package za.co.neroland.nerospace.machine.quarry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Block entity for a {@link QuarryLandmarkBlock}: it exists so the client can animate the Nerospace
 * marker lasers — a glowing vertical beam plus marching projection dots along the four horizontal
 * axes (the "links" the controller scans). Purely cosmetic; no server state.
 */
public class QuarryLandmarkBlockEntity extends BlockEntity {

    public QuarryLandmarkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUARRY_LANDMARK.get(), pos, state);
    }

    /** Client-only cosmetic tick: project the animated marker lasers. */
    public void clientTick(Level level, BlockPos pos, BlockState state) {
        long time = level.getGameTime();
        if ((time & 3L) != 0L) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        // Vertical beam.
        double rise = (time % 16L) * 0.06;
        for (int i = 0; i < 3; i++) {
            level.addParticle(ParticleTypes.END_ROD, x, y + i * 0.6 + rise, z, 0.0, 0.01, 0.0);
        }
        // Marching projection dots along each horizontal axis.
        double march = (time % 12L) * 0.4;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            level.addParticle(ParticleTypes.GLOW,
                    x + dir.getStepX() * march, y, z + dir.getStepZ() * march, 0.0, 0.0, 0.0);
        }
    }
}
