package za.co.neroland.nerospace.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.world.HamletFeature;
import za.co.neroland.nerospace.world.MegaCityFeature;
import za.co.neroland.nerospace.world.RuinFeature;

/** Custom worldgen features: hamlet outpost (P3), ancient ruin (P7), mega-city (finale). */
public final class ModFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Nerospace.MODID);

    public static final Supplier<Feature<NoneFeatureConfiguration>> HAMLET =
            FEATURES.register("hamlet", () -> new HamletFeature(NoneFeatureConfiguration.CODEC));

    public static final Supplier<Feature<NoneFeatureConfiguration>> RUIN =
            FEATURES.register("ruin", () -> new RuinFeature(NoneFeatureConfiguration.CODEC));

    public static final Supplier<Feature<NoneFeatureConfiguration>> MEGA_CITY =
            FEATURES.register("mega_city", () -> new MegaCityFeature(NoneFeatureConfiguration.CODEC));

    private ModFeatures() {
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}
