package za.co.neroland.nerospace.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Cross-loader {@link ResourceKey} handles for the terraform biomes. Unlike the root (which registers
 * these via a datagen {@code BootstrapContext}), the multiloader ships the biomes as committed datapack
 * JSON (under {@code data/nerospace/worldgen/biome/}); this class only exposes the keys so the
 * {@code TerraformConversion} engine can look the biomes up and write them onto converted columns.
 *
 * <p>{@link #TERRAFORMED} is the vibrant intermediate look (stages 1–2); the three mature biomes are
 * the per-planet stage-3 payoff (DEEPER_TERRAFORM_DESIGN.md §4). The planet surface biomes
 * (greenxertz/cindara/glacira) are pure datapack JSON with no Java handle needed.</p>
 */
public final class ModBiomes {

    /** Intermediate terraformed look (stages 1–2): neon emerald/turquoise. */
    public static final @org.jspecify.annotations.NonNull ResourceKey<Biome> TERRAFORMED = key("terraformed");

    /** Greenxertz mature stage-3 biome: natural lush meadow. */
    public static final @org.jspecify.annotations.NonNull ResourceKey<Biome> TERRAFORMED_MEADOW = key("terraformed_meadow");

    /** Cindara mature stage-3 biome: warm gold-green savanna. */
    public static final @org.jspecify.annotations.NonNull ResourceKey<Biome> TERRAFORMED_SAVANNA = key("terraformed_savanna");

    /** Glacira mature stage-3 biome: cold sage-green tundra. */
    public static final @org.jspecify.annotations.NonNull ResourceKey<Biome> TERRAFORMED_TUNDRA = key("terraformed_tundra");

    private ModBiomes() {
    }

    private static @org.jspecify.annotations.NonNull ResourceKey<Biome> key(
            @org.jspecify.annotations.NonNull String name) {
        return ResourceKey.create(Registries.BIOME,
                NerospaceCommon.id(NerospaceCommon.requireNonNull(name)));
    }
}
