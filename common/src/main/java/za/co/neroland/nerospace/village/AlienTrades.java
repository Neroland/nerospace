package za.co.neroland.nerospace.village;

import java.util.Map;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Tier-gated trade tables, one per villager culture (ALIEN_VILLAGERS_DESIGN.md §6). Offers are cumulative: a
 * tier-N villager offers everything unlocked at tiers 1..N. Emeralds are the currency so the trades are useful in
 * any modpack. The Xertz table is the 1.6.0 table, row for row, and is the fallback for a culture with no table.
 *
 * <p>Tables are plain data ({@link TradeTable}) keyed by culture id; this class only resolves item ids into
 * {@link MerchantOffers}. The Mars salvage drone's table lives here too ({@link #SALVAGE}), but it is not a
 * culture: no reputation, one tier.</p>
 */
public final class AlienTrades {

    private static final float PRICE_MULT = 0.05F;

    public static final String XERTZ = "nerospace:xertz";
    public static final String EMBER_KIN = "nerospace:ember_kin";
    public static final String FROSTBOUND = "nerospace:frostbound";
    public static final String DUSKBORN = "nerospace:duskborn";
    /** The Mars salvage drone's stock (not a culture). */
    public static final String SALVAGE = "nerospace:salvage_drone";

    private static final String EMERALD = "minecraft:emerald";
    private static final String DIAMOND = "minecraft:diamond";

    /** The 1.6.0 table, unchanged. */
    public static final TradeTable XERTZ_TABLE = new TradeTable.Builder()
            .tier(1)
            .buy("nerospace:xertz_quartz", 12, EMERALD, 1, 16, 1)
            .buy(EMERALD, 1, "minecraft:iron_ingot", 3, 16, 1)
            .buy(EMERALD, 2, "minecraft:bread", 6, 16, 1)
            .tier(2)
            .buy(EMERALD, 4, "nerospace:nerosium_ingot", 1, 12, 5)
            .buy2(EMERALD, 1, "nerospace:raw_nerosteel", 8, "nerospace:nerosteel_ingot", 4, 12, 5)
            .buy("nerospace:alien_fragment", 4, EMERALD, 1, 12, 2)
            .tier(3)
            .buy(EMERALD, 8, DIAMOND, 1, 6, 10)
            .buy(EMERALD, 5, "nerospace:rocket_fuel_canister", 1, 8, 8)
            .tier(4)
            .buy2(EMERALD, 12, "nerospace:alien_tech_scrap", 2, "nerospace:alien_core", 1, 4, 15)
            // Exclusive Artificer gear (§6.1).
            .buy(EMERALD, 16, "nerospace:xertz_resonator", 1, 2, 15)
            .tier(5)
            .buy(EMERALD, 18, DIAMOND, 3, 4, 20)
            .buy2("nerospace:alien_core", 1, EMERALD, 24, "nerospace:grav_striders", 1, 2, 20)
            .build();

    /** Cindara: cindrite, sulfur, heat-shield parts; buys ash. */
    public static final TradeTable EMBER_KIN_TABLE = new TradeTable.Builder()
            .tier(1)
            .buy("nerospace:ash_dust", 16, EMERALD, 1, 16, 1)
            .buy(EMERALD, 1, "nerospace:cindrite", 2, 16, 1)
            .buy(EMERALD, 1, "nerospace:sulfur", 4, 16, 1)
            .tier(2)
            .buy(EMERALD, 4, "nerospace:cindrite_block", 1, 12, 5)
            .buy(EMERALD, 3, "nerospace:magma_glass", 4, 12, 5)
            .buy("nerospace:alien_fragment", 4, EMERALD, 1, 12, 2)
            .tier(3)
            .buy(EMERALD, 10, "nerospace:oxygen_suit_heat_helmet", 1, 4, 10)
            .buy(EMERALD, 9, "nerospace:oxygen_suit_heat_boots", 1, 4, 10)
            .tier(4)
            .buy(EMERALD, 16, "nerospace:oxygen_suit_heat_chestplate", 1, 2, 15)
            .buy(EMERALD, 14, "nerospace:oxygen_suit_heat_leggings", 1, 2, 15)
            .tier(5)
            .buy(EMERALD, 18, DIAMOND, 3, 4, 20)
            .buy2("nerospace:alien_core", 1, EMERALD, 20, "nerospace:alien_tech_scrap", 4, 2, 20)
            .build();

    /** Glacira: glacite, methane ice, cold-shield parts; buys frost crystal. */
    public static final TradeTable FROSTBOUND_TABLE = new TradeTable.Builder()
            .tier(1)
            .buy("nerospace:frost_shard", 12, EMERALD, 1, 16, 1)
            .buy(EMERALD, 1, "nerospace:glacite", 2, 16, 1)
            .buy(EMERALD, 2, "nerospace:methane_ice", 4, 16, 1)
            .tier(2)
            .buy(EMERALD, 4, "nerospace:glacite_block", 1, 12, 5)
            .buy(EMERALD, 3, "nerospace:packed_cryo_ice", 4, 12, 5)
            .buy("nerospace:alien_fragment", 4, EMERALD, 1, 12, 2)
            .tier(3)
            .buy(EMERALD, 10, "nerospace:oxygen_suit_cold_helmet", 1, 4, 10)
            .buy(EMERALD, 9, "nerospace:oxygen_suit_cold_boots", 1, 4, 10)
            .tier(4)
            .buy(EMERALD, 16, "nerospace:oxygen_suit_cold_chestplate", 1, 2, 15)
            .buy(EMERALD, 14, "nerospace:oxygen_suit_cold_leggings", 1, 2, 15)
            .tier(5)
            .buy(EMERALD, 18, DIAMOND, 3, 4, 20)
            .buy2("nerospace:alien_core", 1, EMERALD, 20, "nerospace:helium3_canister", 4, 2, 20)
            .build();

