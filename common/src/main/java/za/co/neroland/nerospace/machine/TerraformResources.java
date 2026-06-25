package za.co.neroland.nerospace.machine;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import org.jetbrains.annotations.Nullable;

/**
 * Resolves the Tier-3 terraform ore list (terraform design §2.2) into {@link Block}s. Defaults to
 * Nerospace ores so seeding doesn't trivialise vanilla mining; resolved lazily and cached.
 *
 * <p>Cross-loader port note: the root drew the id list from {@code Config.TERRAFORM_RESOURCE_ORES};
 * the config seam is deferred, so the list is inlined to the root's shipped default.</p>
 */
public final class TerraformResources {

    /** Inlined from {@code Config.TERRAFORM_RESOURCE_ORES} default until the config seam lands. */
    private static final List<String> ORE_IDS = java.util.Objects.requireNonNull(List.of(
            "nerospace:nerosteel_ore", "nerospace:xertz_quartz_ore", "nerospace:nerosium_ore"));

    private static @Nullable List<Block> cached;

    private TerraformResources() {
    }

    @Nullable
    public static Block pickOre(RandomSource rnd) {
        List<Block> ores = resolve();
        return ores.isEmpty() ? null : ores.get(rnd.nextInt(ores.size()));
    }

    private static synchronized List<Block> resolve() {
        List<Block> local = cached;
        if (local != null) {
            return local;
        }
        List<Block> out = new ArrayList<>();
        for (String id : ORE_IDS) {
            Identifier rl = Identifier.tryParse(java.util.Objects.requireNonNull(id));
            if (rl == null) {
                continue;
            }
            Block block = BuiltInRegistries.BLOCK.getValue(rl);
            if (block != Blocks.AIR) {
                out.add(block);
            }
        }
        cached = out;
        return out;
    }
}
