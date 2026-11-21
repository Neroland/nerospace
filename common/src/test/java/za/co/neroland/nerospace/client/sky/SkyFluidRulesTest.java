package za.co.neroland.nerospace.client.sky;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The planet sky steps aside whenever the camera is inside a fog-hiding medium. */
class SkyFluidRulesTest {

    @Test
    @DisplayName("water, lava and powder snow hide the planet sky; open air does not")
    void table() {
        assertTrue(SkyFluidRules.hidesPlanetSky("WATER"));
        assertTrue(SkyFluidRules.hidesPlanetSky("LAVA"));
        assertTrue(SkyFluidRules.hidesPlanetSky("POWDER_SNOW"));
        assertFalse(SkyFluidRules.hidesPlanetSky("NONE"));
        assertFalse(SkyFluidRules.hidesPlanetSky("ATMOSPHERIC"));
        assertFalse(SkyFluidRules.hidesPlanetSky(null));
    }

    @Test
    @DisplayName("unknown fog types keep the planet sky")
    void everyFogTypeKnown() {
        // Unknown or future constants default to "draw".
        assertFalse(SkyFluidRules.hidesPlanetSky("SOME_FUTURE_FOG"));
    }
}
