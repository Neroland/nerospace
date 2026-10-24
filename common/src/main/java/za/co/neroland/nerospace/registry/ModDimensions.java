package za.co.neroland.nerospace.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
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

    /**
     * The Overworld key — equal to (and interned with) {@code Level.OVERWORLD}, but built without touching the
     * {@code Level} class, so pure code and unit tests can use it without a loader present.
     */
    public static final ResourceKey<Level> OVERWORLD =
            ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("overworld"));

    public static final ResourceKey<LevelStem> GREENXERTZ_STEM = stem("greenxertz");
    public static final ResourceKey<Level> GREENXERTZ_LEVEL = level("greenxertz");

    public static final ResourceKey<LevelStem> CINDARA_STEM = stem("cindara");
    public static final ResourceKey<Level> CINDARA_LEVEL = level("cindara");

    public static final ResourceKey<LevelStem> GLACIRA_STEM = stem("glacira");
    public static final ResourceKey<Level> GLACIRA_LEVEL = level("glacira");

    public static final ResourceKey<LevelStem> STATION_STEM = stem("station");
    public static final ResourceKey<Level> STATION_LEVEL = level("station");

    // 2.0 worlds. Their dimension JSON ships in a later build stage; until it exists on a server the planet is
    // hidden everywhere (see Planets.isAvailable), so these keys are safe to reference now.
    public static final ResourceKey<Level> LUNA_LEVEL = level("luna");
    public static final ResourceKey<Level> MARS_LEVEL = level("mars");
    public static final ResourceKey<Level> NYXARA_LEVEL = level("nyxara");

    private static ResourceKey<LevelStem> stem(String name) {
        return ResourceKey.create(Registries.LEVEL_STEM, Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, name));
    }

    private static ResourceKey<Level> level(String name) {
        return ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, name));
    }

    private ModDimensions() {
    }
}
