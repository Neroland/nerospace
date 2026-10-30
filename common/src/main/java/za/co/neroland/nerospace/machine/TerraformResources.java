package za.co.neroland.nerospace.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import za.co.neroland.nerospace.world.planet.TerraformTable;
import za.co.neroland.nerospace.world.planet.TerraformTables;

/**
 * Resolves a planet's Tier-3 terraform ore seed set (terraform design) into {@link Block}s. The 1.x
 * planets and off-planet dimensions keep the 1.x list (Nerospace ores, so seeding doesn't trivialise vanilla
 * mining); the 2.0 worlds seed their own ores. Resolved lazily and cached per table; ids that are not registered
 * are skipped.
 */
public final class TerraformResources {

    private static final Map<String, List<Block>> CACHE = new ConcurrentHashMap<>();

    private TerraformResources() {
    }

    /** A random ore from the off-planet (1.x) list. */
    @Nullable
    public static Block pickOre(RandomSource rnd) {
        return pickOre(TerraformTables.LEGACY, rnd);
    }

    /** A random ore from {@code table}'s seed set, or null when none of its ids resolve. */
    @Nullable
    public static Block pickOre(TerraformTable table, RandomSource rnd) {
        List<Block> ores = CACHE.computeIfAbsent(table.langId(), k -> resolve(table.ores()));
        return ores.isEmpty() ? null : ores.get(rnd.nextInt(ores.size()));
    }

    private static List<Block> resolve(List<Identifier> ids) {
        List<Block> out = new ArrayList<>();
        for (Identifier id : ids) {
            if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                continue;
            }
            Block block = BuiltInRegistries.BLOCK.getValue(id);
            if (block != null && block != Blocks.AIR) {
                out.add(block);
            }
        }
        return List.copyOf(out);
    }
}
