package za.co.neroland.nerospace.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.entity.AlienVillager;

/** Europa's Brinekin (2.4.0): the table of design 9.4, row for row, and the culture wiring. */
class BrinekinTradesTest {

    private static final String E = "minecraft:emerald";

    /** tier, costA, n, costB, b, result, rc, maxUses, xp. */
    private static final List<TradeTable.Row> BRINEKIN = List.of(
            new TradeTable.Row(1, "nerolandcore:brine_salt", 12, null, 0, E, 1, 16, 1),
            new TradeTable.Row(1, E, 1, null, 0, "nerospace:europa_ice_bricks", 4, 16, 1),
            new TradeTable.Row(1, E, 2, null, 0, "nerospace:brine_bucket", 1, 8, 1),
            new TradeTable.Row(2, E, 3, null, 0, "nerospace:deuterium_shard", 2, 12, 5),
            new TradeTable.Row(2, E, 3, null, 0, "nerospace:polished_vent_basalt", 4, 12, 5),
            new TradeTable.Row(2, "nerospace:alien_fragment", 4, null, 0, E, 1, 12, 2),
            new TradeTable.Row(3, E, 10, null, 0, "nerospace:oxygen_suit_cold_helmet", 1, 4, 10),
            new TradeTable.Row(3, E, 4, null, 0, "nerospace:oxidant_crystal", 4, 8, 8),
            new TradeTable.Row(4, E, 6, null, 0, "nerospace:tube_mat", 4, 6, 12),
            new TradeTable.Row(4, E, 14, null, 0, "nerospace:hydrothermal_vent", 1, 2, 15),
            new TradeTable.Row(5, E, 18, null, 0, "nerospace:deuterium_cell", 2, 4, 20),
            new TradeTable.Row(5, "nerospace:alien_core", 1, E, 20, "nerospace:rad_lining", 1, 2, 20));

    @Test
    void tableIsTheDesignTable() {
        assertEquals(BRINEKIN, AlienTrades.BRINEKIN_TABLE.rows());
        int[] expected = {3, 6, 8, 10, 12};
        for (int tier = 1; tier <= 5; tier++) {
            assertEquals(expected[tier - 1], AlienTrades.BRINEKIN_TABLE.rowsUpTo(tier).size(), "tier " + tier);
        }
        assertSame(AlienTrades.BRINEKIN_TABLE, AlienTrades.table(AlienTrades.BRINEKIN));
    }

    @Test
    void cultureIsWired() {
        VillagerCulture c = VillagerCultures.BRINEKIN;
        assertEquals("nerospace:brinekin", c.id());
        assertSame(AlienVillager.Planet.EUROPA, c.legacy());
        assertEquals(4, AlienVillager.Planet.EUROPA.ordinal());
        assertFalse(c.megaCity());
        assertFalse(c.bandOnly());
        assertEquals("nerolandcore:brine_salt", c.giftItem());
        assertSame(c, VillagerCultures.byId("nerospace:brinekin").orElseThrow());
        assertSame(c, VillagerCultures.byLegacy(AlienVillager.Planet.EUROPA));
        assertEquals(VillagerCultures.ALL.size() - 1, VillagerCultures.ALL.indexOf(c), "appended last");
        assertSame(AlienVillager.Planet.EUROPA, AlienVillager.Planet.byCulture(AlienVillager.Planet.EUROPA.culture()));
    }

    @Test
    void oneSuitPieceOnly() {
        long suits = AlienTrades.BRINEKIN_TABLE.rows().stream().filter(r -> r.result().contains("oxygen_suit")).count();
        assertEquals(1, suits, "the Brinekin sell the cold helmet only");
    }

    @Test
    void sellsNoDecorativeFind() {
        for (TradeTable.Row row : AlienTrades.BRINEKIN_TABLE.rows()) {
            for (String id : new String[] {row.costA(), row.costB(), row.result()}) {
                assertTrue(id == null || !id.contains("lamp") || id.equals("minecraft:sea_lantern"), id);
            }
        }
    }
}
