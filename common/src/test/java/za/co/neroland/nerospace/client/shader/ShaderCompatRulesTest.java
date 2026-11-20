package za.co.neroland.nerospace.client.shader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.client.shader.ShaderCompatRules.Mode;

/** Shader-pack dimension compatibility: the resolution table, pack precedence, the off switch, no shader mod. */
class ShaderCompatRulesTest {

    @Test
    void resolutionTable() {
        assertEquals(Optional.of(ShaderDimension.END),
                ShaderCompatRules.override(Mode.AUTO, true, ShaderDimension.END, false));
        assertEquals(Optional.of(ShaderDimension.NETHER),
                ShaderCompatRules.override(Mode.AUTO, true, ShaderDimension.NETHER, false));
        assertEquals(Optional.empty(), ShaderCompatRules.override(Mode.AUTO, true, ShaderDimension.OVERWORLD, false),
                "overworld is the pack's default for an unknown dimension: nothing to override");
        assertEquals(Optional.empty(), ShaderCompatRules.override(Mode.AUTO, false, ShaderDimension.END, false),
                "a level without a planet profile is never touched");
        assertEquals(Optional.empty(), ShaderCompatRules.override(Mode.AUTO, true, null, false));
    }

    @Test
    void thePackWinsWhenItNamesTheDimension() {
        for (ShaderDimension choice : ShaderDimension.values()) {
            assertEquals(Optional.empty(), ShaderCompatRules.override(Mode.AUTO, true, choice, true));
        }
    }

    @Test
    void offNeverOverrides() {
        for (ShaderDimension choice : ShaderDimension.values()) {
            assertEquals(Optional.empty(), ShaderCompatRules.override(Mode.OFF, true, choice, false));
        }
        assertEquals(Mode.OFF, Mode.byName("off"));
        assertEquals(Mode.OFF, Mode.byName(" OFF "));
        assertEquals(Mode.AUTO, Mode.byName("auto"));
        assertEquals(Mode.AUTO, Mode.byName(null));
        assertEquals(Mode.AUTO, Mode.byName("nonsense"));
    }

    @Test
    void profileNamesAreLenient() {
        assertEquals(ShaderDimension.END, ShaderDimension.byName("end"));
        assertEquals(ShaderDimension.NETHER, ShaderDimension.byName("Nether"));
        assertEquals(ShaderDimension.OVERWORLD, ShaderDimension.byName(null));
        assertEquals(ShaderDimension.OVERWORLD, ShaderDimension.byName(""));
        assertEquals(ShaderDimension.OVERWORLD, ShaderDimension.byName("the_end"));
        assertEquals("end", ShaderDimension.END.serializedName());
    }

    @Test
    void planetSkyDrawsUnlessThePlayerHandsItToThePack() {
        assertTrue(ShaderCompatRules.drawPlanetSky(false, true));
        assertTrue(ShaderCompatRules.drawPlanetSky(false, false), "the setting only matters under a pack");
        assertTrue(ShaderCompatRules.drawPlanetSky(true, true));
        assertFalse(ShaderCompatRules.drawPlanetSky(true, false));
    }

    @Test
    void bridgeIsInactiveWithoutAShaderMod() {
        // No shader mod is on the test classpath: every query answers "nothing", and nothing throws.
        assertFalse(IrisBridge.present());
        assertFalse(IrisBridge.packActive());
        assertFalse(IrisBridge.packNames("nerospace", "luna"));
        assertNull(IrisBridge.dimensionConstant(ShaderDimension.END));
        assertNull(IrisBridge.dimensionConstant(null));
    }
}
