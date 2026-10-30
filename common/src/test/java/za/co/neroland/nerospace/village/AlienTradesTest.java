package za.co.neroland.nerospace.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** D8: one trade table per culture; the Xertz table is the 1.6.0 table row for row. */
class AlienTradesTest {

    /** The 1.6.0 {@code AlienTrades.forTier} body, transcribed: tier, costA, n, costB, b, result, rc, maxUses, xp. */
    private static final List<TradeTable.Row> XERTZ_1_6_0 = List.of(
            new TradeTable.Row(1, "nerospace:xertz_quartz", 12, null, 0, "minecraft:emerald", 1, 16, 1),
            new TradeTable.Row(1, "minecraft:emerald", 1, null, 0, "minecraft:iron_ingot", 3, 16, 1),
            new TradeTable.Row(1, "minecraft:emerald", 2, null, 0, "minecraft:bread", 6, 16, 1),
            new TradeTable.Row(2, "minecraft:emerald", 4, null, 0, "nerospace:nerosium_ingot", 1, 12, 5),
            new TradeTable.Row(2, "minecraft:emerald", 1, "nerospace:raw_nerosteel", 8, "nerospace:nerosteel_ingot", 4, 12, 5),
            new TradeTable.Row(2, "nerospace:alien_fragment", 4, null, 0, "minecraft:emerald", 1, 12, 2),
            new TradeTable.Row(3, "minecraft:emerald", 8, null, 0, "minecraft:diamond", 1, 6, 10),
            new TradeTable.Row(3, "minecraft:emerald", 5, null, 0, "nerospace:rocket_fuel_canister", 1, 8, 8),
            new TradeTable.Row(4, "minecraft:emerald", 12, "nerospace:alien_tech_scrap", 2, "nerospace:alien_core", 1, 4, 15),
            new TradeTable.Row(4, "minecraft:emerald", 16, null, 0, "nerospace:xertz_resonator", 1, 2, 15),
            new TradeTable.Row(5, "minecraft:emerald", 18, null, 0, "minecraft:diamond", 3, 4, 20),
            new TradeTable.Row(5, "nerospace:alien_core", 1, "minecraft:emerald", 24, "nerospace:grav_striders", 1, 2, 20));

    @Test
    void xertzTableIsUnchanged() {
        assertEquals(XERTZ_1_6_0, AlienTrades.XERTZ_TABLE.rows());
        // Cumulative tiers exactly as 1.6.0: 3, 6, 8, 10, 12 offers.
        int[] expected = {3, 6, 8, 10, 12};
        for (int tier = 1; tier <= 5; tier++) {
            assertEquals(expected[tier - 1], AlienTrades.XERTZ_TABLE.rowsUpTo(tier).size(), "tier " + tier);
        }
    }

    @Test
    void everyCultureHasItsOwnTable() {
        List<String> cultures = List.of(AlienTrades.XERTZ, AlienTrades.EMBER_KIN, AlienTrades.FROSTBOUND,
                AlienTrades.DUSKBORN);
        List<TradeTable> seen = new ArrayList<>();
        for (String id : cultures) {
            TradeTable t = AlienTrades.table(id);
            for (TradeTable other : seen) {
                assertFalse(t == other || t.rows().equals(other.rows()), id + " shares a table");
            }
            seen.add(t);
            for (int tier = 1; tier <= 5; tier++) {
                assertTrue(t.rowsUpTo(tier).size() > t.rowsUpTo(tier - 1).size(), id + " has no tier " + tier + " offer");
            }
        }
    }

    @Test
    void cultureTablesBuyTheirNativeGood() {
        assertBuys(AlienTrades.EMBER_KIN_TABLE, "nerospace:ash_dust");
        assertBuys(AlienTrades.FROSTBOUND_TABLE, "nerospace:frost_shard");
        assertBuys(AlienTrades.DUSKBORN_TABLE, "nerospace:shadow_lichen");
        for (VillagerCulture c : VillagerCultures.ALL) {
            assertBuys(c.trades(), c.giftItem());
        }
    }

    @Test
    void unknownCultureFallsBackToXertz() {
        assertSame(AlienTrades.XERTZ_TABLE, AlienTrades.table("nerospace:nobody"));
        assertSame(AlienTrades.XERTZ_TABLE, AlienTrades.table(null));
    }

    @Test
    void salvageDroneTradesPartsForHematiteInOneTier() {
        TradeTable t = AlienTrades.SALVAGE_TABLE;
        assertFalse(t.rows().isEmpty());
        for (TradeTable.Row row : t.rows()) {
            assertEquals(1, row.tier());
            assertEquals("nerospace:raw_hematite", row.costA());
            assertEquals(null, row.costB());
        }
        assertTrue(VillagerCultures.byId(AlienTrades.SALVAGE).isEmpty(), "the drone is not a culture");
    }

    @Test
    void everyNerospaceItemInATableExists() throws Exception {
        Path items = itemsDir();
        for (TradeTable t : AlienTrades.TABLES.values()) {
            for (TradeTable.Row row : t.rows()) {
                for (String id : new String[] {row.costA(), row.costB(), row.result()}) {
                    if (id != null && id.startsWith("nerospace:")) {
                        Path def = items.resolve(id.substring("nerospace:".length()) + ".json");
                        assertTrue(Files.isRegularFile(def), "no item definition for " + id);
                    }
                }
            }
        }
    }

    private static void assertBuys(TradeTable t, String good) {
        assertTrue(t.rows().stream().anyMatch(r -> r.costA().equals(good) && r.result().equals("minecraft:emerald")),
                "table does not buy " + good);
    }

    private static Path itemsDir() {
        for (Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath(); p != null; p = p.getParent()) {
            Path c = p.resolve("common/src/main/resources/assets/nerospace/items");
            if (Files.isDirectory(c)) {
                return c;
            }
            c = p.resolve("src/main/resources/assets/nerospace/items");
            if (Files.isDirectory(c)) {
                return c;
            }
        }
        throw new IllegalStateException("items dir not found");
    }
}
