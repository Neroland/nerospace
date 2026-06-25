package za.co.neroland.nerospace.village;

import java.util.Optional;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Tier-gated trade tables for the Greenxertz alien villagers (ALIEN_VILLAGERS_DESIGN.md §6). Offers
 * are cumulative: a tier-N villager offers everything unlocked at tiers 1..N. Spans universal
 * materials, nerospace progression, rare alien goods, and — at the top tiers — the exclusive Artificer
 * gear. Emeralds are the currency so the trades are useful in any modpack.
 */
public final class AlienTrades {

    private static final float PRICE_MULT = 0.05F;

    private AlienTrades() {
    }

    public static MerchantOffers forTier(int tier) {
        MerchantOffers offers = new MerchantOffers();

        if (tier >= 1) {
            offers.add(sell(ModItems.XERTZ_QUARTZ.get(), 12, Items.EMERALD, 1, 16, 1));
            offers.add(buy(Items.EMERALD, 1, Items.IRON_INGOT, 3, 16, 1));
            offers.add(buy(Items.EMERALD, 2, Items.BREAD, 6, 16, 1));
        }
        if (tier >= 2) {
            offers.add(buy(Items.EMERALD, 4, ModItems.NEROSIUM_INGOT.get(), 1, 12, 5));
            offers.add(buy2(Items.EMERALD, 1, ModItems.RAW_NEROSTEEL.get(), 8,
                    ModItems.NEROSTEEL_INGOT.get(), 4, 12, 5));
            offers.add(sell(ModItems.ALIEN_FRAGMENT.get(), 4, Items.EMERALD, 1, 12, 2));
        }
        if (tier >= 3) {
            offers.add(buy(Items.EMERALD, 8, Items.DIAMOND, 1, 6, 10));
            offers.add(buy(Items.EMERALD, 5, ModItems.ROCKET_FUEL_CANISTER.get(), 1, 8, 8));
        }
        if (tier >= 4) {
            offers.add(buy2(Items.EMERALD, 12, ModItems.ALIEN_TECH_SCRAP.get(), 2,
                    ModItems.ALIEN_CORE.get(), 1, 4, 15));
            // Exclusive Artificer gear (§6.1).
            offers.add(buy(Items.EMERALD, 16, ModItems.XERTZ_RESONATOR.get(), 1, 2, 15));
        }
        if (tier >= 5) {
            offers.add(buy(Items.EMERALD, 18, Items.DIAMOND, 3, 4, 20));
            offers.add(buy2(ModItems.ALIEN_CORE.get(), 1, Items.EMERALD, 24,
                    ModItems.GRAV_STRIDERS.get(), 1, 2, 20));
        }
        return offers;
    }

    private static MerchantOffer buy(ItemLike cost, int n, ItemLike result, int rc, int maxUses, int xp) {
        return new MerchantOffer(new ItemCost(NerospaceCommon.requireNonNull(cost), n),
                new ItemStack(NerospaceCommon.requireNonNull(result), rc), maxUses, xp, PRICE_MULT);
    }

    private static MerchantOffer buy2(ItemLike costA, int a, ItemLike costB, int b,
            ItemLike result, int rc, int maxUses, int xp) {
        return new MerchantOffer(new ItemCost(NerospaceCommon.requireNonNull(costA), a),
                Optional.of(new ItemCost(NerospaceCommon.requireNonNull(costB), b)),
                new ItemStack(NerospaceCommon.requireNonNull(result), rc), maxUses, xp, PRICE_MULT);
    }

    private static MerchantOffer sell(ItemLike cost, int n, ItemLike result, int rc, int maxUses, int xp) {
        return buy(cost, n, result, rc, maxUses, xp);
    }
}
