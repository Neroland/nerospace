package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.registry.ModDimensions;

/**
 * Destination indices are append-only, every tier keeps its 1.x destinations as a prefix,
 * and the default (signature) destination is pinned to the 1.x value even though new ones were appended.
 */
class RocketTierDefaultsTest {

    private static final List<List<ResourceKey<Level>>> LEGACY = List.of(
            List.of(ModDimensions.STATION_LEVEL),
            List.of(ModDimensions.STATION_LEVEL, ModDimensions.GREENXERTZ_LEVEL),
            List.of(ModDimensions.STATION_LEVEL, ModDimensions.GREENXERTZ_LEVEL, ModDimensions.CINDARA_LEVEL),
            List.of(ModDimensions.STATION_LEVEL, ModDimensions.GREENXERTZ_LEVEL, ModDimensions.CINDARA_LEVEL,
                    ModDimensions.GLACIRA_LEVEL));

    @Test
    @DisplayName("Destinations: 1.x indices frozen, Luna/Mars/Nyxara appended as 5/6/7")
    void destinationsAppend() {
        assertEquals(0, Destinations.HOME);
        assertEquals(1, Destinations.STATION);
        assertEquals(2, Destinations.GREENXERTZ);
        assertEquals(3, Destinations.CINDARA);
        assertEquals(4, Destinations.GLACIRA);
        assertEquals(5, Destinations.LUNA);
        assertEquals(6, Destinations.MARS);
        assertEquals(7, Destinations.NYXARA);
        assertEquals(8, Destinations.all().size());
        assertEquals(ModDimensions.OVERWORLD, Destinations.byIndex(Destinations.HOME));
        assertEquals(ModDimensions.LUNA_LEVEL, Destinations.byIndex(Destinations.LUNA));
        assertEquals(ModDimensions.NYXARA_LEVEL, Destinations.byIndex(Destinations.NYXARA));
        for (int i = 0; i < Destinations.all().size(); i++) {
            assertEquals(i, Destinations.indexOf(Destinations.byIndex(i)));
        }
    }

    @Test
    @DisplayName("English names of the 1.x destinations are frozen")
    void frozenNames() {
        assertEquals("Home", Destinations.name(ModDimensions.OVERWORLD));
        assertEquals("Orbital Station", Destinations.name(ModDimensions.STATION_LEVEL));
        assertEquals("Greenxertz", Destinations.name(ModDimensions.GREENXERTZ_LEVEL));
        assertEquals("Cindara", Destinations.name(ModDimensions.CINDARA_LEVEL));
        assertEquals("Glacira", Destinations.name(ModDimensions.GLACIRA_LEVEL));
    }

    @Test
    @DisplayName("every tier keeps its 1.x list as a prefix and its 1.x default")
    void tiers() {
        RocketTier[] tiers = RocketTier.values();
        assertEquals(4, tiers.length);
        assertEquals("TIER_1", tiers[0].name());
        assertEquals("TIER_4", tiers[3].name());
        for (int t = 0; t < tiers.length; t++) {
            List<ResourceKey<Level>> legacy = LEGACY.get(t);
            assertEquals(legacy, tiers[t].destinations().subList(0, legacy.size()), tiers[t] + " prefix");
            assertEquals(legacy.get(legacy.size() - 1), tiers[t].destination(tiers[t].defaultDestinationIndex()),
                    tiers[t] + " default");
            for (int i = 0; i < legacy.size(); i++) {
                assertEquals(Destinations.indexOf(legacy.get(i)), Destinations.legacyIndex(tiers[t], i),
                        tiers[t] + " legacy index " + i);
            }
        }
    }

    @Test
    @DisplayName("roster: T1 +Luna, T2 +Mars, T4 +Nyxara")
    void roster() {
        assertTrue(RocketTier.TIER_1.destinations().contains(ModDimensions.LUNA_LEVEL));
        assertFalse(RocketTier.TIER_1.destinations().contains(ModDimensions.MARS_LEVEL));
        assertTrue(RocketTier.TIER_2.destinations().contains(ModDimensions.MARS_LEVEL));
        assertTrue(RocketTier.TIER_4.destinations().contains(ModDimensions.NYXARA_LEVEL));
        assertFalse(RocketTier.TIER_3.destinations().contains(ModDimensions.NYXARA_LEVEL));
    }

