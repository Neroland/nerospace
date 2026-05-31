package za.co.neroland.nerospace.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.world.ModBiomes;

/**
 * The Greenxertz dimension (Phase 3): a level stem (the dimension itself) plus the
 * {@code ResourceKey<Level>} used to teleport into it.
 *
 * <p>The stem and the level key share the path {@code greenxertz}, so the datapack file lands at
 * {@code dimension/greenxertz.json} and the dimension is addressable in-game as
 * {@code nerospace:greenxertz}. The chunk generator reuses vanilla {@code minecraft:overworld} noise
 * settings with a {@link FixedBiomeSource} pinned to {@link ModBiomes#GREENXERTZ}.</p>
 *
 * <p><b>Dimension type:</b> we reuse the vanilla overworld {@link DimensionType} (skylight + normal
 * day/night). MC 26.1 reworked {@code DimensionType} construction (skybox, cardinal lighting,
 * environment attributes, timelines), so a bespoke sky/atmosphere is deferred to a later polish pass;
 * the green surface palette comes from the biome for now.</p>
 */
public final class ModDimensions {

    public static final ResourceKey<LevelStem> GREENXERTZ_STEM = ResourceKey.create(
            Registries.LEVEL_STEM, Identifier.fromNamespaceAndPath(Nerospace.MODID, "greenxertz"));

    public static final ResourceKey<Level> GREENXERTZ_LEVEL = ResourceKey.create(
            Registries.DIMENSION, Identifier.fromNamespaceAndPath(Nerospace.MODID, "greenxertz"));

    private ModDimensions() {
    }

    public static void bootstrapStem(BootstrapContext<LevelStem> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<DimensionType> dimensionTypes = context.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<NoiseGeneratorSettings> noiseSettings = context.lookup(Registries.NOISE_SETTINGS);

        Holder<DimensionType> typeHolder = dimensionTypes.getOrThrow(BuiltinDimensionTypes.OVERWORLD);

        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(
                new FixedBiomeSource(biomes.getOrThrow(ModBiomes.GREENXERTZ)),
                noiseSettings.getOrThrow(NoiseGeneratorSettings.OVERWORLD));

        context.register(GREENXERTZ_STEM, new LevelStem(typeHolder, generator));
    }
}
