package za.co.neroland.nerospace.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The 1.x planet API, probed exactly the way sibling mods read it at runtime (reflection by name, no compile
 * dependency): {@code byDimension(ResourceKey)} returning Optional, {@code traits(PlanetId)}, and on
 * {@code PlanetTraits} the accessors {@code planet/defaultGravity/airless/hazard} plus the 4-arg constructor.
 * Adding components is allowed; removing or renaming any of these is a break.
 */
class PlanetTraitsCompatTest {

    private static final String PLANETS = "za.co.neroland.nerospace.api.NerospacePlanets";
    private static final String TRAITS = "za.co.neroland.nerospace.api.PlanetTraits";

    @Test
    @DisplayName("NerospacePlanets.byDimension(ResourceKey) is public static and returns Optional")
    void byDimension() throws Exception {
        Class<?> planets = Class.forName(PLANETS);
        Method m = planets.getMethod("byDimension", ResourceKey.class);
        assertTrue(Modifier.isStatic(m.getModifiers()));
        assertEquals(Optional.class, m.getReturnType());
    }

    @Test
    @DisplayName("NerospacePlanets.traits(PlanetId) exists with that exact parameter type")
    void traitsSignature() throws Exception {
        Class<?> planets = Class.forName(PLANETS);
        Method m = planets.getMethod("traits", PlanetId.class);
        assertTrue(Modifier.isStatic(m.getModifiers()));
        assertEquals(Class.forName(TRAITS), m.getReturnType());
    }

    @Test
    @DisplayName("every 1.x PlanetTraits accessor keeps its name and type")
    void accessors() throws Exception {
        Class<?> traits = Class.forName(TRAITS);
        assertEquals(boolean.class, traits.getMethod("airless").getReturnType());
        assertEquals(Hazard.class, traits.getMethod("hazard").getReturnType());
        assertEquals(double.class, traits.getMethod("defaultGravity").getReturnType());
        assertEquals(PlanetId.class, traits.getMethod("planet").getReturnType());
    }

    @Test
    @DisplayName("the 1.x 4-arg constructor is kept and fills neutral defaults")
    void fourArgConstructor() throws Exception {
        Class<?> traits = Class.forName(TRAITS);
        assertNotNull(traits.getConstructor(PlanetId.class, double.class, boolean.class, Hazard.class));
        PlanetTraits t = new PlanetTraits(NerospacePlanets.CINDARA, 0.8, true, Hazard.HEAT);
        assertEquals(0.8, t.defaultGravity());
        assertTrue(t.airless());
        assertEquals(Hazard.HEAT, t.hazard());
        assertTrue(t.fixedClock());
        assertTrue(t.villagerCulture().isEmpty());
    }

    @Test
    @DisplayName("StationInfo keeps its 5-arg constructor")
    void stationInfo() throws Exception {
        assertNotNull(StationInfo.class.getConstructor(int.class, String.class, PlanetId.class, BlockPos.class, int.class));
    }

    @Test
    @DisplayName("Hazard keeps NONE, HEAT, COLD first and appends RADIATION")
    void hazardOrder() {
        assertArrayEquals(new Hazard[] {Hazard.NONE, Hazard.HEAT, Hazard.COLD, Hazard.RADIATION}, Hazard.values());
    }

    @Test
    @DisplayName("all() keeps the 1.x order as a prefix and appends Luna, Mars, Nyxara")
    void allOrder() {
        assertEquals(List.of(NerospacePlanets.GREENXERTZ, NerospacePlanets.CINDARA, NerospacePlanets.GLACIRA,
                NerospacePlanets.STATION, NerospacePlanets.LUNA, NerospacePlanets.MARS, NerospacePlanets.NYXARA),
                NerospacePlanets.all());
    }

    @Test
    @DisplayName("the 1.x planets report exactly their 1.x traits through the facade")
    void legacyValues() {
        assertTraits(NerospacePlanets.GREENXERTZ, 0.6, true, Hazard.NONE);
        assertTraits(NerospacePlanets.CINDARA, 0.8, true, Hazard.HEAT);
        assertTraits(NerospacePlanets.GLACIRA, 0.4, true, Hazard.COLD);
        assertTraits(NerospacePlanets.STATION, 0.1, true, Hazard.NONE);
        assertEquals(Optional.of(NerospacePlanets.CINDARA),
                NerospacePlanets.byDimension(NerospacePlanets.CINDARA.dimension()));
        assertEquals(Optional.empty(), NerospacePlanets.byId("nerospace:capture"), "the gallery dimension is not a planet");
    }

    private static void assertTraits(PlanetId id, double gravity, boolean airless, Hazard hazard) {
        PlanetTraits t = NerospacePlanets.traits(id);
        assertEquals(id, t.planet());
        assertEquals(gravity, t.defaultGravity(), 1.0E-9, id.asString());
        assertEquals(airless, t.airless(), id.asString());
        assertEquals(hazard, t.hazard(), id.asString());
    }
}
