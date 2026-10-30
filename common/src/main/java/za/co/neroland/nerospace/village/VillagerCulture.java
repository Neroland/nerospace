package za.co.neroland.nerospace.village;

import za.co.neroland.nerospace.entity.AlienVillager;

/**
 * One villager culture (D8): which species it is on disk, which trade table it uses, whether it builds Mega Cities,
 * and whether it only settles the Nyxara twilight band. Plain data; the build palette is looked up by {@link #id()}
 * in {@code world.AlienBuild} and the trade table in {@link AlienTrades}.
 *
 * @param id        culture id ({@code nerospace:<name>}), the value of {@code PlanetTraits.villagerCulture()}
 * @param legacy    the ordinal-persisted species enum (never replaced by this id in NBT)
 * @param megaCity  whether this culture's planet grows Mega Cities
 * @param bandOnly  whether settlements are confined to {@code #nerospace:nyxara_band}
 * @param giftItem  the culture's favourite gift on top of the shared ones (the item its table "buys")
 */
public record VillagerCulture(String id, AlienVillager.Planet legacy, boolean megaCity, boolean bandOnly,
        String giftItem) {

    public TradeTable trades() {
        return AlienTrades.table(this.id);
    }
}
