package za.co.neroland.nerospace.world.planet.mars.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.village.AlienTrades;
import za.co.neroland.nerospace.village.TradeTable;

/** The Salvage Drone's stock is finite: uses persist with the drone and restock once a day. */
class SalvageDroneStockTest {

    private static Path droneSource() {
        for (Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath(); p != null; p = p.getParent()) {
            Path f = p.resolve("common/src/main/java/za/co/neroland/nerospace/world/planet/mars/entity/SalvageDrone.java");
            if (Files.isRegularFile(f)) {
                return f;
            }
        }
        throw new IllegalStateException("SalvageDrone.java not found");
    }

    private static TradeTable.Row row(String result) {
        return AlienTrades.SALVAGE_TABLE.rows().stream().filter(r -> r.result().equals(result)).findFirst()
                .orElse(null);
    }

    @Test
    @DisplayName("restock: never stocked, a day later, or the clock went back; not before")
    void restockRule() {
        assertTrue(SalvageStock.due(0L, SalvageStock.NEVER));
        assertFalse(SalvageStock.due(1_000L, 1_000L));
        assertFalse(SalvageStock.due(1_000L + SalvageStock.RESTOCK_TICKS - 1, 1_000L));
        assertTrue(SalvageStock.due(1_000L + SalvageStock.RESTOCK_TICKS, 1_000L));
        assertTrue(SalvageStock.due(500L, 1_000L), "game time reset");
        assertEquals(24_000L, SalvageStock.RESTOCK_TICKS);
    }

    @Test
    @DisplayName("titanium is scarce and Alien Tech Scrap is one per restock")
    void scarceRows() {
        TradeTable.Row titanium = row("nerospace:titanium_ingot");
        if (titanium != null) {
            assertTrue(titanium.countA() >= 24, "titanium costs at least 24 raw hematite");
            assertTrue(titanium.maxUses() <= 2, "titanium at most twice per restock");
        }
        TradeTable.Row scrap = row("nerospace:alien_tech_scrap");
        if (scrap != null) {
            assertEquals(1, scrap.maxUses(), "Alien Tech Scrap once per restock");
        }
        for (TradeTable.Row r : AlienTrades.SALVAGE_TABLE.rows()) {
            assertTrue(r.maxUses() > 0 && r.maxUses() <= 12, r.result());
        }
    }

    @Test
    @DisplayName("the drone saves its offers (no player field) and closing a window keeps them")
    void dronePersistsStock() throws IOException {
        String src = Files.readString(droneSource(), StandardCharsets.UTF_8);
        assertTrue(src.contains("\"Offers\", MerchantOffers.CODEC"), "offers are saved and loaded");
        assertTrue(src.contains("\"LastRestock\""), "restock time is saved");
        assertTrue(src.contains("SalvageStock.due("), "restock goes through the rule");
        int set = src.indexOf("public void setTradingPlayer(");
        int end = src.indexOf('}', set);
        assertFalse(src.substring(set, end).contains("offers = null"), "closing the window must not reset the stock");
        assertFalse(src.contains("UUID"), "the drone keeps no player record");
    }
}
