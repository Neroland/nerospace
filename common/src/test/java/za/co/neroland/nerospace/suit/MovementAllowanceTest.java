package za.co.neroland.nerospace.suit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MovementAllowanceTest {

    @AfterEach
    void clean() {
        MovementAllowance.clearAll();
    }

    @Test
    void offByDefaultAndCapped() {
        Object player = new Object();
        assertFalse(MovementAllowance.active(player));
        assertFalse(MovementAllowance.withinCap(player, 0.1));
        MovementAllowance.activate(player, 0.6, false);
        assertTrue(MovementAllowance.active(player));
        assertTrue(MovementAllowance.withinCap(player, 0.6));
        assertFalse(MovementAllowance.withinCap(player, 0.61));
        MovementAllowance.clear(player);
        assertFalse(MovementAllowance.active(player));
    }
}
