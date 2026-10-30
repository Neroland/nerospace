package za.co.neroland.nerospace.world.planet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.api.TemperatureBand;
import za.co.neroland.nerospace.world.planet.HazardOrderRules.SuitClass;

/**
 * Per-player hazard resolution: the first hazard in [RADIATION, HEAT, COLD] the player is not shielded from, the
 * graded radiation damage, and the Rad Lining upgrade that shields RADIATION.
 */
class HazardModelTest {

    private static Set<Hazard> set(Hazard... hs) {
        Set<Hazard> s = EnumSet.noneOf(Hazard.class);
        s.addAll(List.of(hs));
        return s;
    }

    @Test
    @DisplayName("fixed order radiation, heat, cold; NONE when every present hazard is shielded")
    void order() {
        assertEquals(List.of(Hazard.RADIATION, Hazard.HEAT, Hazard.COLD), HazardOrderRules.ORDER);
        assertEquals(Hazard.NONE, HazardOrderRules.firstUnshielded(set(), set()));
        assertEquals(Hazard.NONE, HazardOrderRules.firstUnshielded(set(Hazard.NONE), set()));
        assertEquals(Hazard.HEAT, HazardOrderRules.firstUnshielded(set(Hazard.COLD, Hazard.HEAT), set()));
        assertEquals(Hazard.COLD, HazardOrderRules.firstUnshielded(set(Hazard.COLD, Hazard.HEAT), set(Hazard.HEAT)));
    }

    @Test
    @DisplayName("Nyxara nightside needs both linings; a flaring Dawnband stands in for the cryo lining")
    void nightside() {
        Set<Hazard> night = set(Hazard.RADIATION, Hazard.COLD);
        assertEquals(Hazard.RADIATION, HazardOrderRules.firstUnshielded(night, set()));
        assertEquals(Hazard.RADIATION, HazardOrderRules.firstUnshielded(night, set(Hazard.COLD)), "cryo alone");
        assertEquals(Hazard.COLD, HazardOrderRules.firstUnshielded(night, set(Hazard.RADIATION)), "rad lining alone");
        assertEquals(Hazard.NONE, HazardOrderRules.firstUnshielded(night, set(Hazard.RADIATION, Hazard.COLD)),
                "rad-lined cryo suit, or rad lining beside a flaring Dawnband");
    }

    @Test
    @DisplayName("Nyxara dayside: radiation and heat; the band stays radiation-only")
    void daysideAndBand() {
        Set<Hazard> day = set(Hazard.RADIATION, Hazard.HEAT);
        assertEquals(Hazard.HEAT, HazardOrderRules.firstUnshielded(day, set(Hazard.RADIATION)));
        assertEquals(Hazard.NONE, HazardOrderRules.firstUnshielded(day, set(Hazard.RADIATION, Hazard.HEAT)));
        assertEquals(Hazard.RADIATION, Planets.NYXARA.traits().hazard(), "trait hazard unchanged (API)");
        assertEquals(Hazard.NONE, HazardOrderRules.firstUnshielded(set(Hazard.RADIATION), set(Hazard.RADIATION)));
    }

    // --- per planet, biome, suit and Rad Lining ---------------------------------------------------------------------

    /** Suit columns of the matrix, in order: each suit unlined, then lined (a T1 or bare player cannot be lined). */
    private static final SuitClass[] SUITS = { SuitClass.NONE, SuitClass.T1, SuitClass.T2, SuitClass.T2,
        SuitClass.THERMAL, SuitClass.THERMAL, SuitClass.CRYO, SuitClass.CRYO };
    private static final boolean[] LINED = { false, false, false, true, false, true, false, true };

    private static char code(Hazard h) {
        return switch (h) {
            case NONE -> '.';
            case RADIATION -> 'R';
            case HEAT -> 'H';
            case COLD -> 'C';
        };
    }

    /**
     * One row: what bites on {@code planet} in {@code biome} (weather radiation and a flaring Dawnband as given), as
     * one character per suit column: none, T1, T2, T2+Rad, Thermal, Thermal+Rad, Cryo, Cryo+Rad.
     */
    private static void row(PlanetDefinition planet, String biome, boolean weatherRad, boolean dawnband,
            String expected) throws IOException {
        boolean rad = tagged("radiation", biome);
        boolean heat = tagged("heat", biome);
        boolean cold = tagged("cold", biome);
        Set<Hazard> present = HazardOrderRules.present(planet.traits().hazard(), rad, heat, cold, weatherRad);
        boolean frigid = HazardOrderRules.frigidCold(planet.traits().temperatureBand(), cold);
        StringBuilder got = new StringBuilder();
        for (int i = 0; i < SUITS.length; i++) {
            got.append(code(HazardOrderRules.resolve(present, SUITS[i], LINED[i], frigid, dawnband)));
        }
        assertEquals(expected, got.toString(), planet.id() + " / " + biome + (weatherRad ? " / storm" : "")
                + (dawnband ? " / dawnband" : "") + "  [none,T1,T2,T2+R,Th,Th+R,Cr,Cr+R]");
    }

