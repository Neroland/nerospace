package za.co.neroland.nerospace.world;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.registry.ModEntities;

/**
 * The Greenxertz surface biome (Phase 3). Datapack-registered via the {@code RegistrySetBuilder} in
 * {@code datagen/DataGenerators}. Terrain shape comes from the reused {@code minecraft:overworld}
 * noise settings; this biome supplies the green palette, the dimension ores, and cave carvers. Mob
 * spawns are intentionally empty — life &amp; danger arrive in Phase 5.
 */
public final class ModBiomes {

    public static final ResourceKey<Biome> GREENXERTZ = ResourceKey.create(
            Registries.BIOME, Identifier.fromNamespaceAndPath(Nerospace.MODID, "greenxertz"));

    /** Cindara surface biome (Phase 7) — barren volcanic ash; dark palette. */
    public static final ResourceKey<Biome> CINDARA = ResourceKey.create(
            Registries.BIOME, Identifier.fromNamespaceAndPath(Nerospace.MODID, "cindara"));

    private ModBiomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        greenxertz(context);
        cindara(context);
    }

    private static void greenxertz(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);

        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        generation.addCarver(Carvers.CAVE);
        generation.addCarver(Carvers.CAVE_EXTRA_UNDERGROUND);
        generation.addCarver(Carvers.CANYON);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES,
                placedFeatures.getOrThrow(ModPlacedFeatures.NEROSTEEL_ORE_PLACED));
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES,
                placedFeatures.getOrThrow(ModPlacedFeatures.XERTZ_QUARTZ_ORE_PLACED));

        // MC 26.1 trimmed BiomeSpecialEffects.Builder to water + grass/foliage colors; fog/sky/water-fog
        // colors are no longer set here. The green surface palette comes from the grass/foliage overrides.
        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .waterColor(0x3A8E63)
                .grassColorOverride(0x5BD46A)
                .foliageColorOverride(0x4FB85A)
                .build();

        // Phase 5 — life & danger: the planet's three native creatures.
        MobSpawnSettings spawns = new MobSpawnSettings.Builder()
                .addSpawn(MobCategory.MONSTER, 12,
                        new MobSpawnSettings.SpawnerData(ModEntities.XERTZ_STALKER.get(), 1, 2))
                .addSpawn(MobCategory.CREATURE, 10,
                        new MobSpawnSettings.SpawnerData(ModEntities.QUARTZ_CRAWLER.get(), 1, 3))
                .addSpawn(MobCategory.AMBIENT, 8,
                        new MobSpawnSettings.SpawnerData(ModEntities.GREENLING.get(), 2, 4))
                .build();

        Biome biome = new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.8F)
                .downfall(0.0F)
                .specialEffects(effects)
                .mobSpawnSettings(spawns)
                .generationSettings(generation.build())
                .build();

        context.register(GREENXERTZ, biome);
    }

    private static void cindara(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);

        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        generation.addCarver(Carvers.CAVE);
        generation.addCarver(Carvers.CAVE_EXTRA_UNDERGROUND);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES,
                placedFeatures.getOrThrow(ModPlacedFeatures.CINDRITE_ORE_PLACED));

        // Dark, ashen palette to contrast green Greenxertz. (Barren surface shows little foliage, but
        // the dark water + muted grass/foliage tints read as a scorched world.)
        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .waterColor(0x70402A)
                .grassColorOverride(0x4A3A33)
                .foliageColorOverride(0x5A3A2A)
                .build();

        // A single hostile predator roams Cindara.
        MobSpawnSettings spawns = new MobSpawnSettings.Builder()
                .addSpawn(MobCategory.MONSTER, 14,
                        new MobSpawnSettings.SpawnerData(ModEntities.CINDER_STALKER.get(), 1, 2))
                .build();

        Biome biome = new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(2.0F)
                .downfall(0.0F)
                .specialEffects(effects)
                .mobSpawnSettings(spawns)
                .generationSettings(generation.build())
                .build();

        context.register(CINDARA, biome);
    }
}
