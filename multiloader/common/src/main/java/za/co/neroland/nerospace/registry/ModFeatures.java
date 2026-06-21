package za.co.neroland.nerospace.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;
import za.co.neroland.nerospace.world.HamletFeature;
import za.co.neroland.nerospace.world.MegaCityFeature;
import za.co.neroland.nerospace.world.RuinFeature;

/**
 * Custom worldgen feature types, ported cross-loader via {@link RegistrationProvider} over the vanilla
 * {@code FEATURE} registry (the root used a NeoForge {@code DeferredRegister}). The configured/placed
 * feature JSON (copied from the root's datagen) reference these by id; the Greenxertz biome lists the
 * placed features so they generate there.
 */
public final class ModFeatures {

    public static final RegistrationProvider<Feature<?>> FEATURES =
            RegistrationProvider.get(Registries.FEATURE, NerospaceCommon.MOD_ID);

    public static final RegistryEntry<HamletFeature> HAMLET =
            FEATURES.register("hamlet", key -> new HamletFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryEntry<RuinFeature> RUIN =
            FEATURES.register("ruin", key -> new RuinFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryEntry<MegaCityFeature> MEGA_CITY =
            FEATURES.register("mega_city", key -> new MegaCityFeature(NoneFeatureConfiguration.CODEC));

    private ModFeatures() {
    }

    public static void init() {
    }
}
