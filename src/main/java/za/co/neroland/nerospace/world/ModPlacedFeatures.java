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
    /**
     * Overworld nerosteel (BALANCE_COMPAT_AUDIT.md §0): a rare, deep seam so the metal that gates the
     * power grid + first rocket is reachable BEFORE Greenxertz (which would otherwise be circular —
     * nerosteel gated Greenxertz, and Greenxertz held all the nerosteel). Deliberately scarcer and
     * deeper than the Greenxertz placement, which stays the abundant source.
     */
    public static final ResourceKey<PlacedFeature> NEROSTEEL_ORE_OVERWORLD_PLACED =
            registerKey("nerosteel_ore_overworld_placed");
    public static final ResourceKey<PlacedFeature> XERTZ_QUARTZ_ORE_PLACED = registerKey("xertz_quartz_ore_placed");
    // Phase 7 — Cindara dimension ore.
    public static final ResourceKey<PlacedFeature> CINDRITE_ORE_PLACED = registerKey("cindrite_ore_placed");
    // Glacira dimension ore (NEW_DESTINATION_DESIGN.md).
    public static final ResourceKey<PlacedFeature> GLACITE_ORE_PLACED = registerKey("glacite_ore_placed");
    /** Rare surface alien hamlet (Phase 3). */
    public static final ResourceKey<PlacedFeature> HAMLET_PLACED = registerKey("hamlet_placed");
    public static final ResourceKey<PlacedFeature> RUIN_PLACED = registerKey("ruin_placed");

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

        // Overworld nerosteel: rare and deep — a quarter the Greenxertz frequency, peaking below 0 so
        // it reads as a deep-dig "tier-2 alloy" rather than a surface metal (BALANCE_COMPAT_AUDIT.md §0).
        context.register(NEROSTEEL_ORE_OVERWORLD_PLACED, new PlacedFeature(
                configuredFeatures.getOrThrow(ModConfiguredFeatures.NEROSTEEL_ORE),
                List.of(
                        CountPlacement.of(4),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.triangle(VerticalAnchor.absolute(-56), VerticalAnchor.absolute(16)),
                        BiomeFilter.biome())));

        // Xertz quartz: plentiful like nether quartz, spread through the upper-to-mid range.
        context.register(XERTZ_QUARTZ_ORE_PLACED, new PlacedFeature(
                configuredFeatures.getOrThrow(ModConfiguredFeatures.XERTZ_QUARTZ_ORE),
                List.of(
                        CountPlacement.of(12),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(110)),
                        BiomeFilter.biome())));

        // Cindrite: a rarer Cindara crystal, deep-to-mid.
        context.register(CINDRITE_ORE_PLACED, new PlacedFeature(
                configuredFeatures.getOrThrow(ModConfiguredFeatures.CINDRITE_ORE),
                List.of(
                        CountPlacement.of(7),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.triangle(VerticalAnchor.absolute(-48), VerticalAnchor.absolute(48)),
                        BiomeFilter.biome())));

        // Glacite: rarer like cindrite, deep-to-mid in the Glacira column.
        context.register(GLACITE_ORE_PLACED, new PlacedFeature(
                configuredFeatures.getOrThrow(ModConfiguredFeatures.GLACITE_ORE),
                List.of(
                        CountPlacement.of(7),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.triangle(VerticalAnchor.absolute(-48), VerticalAnchor.absolute(48)),
                        BiomeFilter.biome())));

        context.register(HAMLET_PLACED, new PlacedFeature(
                configuredFeatures.getOrThrow(ModConfiguredFeatures.HAMLET),
                List.of(
                        net.minecraft.world.level.levelgen.placement.RarityFilter.onAverageOnceEvery(40),
                        InSquarePlacement.spread(),
                        net.minecraft.world.level.levelgen.placement.HeightmapPlacement.onHeightmap(
                                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG),
                        BiomeFilter.biome())));

        context.register(RUIN_PLACED, new PlacedFeature(
                configuredFeatures.getOrThrow(ModConfiguredFeatures.RUIN),
                List.of(
                        net.minecraft.world.level.levelgen.placement.RarityFilter.onAverageOnceEvery(120),
                        InSquarePlacement.spread(),
                        net.minecraft.world.level.levelgen.placement.HeightmapPlacement.onHeightmap(
                                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG),
                        BiomeFilter.biome())));
    }

    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(Nerospace.MODID, name));
    }
}