    private static boolean tagged(String hazard, String biome) throws IOException {
        if (biome.isEmpty()) {
            return false;
        }
        for (JsonElement e : load("/data/nerospace/tags/worldgen/biome/hazard/" + hazard + ".json").getAsJsonObject()
                .getAsJsonArray("values")) {
            if (e.getAsString().equals(biome)) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("matrix: every planet and biome x suit tier/variant x Rad Lining")
    void matrix() throws IOException {
        //                                                                     none T1 T2 T2R Th ThR Cr CrR
        row(Planets.STATION, "", false, false, "........");
        for (String b : List.of("nerospace:luna_maria", "nerospace:luna_highlands", "nerospace:luna_crater_field",
                "nerospace:terraformed_luna_living")) {
            row(Planets.LUNA, b, false, false, "........");
        }
        for (String b : List.of("nerospace:greenxertz", "nerospace:greenxertz_spore_marsh",
                "nerospace:greenxertz_fungal_depths", "nerospace:terraformed_greenxertz_living")) {
            row(Planets.GREENXERTZ, b, false, false, "........");
        }
        // Mars: a cold world. Any Tier 2 class suit keeps the cold out; the Polar Cap is frigid (Cryo only).
        for (String b : List.of("nerospace:mars_dunes", "nerospace:mars_canyons", "nerospace:terraformed_mars_living")) {
            row(Planets.MARS, b, false, false, "CC......");
            row(Planets.MARS, b, true, false, "RRR.R.R.");
        }
        row(Planets.MARS, "nerospace:mars_polar", false, false, "CCCCCC..");
        row(Planets.MARS, "nerospace:mars_polar", true, false, "RRRCRCR.");
        // Cindara: heat, Thermal only (as in 1.x).
        for (String b : List.of("nerospace:cindara", "nerospace:cindara_ember_plains", "nerospace:cindara_sulfur_vents",
                "nerospace:terraformed_savanna")) {
            row(Planets.CINDARA, b, false, false, "HHHH..HH");
        }
        // Glacira: frigid, Cryo only (as in 1.x).
        for (String b : List.of("nerospace:glacira", "nerospace:glacira_methane_shore", "nerospace:glacira_spires",
                "nerospace:terraformed_tundra")) {
            row(Planets.GLACIRA, b, false, false, "CCCCCC..");
        }
        // Nyxara: radiation everywhere; heat on the Dayside, frigid cold on the Nightside.
        row(Planets.NYXARA, "nerospace:nyxara_twilight_band", false, false, "RRR.R.R.");
        row(Planets.NYXARA, "nerospace:terraformed_nyxara_living", false, false, "RRR.R.R.");
        row(Planets.NYXARA, "nerospace:nyxara_dayside", false, false, "RRRHR.RH");
        row(Planets.NYXARA, "nerospace:nyxara_nightside", false, false, "RRRCRCR.");
        row(Planets.NYXARA, "nerospace:nyxara_nightside", false, true, "RRR.R.R.");
    }

    @Test
    @DisplayName("cold bands: Mars is cold, Glacira frigid, a cold-tagged biome is frigid anywhere")
    void coldBands() {
        assertEquals(TemperatureBand.COLD, Planets.MARS.traits().temperatureBand());
        assertEquals(TemperatureBand.FRIGID, Planets.GLACIRA.traits().temperatureBand());
        assertEquals(Hazard.COLD, Planets.GLACIRA.traits().hazard(), "Glacira trait hazard unchanged (API)");
        assertFalse(HazardOrderRules.frigidCold(TemperatureBand.COLD, false));
        assertTrue(HazardOrderRules.frigidCold(TemperatureBand.COLD, true));
        assertTrue(HazardOrderRules.frigidCold(TemperatureBand.FRIGID, false));
        assertTrue(HazardOrderRules.frigidCold(TemperatureBand.TEMPERATE, true));
        assertEquals(Set.of(), HazardOrderRules.shields(SuitClass.T1, false, false), "a Tier 1 suit shields nothing");
    }

    @Test
    @DisplayName("Mars dust storm requires the Rad Lining on top of the suit; a quiet Mars does not")
    void marsStorm() {
        assertEquals(Hazard.COLD, Planets.MARS.traits().hazard());
        Set<Hazard> storm = set(Hazard.RADIATION, Planets.MARS.traits().hazard());
        assertEquals(Hazard.RADIATION, HazardOrderRules.firstUnshielded(storm, set(Hazard.COLD)));
        assertEquals(Hazard.NONE, HazardOrderRules.firstUnshielded(storm, set(Hazard.COLD, Hazard.RADIATION)));
        int stormLevel = RadiationRules.effective(Planets.MARS.traits().radiation(), WeatherEvent.DUST_STORM,
                "nerospace:mars_dunes", true);
        assertTrue(RadiationRules.damageFor(stormLevel) > 0.0F, "an unlined suit is hurt in a storm");
        int calm = RadiationRules.effective(Planets.MARS.traits().radiation(), WeatherEvent.NONE,
                "nerospace:mars_dunes", true);
        assertEquals(0.0F, RadiationRules.damageFor(calm));
    }

    @Test
    @DisplayName("Luna and the station never require the Rad Lining")
    void lunaAndStation() {
        for (PlanetDefinition p : List.of(Planets.LUNA, Planets.STATION)) {
            assertEquals(Hazard.NONE, p.traits().hazard(), p.id().toString());
            assertFalse(p.weather().hasWeather(), "no storm can raise radiation on " + p.id());
            assertEquals(0.0F, RadiationRules.damageFor(RadiationRules.effective(p.traits().radiation(),
                    WeatherEvent.NONE, "", true)), p.id().toString());
        }
    }

    @Test
    @DisplayName("radiation damage graded by level 0..3 (flare +1)")
    void graded() {
        assertEquals(0.0F, RadiationRules.damageFor(0));
        assertEquals(0.0F, RadiationRules.damageFor(1));
        assertEquals(1.0F, RadiationRules.damageFor(2));
        assertEquals(2.0F, RadiationRules.damageFor(3));
        assertEquals(2.0F, RadiationRules.damageFor(9), "clamped");
        int day = RadiationRules.base(3, RadiationRules.NYXARA_DAYSIDE);
        assertTrue(RadiationRules.damageFor(RadiationRules.effective(day, WeatherEvent.FLARE,
                RadiationRules.NYXARA_DAYSIDE, true)) > RadiationRules.damageFor(day), "a flare hurts more");
        assertEquals(0.0F, RadiationRules.damageFor(RadiationRules.base(3, RadiationRules.NYXARA_BAND)));
        assertEquals(2.0F, RadiationRules.damageFor(RadiationRules.base(3, RadiationRules.NYXARA_NIGHTSIDE)));
    }

    // --- data -------------------------------------------------------------------------------------------------------

    private static JsonElement load(String path) throws IOException {
        try (InputStream in = HazardModelTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    @Test
    @DisplayName("both Nyxara sides carry the radiation tag; no Luna or station biome does")
    void radiationTag() throws IOException {
        String tag = load("/data/nerospace/tags/worldgen/biome/hazard/radiation.json").toString();
        assertTrue(tag.contains("nerospace:nyxara_dayside"));
        assertTrue(tag.contains("nerospace:nyxara_nightside"));
        assertFalse(tag.contains("luna"));
        assertFalse(tag.contains("station"));
        assertTrue(load("/data/nerospace/tags/worldgen/biome/hazard/cold.json").toString()
                .contains("nerospace:nyxara_nightside"));
    }

    @Test
    @DisplayName("Rad Lining: crafted from Luna and Mars materials, applied by smithing to every T2-tier piece, ids kept")
    void radLiningRecipes() throws IOException {
        String lining = load("/data/nerospace/recipe/rad_lining.json").toString();
        assertTrue(lining.contains("nerospace:titanium_ingot"), "Luna titanium");
        assertTrue(lining.contains("nerospace:regolith"), "Luna regolith");
        assertTrue(lining.contains("nerospace:raw_hematite"), "Mars hematite");
        assertTrue(lining.contains("\"id\":\"nerospace:rad_lining\""));
        for (String variant : List.of("t2", "heat", "cold")) {
            for (String slot : List.of("helmet", "chestplate", "leggings", "boots")) {
                String id = "nerospace:oxygen_suit_" + variant + "_" + slot;
                JsonObject r = load("/data/nerospace/recipe/oxygen_suit_" + variant + "_" + slot + "_rad_lining.json")
                        .getAsJsonObject();
                assertEquals("minecraft:smithing_transform", r.get("type").getAsString());
                assertEquals("nerospace:rad_lining", r.get("template").getAsString());
                assertEquals(id, r.get("base").getAsString());
                JsonObject result = r.getAsJsonObject("result");
                assertEquals(id, result.get("id").getAsString(), "the upgraded piece keeps its id");
                assertTrue(result.getAsJsonObject("components").has("nerospace:rad_lining"));
            }
        }
        // The T1 suit is not upgradable (the lining is a T2 upgrade).
        assertTrue(HazardModelTest.class.getResource("/data/nerospace/recipe/oxygen_suit_helmet_rad_lining.json") == null);
    }
}
