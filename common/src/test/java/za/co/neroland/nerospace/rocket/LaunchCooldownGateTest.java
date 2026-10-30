package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/** 005 G5: the launch cooldown is checked before any NeroEconomy fee, so a refused launch is never charged. */
class LaunchCooldownGateTest {

    @Test
    void cooldownRefusesBeforeTheFee() {
        UUID rider = UUID.randomUUID();
        AtomicInteger charges = new AtomicInteger();
        LaunchCooldowns.Gate first = LaunchCooldowns.gate(rider, 1_000L, DescentMath.LAUNCH_COOLDOWN, () -> {
            charges.incrementAndGet();
            return null;
        });
        assertEquals(LaunchCooldowns.Verdict.OK, first.verdict());
        assertEquals(1, charges.get());

        LaunchCooldowns.Gate second = LaunchCooldowns.gate(rider, 1_050L, DescentMath.LAUNCH_COOLDOWN, () -> {
            charges.incrementAndGet();
            return null;
        });
        assertEquals(LaunchCooldowns.Verdict.COOLDOWN, second.verdict());
        assertEquals(1, charges.get(), "no fee is charged inside the cooldown window");

        LaunchCooldowns.Gate third = LaunchCooldowns.gate(rider, 1_000L + DescentMath.LAUNCH_COOLDOWN,
                DescentMath.LAUNCH_COOLDOWN, () -> {
                    charges.incrementAndGet();
                    return null;
                });
        assertEquals(LaunchCooldowns.Verdict.OK, third.verdict());
        assertEquals(2, charges.get());
        LaunchCooldowns.forget(rider);
    }

    @Test
    void refusedFeeDoesNotStartTheCooldown() {
        UUID rider = UUID.randomUUID();
        LaunchCooldowns.Gate refused = LaunchCooldowns.gate(rider, 5_000L, DescentMath.LAUNCH_COOLDOWN,
                () -> "message.test.refused");
        assertEquals(LaunchCooldowns.Verdict.REFUSED, refused.verdict());
        assertEquals("message.test.refused", refused.refusal());
        assertFalse(LaunchCooldowns.coolingDown(rider, 5_001L));
        assertEquals(LaunchCooldowns.Verdict.OK,
                LaunchCooldowns.gate(rider, 5_001L, DescentMath.LAUNCH_COOLDOWN, () -> null).verdict());
        assertTrue(LaunchCooldowns.coolingDown(rider, 5_002L));
        LaunchCooldowns.forget(rider);
    }
}
