package za.co.neroland.nerospace.village;

import java.util.Optional;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;

import za.co.neroland.nerospace.registry.ModItems;

/**
 * Tier-gated trade tables for the Greenxertz alien villagers (ALIEN_VILLAGERS_DESIGN.md §6). Offers
 * are cumulative: a tier-N villager offers everything unlocked at tiers 1..N. The catalogue spans the
 * design's trade categories — universal materials (iron, diamond, food), nerospace progression
 * (nerosium / nerosteel ingots, rocket fuel) and rare alien goods (alien core) — using vanilla
 * emeralds as the currency so the trades are useful in any modpack.
 *
 * <p>Phase 2 ships a single baseline "Quartz Trader" catalogue. Per-profession trade pools (unlocked
 * by the buildings a village constructs) arrive in Phase 5.
 */
public final class AlienTrades {

    private static final float PRICE_MULT = 0.05F;

    private AlienTrades() {
    }

    /** Builds the cumulative offer list for a villager whose reputation with the viewer is {@code tier}. */
    public static MerchantOffers forTier(int tier) {
        MerchantOffers offers = new MerchantOffers();

        // T1 — Acquainted: basic universal goods + a use for raw quartz.
        if (tier >= 1) {
            offers.add(sell(ModItems.XERTZ_QUARTZ.get(), 12, Items.EMERALD, 1, 16, 1));
            offers.add(buy(Items.EMERALD, 1, Items.IRON_INGOT, 3, 16, 1));
            offers.add(buy(Items.EMERALD, 2, Items.BREAD, 6, 16, 1));
        }

        // T2 — Trusted: nerospace progression materials.
        if (tier >= 2) {
            offers.add(buy(Items.EMERALD, 4, ModItems.NEROSIUM_INGOT.get(), 1, 12, 5));
            offers.add(buy2(Items.EMERALD, 1, ModItems.RAW_NEROSTEEL.get(), 8,
                    ModItems.NEROSTEEL_INGOT.get(), 4, 12, 5));
            offers.add(sell(ModItems.ALIEN_FRAGMENT.get(), 4, Items.EMERALD, 1, 12, 2));
        }

        // T3 — Allied: rare universal + fuel.
        if (tier >= 3) {
            offers.add(buy(Items.EMERALD, 8, Items.DIAMOND, 1, 6, 10));
            offers.add(buy(Items.EMERALD, 5, ModItems.ROCKET_FUEL_CANISTER.get(), 1, 8, 8));
        }

        // T4 — Honored: rare alien goods (interim stand-in for the Phase 6 exclusive gear line).
        if (tier >= 4) {
            offers.add(buy2(Items.EMERALD, 12, ModItems.ALIEN_TECH_SCRAP.get(), 2,
                    ModItems.ALIEN_CORE.get(), 1, 4, 15));
        }

        // T5 — Kin: the best deals.
        if (tier >= 5) {
            offers.add(buy(Items.EMERALD, 18, Items.DIAMOND, 3, 4, 20));
            offers.add(buy2(ModItems.ALIEN_CORE.get(), 1, Items.EMERALD, 6,
                    ModItems.NEROSIUM_INGOT.get(), 8, 4, 20));
        }

        return offers;
    }

    /** Villager buys {@code cost} x{@code n}, pays {@code result} x{@code rc}. */
    private static MerchantOffer buy(ItemLike cost, int n, ItemLike result, int rc, int maxUses, int xp) {
        return new MerchantOffer(new ItemCost(cost, n), new ItemStack(result, rc), maxUses, xp, PRICE_MULT);
    }

    /** Two-cost variant (e.g. emeralds + a material) for {@code result}. */
    private static MerchantOffer buy2(ItemLike costA, int a, ItemLike costB, int b,
            ItemLike result, int rc, int maxUses, int xp) {
        return new MerchantOffer(new ItemCost(costA, a), Optional.of(new ItemCost(costB, b)),
                new ItemStack(result, rc), maxUses, xp, PRICE_MULT);
    }

    /** Alias for readability when the villager is buying raw goods for emeralds. */
    private static MerchantOffer sell(ItemLike cost, int n, ItemLike result, int rc, int maxUses, int xp) {
        return buy(cost, n, result, rc, maxUses, xp);
    }
}
