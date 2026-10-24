package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 002 §1.6 Gravity Shard, §1.7 Gravity Plate, §10.4. */
@SuppressWarnings("unused")
class BlockGravityRulesTest {

    private static BlockGravityRules.Override shard() {
        return new BlockGravityRules.Override(BlockGravityRules.Kind.SHARD, BlockGravityRules.SHARD_FACTOR);
    }

    private static BlockGravityRules.Override plate() {
        return new BlockGravityRules.Override(BlockGravityRules.Kind.PLATE, BlockGravityRules.PLATE_FACTOR);
    }

    @Test
    @DisplayName("no override → the dimension factor")
    void none() {
        assertEquals(0.1, BlockGravityRules.factor(0.1, List.of()));
        assertEquals(0.4, BlockGravityRules.factor(0.4, null));
    }

    @Test
    @DisplayName("shard −0.15, plate 1.0, plate beats shard regardless of order")
    void priority() {
        assertEquals(-0.15, BlockGravityRules.factor(0.1, List.of(shard())));
        assertEquals(1.0, BlockGravityRules.factor(0.1, List.of(plate())));
        assertEquals(1.0, BlockGravityRules.factor(0.1, List.of(shard(), plate())));
        assertEquals(1.0, BlockGravityRules.factor(0.1, List.of(plate(), shard(), shard())));
    }

    @Test
    @DisplayName("same kind → lowest factor")
    void lowest() {
        var a = new BlockGravityRules.Override(BlockGravityRules.Kind.SHARD, -0.15);
        var b = new BlockGravityRules.Override(BlockGravityRules.Kind.SHARD, -0.3);
        assertEquals(-0.3, BlockGravityRules.factor(1.0, List.of(a, b)));
        assertEquals(-0.3, BlockGravityRules.factor(1.0, List.of(b, a)));
    }

    @Test
    @DisplayName("shard column 3x5x3 above; plate column 1x6x1 above")
    void columns() {
        int shardCells = 0;
        int plateCells = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 9; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    shardCells += BlockGravityRules.inColumn(dx, dy, dz, BlockGravityRules.Kind.SHARD) ? 1 : 0;
                    plateCells += BlockGravityRules.inColumn(dx, dy, dz, BlockGravityRules.Kind.PLATE) ? 1 : 0;
                }
            }
        }
        assertEquals(3 * 5 * 3, shardCells);
        assertEquals(6, plateCells);
        assertFalse(BlockGravityRules.inColumn(0, 0, 0), "not the block itself");
        assertTrue(BlockGravityRules.inColumn(1, 5, -1));
        assertFalse(BlockGravityRules.inColumn(0, 6, 0));
        assertTrue(BlockGravityRules.inColumn(0, 6, 0, BlockGravityRules.Kind.PLATE));
        assertFalse(BlockGravityRules.inColumn(1, 1, 0, BlockGravityRules.Kind.PLATE));
    }

    @Test
    @DisplayName("64 per chunk; config may lower; kinds persist by name")
    void cap() {
        assertTrue(BlockGravityRules.canRegister(63, 64));
        assertFalse(BlockGravityRules.canRegister(64, 64));
        assertFalse(BlockGravityRules.canRegister(64, 1000));
        assertFalse(BlockGravityRules.canRegister(8, 8));
        assertEquals(BlockGravityRules.Kind.PLATE, BlockGravityRules.Kind.byName("PLATE"));
        assertNull(BlockGravityRules.Kind.byName("nope"));
    }
}
