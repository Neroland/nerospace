package za.co.neroland.nerospace.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;


import za.co.neroland.nerospace.NerospaceCommon;

/**
 * The nerospace planet dimensions, ported cross-loader. In 26.x the dimensions themselves are pure
 * datapack data — {@code data/nerospace/dimension/*.json} (+ the custom {@code dimension_type/space.json}
 * and {@code worldgen/biome/*.json}), which load identically on NeoForge and Fabric with no Java
 * registration. This class only holds the {@link ResourceKey}s code uses to address/teleport into them
 * (the rocket travel mechanic, the alien villager's per-planet variant, …). The datagen bootstrap that
 * authored the JSON is not needed at runtime, so it is intentionally omitted here.
 */
public final class ModDimensions {

    public static final ResourceKey<LevelStem> GREENXERTZ_STEM = stem("greenxertz");
    public static final ResourceKey<Level> GREENXERTZ_LEVEL = level("greenxertz");

    public static final ResourceKey<LevelStem> CINDARA_STEM = stem("cindara");
    public static final ResourceKey<Level> CINDARA_LEVEL = level("cindara");

    public static final ResourceKey<LevelStem> GLACIRA_STEM = stem("glacira");
    public static final ResourceKey<Level> GLACIRA_LEVEL = level("glacira");

    public static final ResourceKey<LevelStem> STATION_STEM = stem("station");
    public static final ResourceKey<Level> STATION_LEVEL = level("station");

    private static ResourceKey<LevelStem> stem(String name) {
        return ResourceKey.create(Registries.LEVEL_STEM, NerospaceCommon.id(name));
    }

    private static ResourceKey<Level> level(String name) {
        return ResourceKey.create(Registries.DIMENSION, NerospaceCommon.id(name));
    }

    private ModDimensions() {
    }
}