    /** Nyxara: voidglass, violet (void) crystal, radiation-lined suit parts; buys shadow lichen. */
    public static final TradeTable DUSKBORN_TABLE = new TradeTable.Builder()
            .tier(1)
            .buy("nerospace:shadow_lichen", 12, EMERALD, 1, 16, 1)
            .buy(EMERALD, 2, "nerospace:voidglass", 2, 16, 1)
            .buy(EMERALD, 1, "nerospace:night_fibre", 3, 16, 1)
            .tier(2)
            .buy(EMERALD, 4, "nerospace:void_crystal", 2, 12, 5)
            .buy(EMERALD, 6, "nerospace:voidglass_lens", 1, 12, 5)
            .buy("nerospace:alien_fragment", 4, EMERALD, 1, 12, 2)
            .tier(3)
            .buy(EMERALD, 12, "nerospace:oxygen_suit_t2_helmet", 1, 4, 10)
            .buy(EMERALD, 11, "nerospace:oxygen_suit_t2_boots", 1, 4, 10)
            .tier(4)
            .buy(EMERALD, 18, "nerospace:oxygen_suit_t2_chestplate", 1, 2, 15)
            .buy(EMERALD, 16, "nerospace:oxygen_suit_t2_leggings", 1, 2, 15)
            .tier(5)
            .buy(EMERALD, 20, "nerospace:gravity_shard", 4, 4, 20)
            .buy2("nerospace:alien_core", 1, EMERALD, 24, "nerospace:void_shard", 2, 2, 20)
            .build();

    /** The Mars salvage drone: rover parts for raw hematite. One tier, no reputation. */
    public static final TradeTable SALVAGE_TABLE = new TradeTable.Builder()
            .tier(1)
            .buy("nerospace:raw_hematite", 6, "nerospace:hull_plating", 2, 12, 2)
            .buy("nerospace:raw_hematite", 8, "nerospace:truss", 4, 12, 2)
            .buy("nerospace:raw_hematite", 10, "nerospace:titanium_ingot", 1, 8, 4)
            .buy("nerospace:raw_hematite", 12, "nerospace:solar_wing", 1, 6, 6)
            .buy("nerospace:raw_hematite", 16, "nerospace:frame_casing", 1, 4, 8)
            .buy("nerospace:raw_hematite", 20, "nerospace:alien_tech_scrap", 1, 4, 10)
            .build();

    /** Every table by id (four cultures + the salvage drone). */
    public static final Map<String, TradeTable> TABLES = Map.of(
            XERTZ, XERTZ_TABLE,
            EMBER_KIN, EMBER_KIN_TABLE,
            FROSTBOUND, FROSTBOUND_TABLE,
            DUSKBORN, DUSKBORN_TABLE,
            SALVAGE, SALVAGE_TABLE);

    private AlienTrades() {
    }

    /** The table for a culture id; the Xertz table when the id has none (old saves, unknown cultures). */
    public static TradeTable table(String cultureId) {
        TradeTable t = cultureId == null ? null : TABLES.get(cultureId);
        return t == null ? XERTZ_TABLE : t;
    }

    /** The 1.6.0 entry point: the Xertz table at {@code tier}. */
    public static MerchantOffers forTier(int tier) {
        return forCulture(XERTZ, tier);
    }

    public static MerchantOffers forCulture(Identifier culture, int tier) {
        return forCulture(culture == null ? null : culture.toString(), tier);
    }

    public static MerchantOffers forCulture(String cultureId, int tier) {
        return resolve(table(cultureId), tier);
    }

    /** Resolves a table into offers. A row naming an item that is not registered is skipped, never a crash. */
    public static MerchantOffers resolve(TradeTable table, int tier) {
        MerchantOffers offers = new MerchantOffers();
        for (TradeTable.Row row : table.rowsUpTo(tier)) {
            Optional<Item> a = item(row.costA());
            Optional<Item> r = item(row.result());
            if (a.isEmpty() || r.isEmpty()) {
                continue;
            }
            Optional<ItemCost> b = Optional.empty();
            if (row.costB() != null) {
                Optional<Item> bi = item(row.costB());
                if (bi.isEmpty()) {
                    continue;
                }
                b = Optional.of(new ItemCost(bi.get(), row.countB()));
            }
            offers.add(new MerchantOffer(new ItemCost(a.get(), row.countA()), b,
                    new ItemStack(r.get(), row.resultCount()), row.maxUses(), row.xp(), PRICE_MULT));
        }
        return offers;
    }

    private static Optional<Item> item(String id) {
        Identifier key = Identifier.tryParse(id);
        return key == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(key);
    }
}
