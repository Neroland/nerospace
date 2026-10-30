package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.rocket.DescentMath.Phase;

/** The descent state machine numbers and transitions. */
class DescentStateTest {

    @Test
    void nominalSixtyTicks() {
        assertEquals(60, DescentMath.nominalTicks());
        assertEquals(48, DescentMath.DESCENT_HEIGHT);
    }

    @Test
    void stepsToTouchdownAndTimeout() {
        assertEquals(Phase.DESCENDING, DescentMath.step(10, 30.0D));
        assertEquals(Phase.TOUCHDOWN, DescentMath.step(60, 0.0D));
        assertEquals(Phase.ABORTED, DescentMath.step(DescentMath.DESCENT_TIMEOUT, 12.0D));
        assertEquals(Phase.DESCENDING, DescentMath.step(DescentMath.DESCENT_TIMEOUT - 1, 12.0D));
    }

    @Test
    void midDescentLoadFinishesAtOnce() {
        assertEquals(Phase.ABORTED, DescentMath.onLoad(Phase.DESCENDING));
        assertEquals(Phase.IDLE, DescentMath.onLoad(Phase.TOUCHDOWN));
        assertEquals(Phase.IDLE, DescentMath.onLoad(Phase.IDLE));
        // A 1.6.0 rocket has no Phase: it loads idle.
        assertEquals(Phase.IDLE, DescentMath.Phase.byName(""));
        assertEquals(Phase.IDLE, DescentMath.Phase.byName("BOGUS"));
    }

    @Test
    void padCheckedEveryTenTicks() {
        assertFalse(DescentMath.checkPadThisTick(0));
        assertFalse(DescentMath.checkPadThisTick(9));
        assertTrue(DescentMath.checkPadThisTick(10));
        assertTrue(DescentMath.checkPadThisTick(20));
    }

    @Test
    void reversedAnimationRunsFromFullToZero() {
        assertEquals(100.0F, DescentMath.reversedLaunchAnim(100, 48.0D));
        assertEquals(50.0F, DescentMath.reversedLaunchAnim(100, 24.0D));
        assertEquals(0.0F, DescentMath.reversedLaunchAnim(100, 0.0D));
    }

    @Test
    void reResolveExcludesTheBrokenNode() {
        LandingContext c = new LandingContext("nerospace:luna", 3, java.util.List.of(), java.util.Optional.empty(),
                java.util.Optional.empty(), false, net.minecraft.core.BlockPos.ZERO, java.util.Optional.empty(),
                LandingResolverTest.CLEAR, java.util.Set.of());
        LandingContext again = c.excluding(7);
        assertTrue(again.excludedNodeIds().contains(7));
        assertEquals(-1, again.explicitPadIndex());
    }
}
