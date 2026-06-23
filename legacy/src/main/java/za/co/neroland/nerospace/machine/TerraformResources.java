package za.co.neroland.nerospace.machine;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.Config;

/**
 * Resolves the configurable Tier-3 terraform ore list (terraform design §2.2) into {@link Block}s.
 * Defaults to Nerospace ores so seeding doesn't trivialise vanilla mining. The list is resolved
 * lazily and cached; an unknown id is skipped.
 */
public final class TerraformResources {

    private static List<Block> cached;
    private static List<? extends String> cachedFrom;

    private TerraformResources() {
    }

    @Nullable
    public static Block pickOre(RandomSource rnd) {
        List<Block> ores = resolve();
        return ores.isEmpty() ? null : ores.get(rnd.nextInt(ores.size()));
    }

    private static synchronized List<Block> resolve() {
        List<? extends String> from = Config.TERRAFORM_RESOURCE_ORES.get();
        if (cached != null && from == cachedFrom) {
            return cached;
        }
        List<Block> out = new ArrayList<>();
        for (String id : from) {
            Identifier rl = Identifier.tryParse(id);
            if (rl == null) {
                continue;
            }
            Block block = BuiltInRegistries.BLOCK.getValue(rl);
            if (block != null && block != Blocks.AIR) {
                out.add(block);
            }
        }
        cached = out;
        cachedFrom = from;
        return out;
    }
}
