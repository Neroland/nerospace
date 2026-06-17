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

    /** Glacira surface biome (NEW_DESTINATION_DESIGN.md) — frozen ice moon; pale cyan palette. */
    public static final ResourceKey<Biome> GLACIRA = ResourceKey.create(
            Registries.BIOME, Identifier.fromNamespaceAndPath(Nerospace.MODID, "glacira"));

    /**
     * Terraformed biome (terraform design): applied to columns the Terraformer converts, so livable
     * ground reads unmistakably as "terraformed" — a vibrant neon emerald/turquoise palette (lush, but
     * with an alien neon glow) that stands apart from the dead planet around it. With deeper
     * terraforming (DEEPER_TERRAFORM_DESIGN.md §1) this is the INTERMEDIATE look (stages 1–2, "raw
     * terraforming chemistry"); stage 3 settles into the mature per-planet biomes below.
     */
    public static final ResourceKey<Biome> TERRAFORMED = ResourceKey.create(
            Registries.BIOME, Identifier.fromNamespaceAndPath(Nerospace.MODID, "terraformed"));

    // Mature stage-3 biomes (DEEPER_TERRAFORM_DESIGN.md §4) — natural per-planet palettes with REAL
    // weather: the visible "this planet has an atmosphere now" payoff.

    /** Greenxertz matures into a natural lush meadow (the neon settles down); rain. */
    public static final ResourceKey<Biome> TERRAFORMED_MEADOW = ResourceKey.create(
            Registries.BIOME, Identifier.fromNamespaceAndPath(Nerospace.MODID, "terraformed_meadow"));

    /** Cindara matures into a warm gold-green savanna with a scorched-earth memory; rare-feel rain. */
    public static final ResourceKey<Biome> TERRAFORMED_SAVANNA = ResourceKey.create(
            Registries.BIOME, Identifier.fromNamespaceAndPath(Nerospace.MODID, "terraformed_savanna"));

    /** Glacira matures into a cold sage-green tundra; SNOW accumulates (and lakes refreeze). */
    public static final ResourceKey<Biome> TERRAFORMED_TUNDRA = ResourceKey.create(
            Registries.BIOME, Identifier.fromNamespaceAndPath(Nerospace.MODID, "terraformed_tundra"));

    private ModBiomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        greenxertz(context);
        cindara(context);
        glacira(context);
        terraformed(context);
        matureBiome(context, TERRAFORMED_MEADOW, 0.8F, 0.8F,
                0x3F76E4, 0x59C93C, 0x3FB04A, ModEntities.MEADOW_LOPER.get());
        matureBiome(context, TERRAFORMED_SAVANNA, 1.2F, 0.3F,
                0x4C8FBF, 0xBFB755, 0xAEA42A, ModEntities.EMBER_STRUTTER.get());
        matureBiome(context, TERRAFORMED_TUNDRA, -0.3F, 0.5F,
                0x3D57D6, 0x80B497, 0x60A17B, ModEntities.WOOLLY_DRIFT.get());
    }

    /**
     * A mature stage-3 biome (DEEPER_TERRAFORM_DESIGN.md §4): runtime-written like
     * {@link #terraformed}, so no generation features — palette + precipitation carry the payoff.
     * The planet's livestock species spawns here as the long-term backstop behind the active
     * herd seeding (§5).
     */
    private static void matureBiome(BootstrapContext<Biome> context, ResourceKey<Biome> key,
            float temperature, float downfall, int waterColor, int grassColor, int foliageColor,
            net.minecraft.world.entity.EntityType<?> livestock) {
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);

        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .waterColor(waterColor)
                .grassColorOverride(grassColor)
                .foliageColorOverride(foliageColor)
                .dryFoliageColorOverride(foliageColor)
                .build();

        MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder()
                .addSpawn(MobCategory.CREATURE, 10,
                        new MobSpawnSettings.SpawnerData(livestock, 2, 4));
        // Greenxertz alien villagers persist into the mature meadow (Phase 1: lighter accessory set).
        if (key == TERRAFORMED_MEADOW) {
            spawnBuilder.addSpawn(MobCategory.CREATURE, 5,
                    new MobSpawnSettings.SpawnerData(ModEntities.ALIEN_VILLAGER.get(), 1, 2));
        }
        MobSpawnSettings spawns = spawnBuilder.build();

        Biome biome = new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(temperature)
                .downfall(downfall)
                .specialEffects(effects)
                .mobSpawnSettings(spawns)
                .generationSettings(generation.build())
                .build();

        context.register(key, biome);
    }

    private static void terraformed(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);

        // Terrain is already generated; this biome is written onto converted columns at runtime, so it
        // carries no generation features — only the vibrant palette and a calm, livable feel.
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);

        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .waterColor(0x1FF0E0)              // bright turquoise water
                .grassColorOverride(0x2BFFB0)      // neon emerald grass
                .foliageColorOverride(0x19E8C0)    // teal-green foliage
                .dryFoliageColorOverride(0x19E8C0)
                .build();

        Biome biome = new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.8F)
                .downfall(0.4F)
                .specialEffects(effects)
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(generation.build())
                .build();

        context.register(TERRAFORMED, biome);
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
        // Alien hamlet outposts dot the surface (ALIEN_VILLAGERS_DESIGN.md §5, Phase 3).
        generation.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES,
                placedFeatures.getOrThrow(ModPlacedFeatures.HAMLET_PLACED));
        generation.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES,
                placedFeatures.getOrThrow(ModPlacedFeatures.RUIN_PLACED));
        generation.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES,
                placedFeatures.getOrThrow(ModPlacedFeatures.MEGA_CITY_PLACED));

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
                // Alien Villagers (Phase 0): small, sparse social groups on the surface.
                .addSpawn(MobCategory.CREATURE, 6,
                        new MobSpawnSettings.SpawnerData(ModEntities.ALIEN_VILLAGER.get(), 1, 3))
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
                .addSpawn(MobCategory.CREATURE, 3,
                        new MobSpawnSettings.SpawnerData(ModEntities.ALIEN_VILLAGER.get(), 1, 2))
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

    private static void glacira(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);

        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        generation.addCarver(Carvers.CAVE);
        generation.addCarver(Carvers.CAVE_EXTRA_UNDERGROUND);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES,
                placedFeatures.getOrThrow(ModPlacedFeatures.GLACITE_ORE_PLACED));

        // Pale frosted palette to mirror Cindara's heat with cold: ice-blue water, white-cyan tints.
        // (Airless — hasPrecipitation(false) — so the freeze reads through colour, not snowfall;
        // true ice/snow surface rules are deferred to the art-overhaul pass per the design doc.)
        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .waterColor(0x77C8E8)
                .grassColorOverride(0xC8E8F0)
                .foliageColorOverride(0xA8D8E8)
                .build();

        // A single hostile predator stalks the ice: the Frost Strider.
        MobSpawnSettings spawns = new MobSpawnSettings.Builder()
                .addSpawn(MobCategory.MONSTER, 14,
                        new MobSpawnSettings.SpawnerData(ModEntities.FROST_STRIDER.get(), 1, 2))
                .addSpawn(MobCategory.CREATURE, 3,
                        new MobSpawnSettings.SpawnerData(ModEntities.ALIEN_VILLAGER.get(), 1, 2))
                .build();

        Biome biome = new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(-0.5F)
                .downfall(0.0F)
                .specialEffects(effects)
                .mobSpawnSettings(spawns)
                .generationSettings(generation.build())
                .build();

        context.register(GLACIRA, biome);
    }
}
