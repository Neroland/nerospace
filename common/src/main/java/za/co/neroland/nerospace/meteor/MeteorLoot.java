package za.co.neroland.nerospace.meteor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import za.co.neroland.nerospace.registry.ModItems;

/**
 * RNG contents of a meteor core (meteor-events design §5). Rolling is deterministic for a given
 * seed, so the {@link MeteorCoreBlockEntity} can roll once on placement and store the result — all
 * players who reach the meteor see identical loot and there is no re-roll exploit.
 *
 * <p>v1 keeps the loot table in code (sensible defaults). Every meteor guarantees a handful of
 * {@code alien_fragment} (the future scanner feedstock) plus a number of weighted bonus rolls drawn
 * from existing raw ores and the rarer alien items.</p>
 */
public final class MeteorLoot {

    /** A single weighted entry: an item, how many to give, and its selection weight. */
    private record Entry(ItemLike item, int min, int max, int weight) {
        int roll(RandomSource rng) {
            return this.min >= this.max ? this.min : this.min + rng.nextInt(this.max - this.min + 1);
        }
    }

    private MeteorLoot() {
    }

    /** The weighted bonus pool (existing ores are common; alien tech/core are the rare prizes). */
    private static List<Entry> pool() {
        List<Entry> pool = new ArrayList<>();
        pool.add(new Entry(ModItems.RAW_NEROSIUM.get(), 2, 5, 30));
        pool.add(new Entry(ModItems.RAW_NEROSTEEL.get(), 2, 5, 24));
        pool.add(new Entry(ModItems.XERTZ_QUARTZ.get(), 1, 4, 18));
        pool.add(new Entry(ModItems.ALIEN_FRAGMENT.get(), 2, 4, 16));
        pool.add(new Entry(ModItems.ALIEN_TECH_SCRAP.get(), 1, 2, 9));
        pool.add(new Entry(ModItems.ALIEN_CORE.get(), 1, 1, 3));
        return pool;
    }

    /**
     * Rolls a fresh set of stacks for a meteor core.
     *
     * @param rng         seeded source (use {@code RandomSource.create(seed)} for reproducibility)
     * @param bonusRolls  number of weighted bonus rolls on top of the guaranteed fragments
     */
    public static List<ItemStack> roll(RandomSource rng, int bonusRolls) {
        List<ItemStack> out = new ArrayList<>();
        // Guaranteed: a handful of alien fragments — every meteor seeds the scanner economy.
        out.add(new ItemStack(ModItems.ALIEN_FRAGMENT.get(), 3 + rng.nextInt(4)));

        List<Entry> pool = pool();
        int totalWeight = pool.stream().mapToInt(Entry::weight).sum();
        for (int i = 0; i < bonusRolls; i++) {
            int pick = rng.nextInt(totalWeight);
            for (Entry e : pool) {
                pick -= e.weight();
                if (pick < 0) {
                    out.add(new ItemStack(e.item(), e.roll(rng)));
                    break;
                }
            }
        }
        return out;
    }
}
