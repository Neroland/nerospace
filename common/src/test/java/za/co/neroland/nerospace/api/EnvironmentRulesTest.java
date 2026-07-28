package za.co.neroland.nerospace.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class EnvironmentRulesTest {

    @Test
    void unloadedRegionsNeverReceiveAtmosphereOrOxygen() {
        EnvironmentSnapshot snapshot = EnvironmentRules.resolve(false,
                Optional.of(NerospacePlanets.CINDARA), Hazard.HEAT, 0.7D, 15, 3, true);
        assertFalse(snapshot.loaded());
        assertEquals(0, snapshot.oxygen());
        assertEquals(Atmosphere.VACUUM, snapshot.atmosphere());
    }

    @Test
    void oxygenAndTerraformingResolveToVisibleAtmosphereStates() {
        EnvironmentSnapshot pressure = EnvironmentRules.resolve(true, Optional.empty(), Hazard.NONE,
                1.0D, 3, 0, false);
        assertEquals(Atmosphere.PRESSURIZED, pressure.atmosphere());
        assertFalse(pressure.breathable());

        EnvironmentSnapshot terraformed = EnvironmentRules.resolve(true,
                Optional.of(NerospacePlanets.GREENXERTZ), Hazard.NONE, 0.5D, 0, 1, false);
        assertEquals(Atmosphere.BREATHABLE, terraformed.atmosphere());
        assertTrue(terraformed.breathable());
    }
}
