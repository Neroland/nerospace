package za.co.neroland.nerospace.datagen;

import java.util.List;
import java.util.Set;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.world.ModBiomeModifiers;
import za.co.neroland.nerospace.world.ModBiomes;
import za.co.neroland.nerospace.world.ModConfiguredFeatures;
import za.co.neroland.nerospace.world.ModPlacedFeatures;

/**
 * Single entry point for Nerospace data generation. Runs via the {@code data} (clientData) run
 * configuration declared in build.gradle: {@code ./gradlew runData}.
 */
@EventBusSubscriber(modid = Nerospace.MODID)
public final class DataGenerators {

    /** Datapack registry entries (worldgen + biome modifiers). */
    public static final RegistrySetBuilder REGISTRY_SET_BUILDER = new RegistrySetBuilder()
            .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap)
            .add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ModBiomeModifiers::bootstrap)
            // Phase 3 — Greenxertz dimension (reuses the vanilla overworld dimension type).
            .add(Registries.BIOME, ModBiomes::bootstrap)
            .add(Registries.LEVEL_STEM, ModDimensions::bootstrapStem);

    private DataGenerators() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        // Register datapack registry objects first so later providers can look them up.
        event.createDatapackRegistryObjects(REGISTRY_SET_BUILDER);

        // Client assets.
        event.createProvider(ModModelProvider::new);
        event.createProvider(ModLanguageProvider::new);

        // Server data.
        event.createProvider(ModRecipeProvider.Runner::new);
        event.createProvider((output, registries) -> new LootTableProvider(
                output,
                Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(
                        ModBlockLootSubProvider::new,
                        LootContextParamSets.BLOCK)),
                registries));

        // Block + item tags (item provider is wired to the block provider's contents).
        event.createBlockAndItemTags(ModBlockTagProvider::new, ModItemTagProvider::new);
    }
}
