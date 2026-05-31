package za.co.neroland.nerospace.world;

import java.util.List;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import za.co.neroland.nerospace.Nerospace;

/**
 * Placement of Nerospace configured features. Nerosium uses a triangular height distribution
 * peaking around y=16, similar to iron, spread across overworld biomes (the biome restriction is
 * applied by the biome modifier, not here).
 */
public final class ModPlacedFeatures {

    public static final ResourceKey<PlacedFeature> NEROSIUM_ORE_PLACED = registerKey("nerosium_ore_placed");
    // Phase 3 — Greenxertz dimension ores.
    public static final ResourceKey<PlacedFeature> NEROSTEEL_ORE_PLACED = registerKey("nerosteel_ore_placed");
    public static final ResourceKey<PlacedFeature> XERTZ_QUARTZ_ORE_PLACED = registerKey("xertz_quartz_ore_placed");

    private ModPlacedFeatures() {
    }

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        context.register(NEROSIUM_ORE_PLACED, new PlacedFeature(
                configuredFeatures.getOrThrow(ModConfiguredFeatures.NEROSIUM_ORE),
                List.of(
                        CountPlacement.of(8),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.triangle(VerticalAnchor.absolute(-24), VerticalAnchor.absolute(56)),
                        BiomeFilter.biome())));

        // Nerosteel: common metal across the Greenxertz column, peaking mid-depth.
        context.register(NEROSTEEL_ORE_PLACED, new PlacedFeature(
                configuredFeatures.getOrThrow(ModConfiguredFeatures.NEROSTEEL_ORE),
                List.of(
                        CountPlacement.of(10),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.triangle(VerticalAnchor.absolute(-32), VerticalAnchor.absolute(72)),
                        BiomeFilter.biome())));

        // Xertz quartz: plentiful like nether quartz, spread through the upper-to-mid range.
        context.register(XERTZ_QUARTZ_ORE_PLACED, new PlacedFeature(
                configuredFeatures.getOrThrow(ModConfiguredFeatures.XERTZ_QUARTZ_ORE),
                List.of(
                        CountPlacement.of(12),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(110)),
                        BiomeFilter.biome())));
    }

    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(Nerospace.MODID, name));
    }
}
