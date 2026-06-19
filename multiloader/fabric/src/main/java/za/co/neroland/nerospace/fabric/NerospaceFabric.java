package za.co.neroland.nerospace.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Fabric entry point. Shared init registers content eagerly, then Fabric-side
 * wiring: creative-tab fill (Fabric API creative-tab module) and biome injection
 * of the ore placed-features (Fabric API biome module — the counterpart to the
 * NeoForge {@code biome_modifier} JSON).
 */
public final class NerospaceFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        NerospaceCommon.LOGGER.info("[Nerospace] Fabric bootstrap");
        NerospaceCommon.init();

        ModItems.creativeTabItems().forEach((tab, items) ->
                CreativeModeTabEvents.modifyOutputEvent(tab)
                        .register(output -> items.forEach(output::accept)));

        addOverworldOre("nerosium_ore_placed");
    }

    private static void addOverworldOre(String placedFeatureName) {
        ResourceKey<PlacedFeature> key = ResourceKey.create(
                Registries.PLACED_FEATURE,
                Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, placedFeatureName));
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                key);
    }
}
