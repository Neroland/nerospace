package za.co.neroland.nerospace.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Atmosphere rules. These are the pure half of {@link NerospaceEnvironment}, deliberately split out so the
 * decision table can be tested without a server.
 */
class EnvironmentRulesTest {

    private static EnvironmentSnapshot resolve(boolean loaded, int oxygen, int stage, boolean underway,
            boolean fieldBreathable) {
        return EnvironmentRules.resolve(loaded, Optional.empty(), Hazard.NONE, 1.0D, oxygen, stage,
                underway, fieldBreathable);
    }

    /** V4-04: on a NO_ATMOSPHERE world (terraformed Europa) a stage alone is not air; everything else unchanged. */
    @Test
    @DisplayName("no-atmosphere world: terraformed ground is not breathable, oxygen and fields still are")
    void noAtmosphereWorld() {
        for (int stage = 1; stage <= EnvironmentRules.MAX_STAGE; stage++) {
            EnvironmentSnapshot europa = EnvironmentRules.resolve(true, Optional.empty(), Hazard.COLD, 1.0D, 0, stage,
                    false, false, true);
            assertFalse(europa.breathable(), "stage " + stage);
            assertEquals(Atmosphere.VACUUM, europa.atmosphere());
            EnvironmentSnapshot other = EnvironmentRules.resolve(true, Optional.empty(), Hazard.NONE, 1.0D, 0, stage,
                    false, false, false);
            assertTrue(other.breathable(), "other worlds: stage " + stage + " is air");
            assertEquals(resolve(true, 0, stage, false, false), other, "8-argument form unchanged");
        }
        assertTrue(EnvironmentRules.resolve(true, Optional.empty(), Hazard.COLD, 1.0D,
                NerospaceOxygen.BREATHABLE_PRESSURE, 2, false, false, true).breathable());
        assertTrue(EnvironmentRules.resolve(true, Optional.empty(), Hazard.COLD, 1.0D, 0, 2, false, true, true)
                .breathable());
    }

    @Test
    @DisplayName("an unloaded position fails closed to vacuum and ignores every other reading")
    void unloadedFailsClosed() {
        EnvironmentSnapshot snapshot = resolve(false, 15, 3, true, true);

        assertFalse(snapshot.loaded());
        assertFalse(snapshot.breathable());
        assertEquals(Atmosphere.VACUUM, snapshot.atmosphere());
        assertEquals(0, snapshot.oxygen());
        assertEquals(0, snapshot.terraformStage());
    }

    @Test
    @DisplayName("no oxygen and no terraforming is vacuum")
    void vacuum() {
        assertEquals(Atmosphere.VACUUM, resolve(true, 0, 0, false, false).atmosphere());
    }

    @Test
    @DisplayName("oxygen below the breathable threshold is pressurised but not breathable")
    void pressurised() {
        EnvironmentSnapshot snapshot = resolve(true, NerospaceOxygen.BREATHABLE_PRESSURE - 1, 0, false, false);

        assertEquals(Atmosphere.PRESSURIZED, snapshot.atmosphere());
        assertFalse(snapshot.breathable());
    }

    @Test
    @DisplayName("oxygen at the threshold is breathable")
    void breathableByOxygen() {
        EnvironmentSnapshot snapshot = resolve(true, NerospaceOxygen.BREATHABLE_PRESSURE, 0, false, false);

        assertEquals(Atmosphere.BREATHABLE, snapshot.atmosphere());
        assertTrue(snapshot.breathable());
    }

    @Test
    @DisplayName("the oxygen field's own verdict wins even with zero measured oxygen (sealed room)")
    void breathableByField() {
        assertEquals(Atmosphere.BREATHABLE, resolve(true, 0, 0, false, true).atmosphere());
    }

    @Test
    @DisplayName("terraform stage 1 is breathable, matching TerraformConversion flagging the chunk")
    void breathableByStage() {
        EnvironmentSnapshot snapshot = resolve(true, 0, 1, false, false);

        assertEquals(Atmosphere.BREATHABLE, snapshot.atmosphere());
        assertTrue(snapshot.breathable());
    }

    @Test
    @DisplayName("an overlay that has not yet reached stage 1 reports TERRAFORMING, not vacuum")
    void terraformingIsReachable() {
        EnvironmentSnapshot snapshot = resolve(true, 0, 0, true, false);

        assertEquals(Atmosphere.TERRAFORMING, snapshot.atmosphere());
        assertFalse(snapshot.breathable());
    }

    @Test
    @DisplayName("out-of-range readings are clamped rather than propagated")
    void clampsOutOfRange() {
        EnvironmentSnapshot snapshot = resolve(true, 9999, 99, false, false);

        assertEquals(EnvironmentRules.MAX_OXYGEN, snapshot.oxygen());
        assertEquals(EnvironmentRules.MAX_STAGE, snapshot.terraformStage());
    }

    @Test
    @DisplayName("a negative reading clamps to zero instead of underflowing")
    void clampsNegative() {
        EnvironmentSnapshot snapshot = resolve(true, -5, -2, false, false);

        assertEquals(0, snapshot.oxygen());
        assertEquals(0, snapshot.terraformStage());
        assertEquals(Atmosphere.VACUUM, snapshot.atmosphere());
    }
}
