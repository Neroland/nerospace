package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.junit.jupiter.api.Test;

/** 005 §1.6: legacy pad centres and fronts. */
class LandingPadGeometryTest {

    static Set<BlockPos> square(int x0, int z0, int size) {
        Set<BlockPos> out = new HashSet<>();
        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                out.add(new BlockPos(x0 + dx, 64, z0 + dz));
            }
        }
        return out;
    }

    @Test
    void cornerOf3x3ResolvesToCentre() {
        assertEquals(new BlockPos(11, 64, 21), LandingPadGeometry.centreOf(square(10, 20, 3), new BlockPos(10, 64, 20)));
        assertEquals(1, LandingPadGeometry.halfSize(square(10, 20, 3), new BlockPos(10, 64, 20)));
    }

    @Test
    void fiveByFiveWins() {
        assertEquals(new BlockPos(2, 64, 2), LandingPadGeometry.centreOf(square(0, 0, 5), new BlockPos(4, 64, 4)));
    }

    @Test
    void loneBlockIsItself() {
        BlockPos p = new BlockPos(7, 64, 7);
        assertEquals(p, LandingPadGeometry.centreOf(Set.of(p), p));
    }

    @Test
    void apronFacing() {
        // N, E, S, W: corridor west (closed), void east (open) → EAST, as the station airlock pad.
        assertEquals(Direction.EAST, LandingPadGeometry.facingOf(new boolean[] { false, true, false, false }));
        assertEquals(Direction.SOUTH, LandingPadGeometry.facingOf(new boolean[] { true, true, true, true }));
        assertEquals(Direction.SOUTH, LandingPadGeometry.facingOf(new boolean[] { false, false, false, false }));
        assertEquals(Direction.NORTH, LandingPadGeometry.facingOf(new boolean[] { true, false, false, false }));
    }
}