    @Test
    @DisplayName("reach is cumulative: each tier reaches every destination of the tiers below it")
    void cumulative() {
        RocketTier[] tiers = RocketTier.values();
        for (int t = 1; t < tiers.length; t++) {
            assertTrue(tiers[t].destinations().containsAll(tiers[t - 1].destinations()), tiers[t] + " ⊇ " + tiers[t - 1]);
            assertEquals(tiers[t].destinations().size(), new java.util.HashSet<>(tiers[t].destinations()).size(),
                    tiers[t] + " has no duplicates");
        }
        assertEquals(List.of(ModDimensions.STATION_LEVEL, ModDimensions.LUNA_LEVEL),
                RocketTier.TIER_1.destinations());
        assertTrue(RocketTier.TIER_3.destinations().contains(ModDimensions.LUNA_LEVEL));
        assertTrue(RocketTier.TIER_3.destinations().contains(ModDimensions.MARS_LEVEL));
        assertFalse(RocketTier.TIER_3.destinations().contains(ModDimensions.GLACIRA_LEVEL));
        assertEquals(7, RocketTier.TIER_4.destinations().size()); // every off-world destination
    }

    @Test
    @DisplayName("the server accepts the cumulative set and nothing beyond it")
    void serverSelectionAllowsCumulativeOnly() {
        int t3 = Destinations.availableMask(RocketTier.TIER_3, ModDimensions.OVERWORLD);
        for (int i : new int[] {Destinations.STATION, Destinations.GREENXERTZ, Destinations.CINDARA,
                Destinations.LUNA, Destinations.MARS}) {
            assertTrue(DestinationSelection.evaluate(i, t3, 0, -1, false).accepted(), "T3 index " + i);
        }
        for (int i : new int[] {Destinations.HOME, Destinations.GLACIRA, Destinations.NYXARA, 8, 30, -1, 31}) {
            assertEquals(DestinationSelection.Outcome.OUT_OF_RANGE, DestinationSelection.evaluate(i, t3, 0, -1, false),
                    "T3 index " + i);
        }
        int t4 = Destinations.availableMask(RocketTier.TIER_4, ModDimensions.OVERWORLD);
        int deepSpaceClosed = 1 << Destinations.NYXARA;
        assertEquals(DestinationSelection.Outcome.GATE_LOCKED,
                DestinationSelection.evaluate(Destinations.NYXARA, t4, deepSpaceClosed, -1, false));
        assertTrue(DestinationSelection.evaluate(Destinations.LUNA, t4, deepSpaceClosed, -1, false).accepted());
    }

    @Test
    @DisplayName("a saved destination index stays valid and the default selection is unchanged")
    void savedDestinationsStayValid() {
        for (RocketTier tier : RocketTier.values()) {
            for (int i = 0; i < Destinations.all().size(); i++) {
                if (Destinations.isAvailable(tier, ModDimensions.OVERWORLD, i)) {
                    assertEquals(i, Destinations.sanitizeIndex(tier, ModDimensions.OVERWORLD, i), tier + " " + i);
                }
            }
        }
        assertEquals(Destinations.CINDARA, Destinations.defaultIndex(RocketTier.TIER_3, ModDimensions.OVERWORLD));
    }

    @Test
    @DisplayName("a T1 rocket in the Overworld still defaults to the Station, not Luna")
    void defaultIndexFromHome() {
        assertEquals(Destinations.STATION, Destinations.defaultIndex(RocketTier.TIER_1, ModDimensions.OVERWORLD));
        assertEquals(Destinations.GREENXERTZ, Destinations.defaultIndex(RocketTier.TIER_2, ModDimensions.OVERWORLD));
        assertEquals(Destinations.GLACIRA, Destinations.defaultIndex(RocketTier.TIER_4, ModDimensions.OVERWORLD));
        assertEquals(Destinations.HOME, Destinations.defaultIndex(RocketTier.TIER_4, ModDimensions.CINDARA_LEVEL));
    }

    @Test
    @DisplayName("a saved Nyxara destination survives load sanitising (the gate is checked at launch)")
    void nyxaraKeptOnLoad() {
        assertEquals(Destinations.NYXARA,
                Destinations.sanitizeIndex(RocketTier.TIER_4, ModDimensions.OVERWORLD, Destinations.NYXARA));
    }
}
