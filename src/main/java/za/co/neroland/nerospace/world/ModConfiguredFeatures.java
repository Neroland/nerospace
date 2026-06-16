package za.co.neroland.nerospace.world;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.registry.ModBlocks;

/**
 * Configured features for Nerospace worldgen. Datapack-registered via the
 * {@code RegistrySetBuilder} in {@code datagen/DataGenerators}.
 */
public final class ModConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> NEROSIUM_ORE = registerKey("nerosium_ore");
    // Phase 3 — Greenxertz dimension ores.
    public static final ResourceKey<ConfiguredFeature<?, ?>> NEROSTEEL_ORE = registerKey("nerosteel_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> XERTZ_QUARTZ_ORE = registerKey("xertz_quartz_ore");
    // Phase 7 — Cindara dimension ore.
    public static final ResourceKey<ConfiguredFeature<?, ?>> CINDRITE_ORE = registerKey("cindrite_ore");
    // Glacira dimension ore (NEW_DESTINATION_DESIGN.md).
    public static final ResourceKey<ConfiguredFeature<?, ?>> GLACITE_ORE = registerKey("glacite_ore");
    /** Alien hamlet outpost (ALIEN_VILLAGERS_DESIGN.md §5, Phase 3). */
    public static final ResourceKey<ConfiguredFeature<?, ?>> HAMLET = registerKey("hamlet");
    public static final ResourceKey<ConfiguredFeature<?, ?>> RUIN = registerKey("ruin");

    private ModConfiguredFeatures() {
    }

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest stoneReplaceables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);

        List<OreConfiguration.TargetBlockState> nerosiumTargets = List.of(
                OreConfiguration.target(stoneReplaceables, ModBlocks.NEROSIUM_ORE.get().defaultBlockState()),
                OreConfiguration.target(deepslateReplaceables, ModBlocks.DEEPSLATE_NEROSIUM_ORE.get().defaultBlockState()));

        context.register(NEROSIUM_ORE, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(nerosiumTargets, 9)));

        // The Greenxertz biome uses overworld-style stone/deepslate, so both ores replace the same
        // stone/deepslate target tags. A single block per material is reused at all depths.
        List<OreConfiguration.TargetBlockState> nerosteelTargets = List.of(
                OreConfiguration.target(stoneReplaceables, ModBlocks.NEROSTEEL_ORE.get().defaultBlockState()),
                OreConfiguration.target(deepslateReplaceables, ModBlocks.NEROSTEEL_ORE.get().defaultBlockState()));
        context.register(NEROSTEEL_ORE, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(nerosteelTargets, 9)));

        List<OreConfiguration.TargetBlockState> xertzQuartzTargets = List.of(
                OreConfiguration.target(stoneReplaceables, ModBlocks.XERTZ_QUARTZ_ORE.get().defaultBlockState()),
                OreConfiguration.target(deepslateReplaceables, ModBlocks.XERTZ_QUARTZ_ORE.get().defaultBlockState()));
        context.register(XERTZ_QUARTZ_ORE, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(xertzQuartzTargets, 14)));

        // Cindara: cindrite scattered through the volcanic column.
        List<OreConfiguration.TargetBlockState> cindriteTargets = List.of(
                OreConfiguration.target(stoneReplaceables, ModBlocks.CINDRITE_ORE.get().defaultBlockState()),
                OreConfiguration.target(deepslateReplaceables, ModBlocks.CINDRITE_ORE.get().defaultBlockState()));
        context.register(CINDRITE_ORE, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(cindriteTargets, 8)));

        // Glacira: glacite crystal frozen through the icy column.
        List<OreConfiguration.TargetBlockState> glaciteTargets = List.of(
                OreConfiguration.target(stoneReplaceables, ModBlocks.GLACITE_ORE.get().defaultBlockState()),
                OreConfiguration.target(deepslateReplaceables, ModBlocks.GLACITE_ORE.get().defaultBlockState()));
        context.register(GLACITE_ORE, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(glaciteTargets, 8)));

        context.register(HAMLET, new ConfiguredFeature<>(
                za.co.neroland.nerospace.registry.ModFeatures.HAMLET.get(),
                net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE));

        context.register(RUIN, new ConfiguredFeature<>(
                za.co.neroland.nerospace.registry.ModFeatures.RUIN.get(),
                net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.fromNamespaceAndPath(Nerospace.MODID, name));
    }
}
