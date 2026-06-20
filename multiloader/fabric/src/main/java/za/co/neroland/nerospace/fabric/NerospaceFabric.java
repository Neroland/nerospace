package za.co.neroland.nerospace.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Fabric entry point. Shared init registers content eagerly, then Fabric-side
 * wiring: creative-tab fill (Fabric API creative-tab module) and biome injection
 * of the ore placed-features (Fabric API biome module — the counterpart to the
 * NeoForge {@code biome_modifier} JSON).
 */
public final class NerospaceFabric implements ModInitializer {

    /** Mod-owned energy lookup; mirrors the NeoForge energy BlockCapability of the same id. */
    public static final BlockApiLookup<NerospaceEnergyStorage, Direction> ENERGY =
            BlockApiLookup.get(
                    Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "energy"),
                    NerospaceEnergyStorage.class, Direction.class);

    /** Mod-owned fluid lookup; mirrors the NeoForge fluid BlockCapability of the same id. */
    public static final BlockApiLookup<NerospaceFluidStorage, Direction> FLUID =
            BlockApiLookup.get(
                    Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "fluid"),
                    NerospaceFluidStorage.class, Direction.class);

    @Override
    public void onInitialize() {
        NerospaceCommon.LOGGER.info("[Nerospace] Fabric bootstrap");
        NerospaceCommon.init();

        ModItems.creativeTabItems().forEach((tab, items) ->
                CreativeModeTabEvents.modifyOutputEvent(tab)
                        .register(output -> items.forEach(output::accept)));

        addOverworldOre("nerosium_ore_placed");

        // Item-storage capability (Fabric Transfer API) — counterpart to NeoForge
        // Capabilities.Item.BLOCK; lets mod pipes move items in/out of the item store.
        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.ITEM_STORE.get());

        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.BATTERY.get());

        FLUID.registerForBlockEntity(
                (be, direction) -> be.getTank(),
                ModBlockEntities.FLUID_TANK.get());

        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.COMBUSTION_GENERATOR.get());
        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.COMBUSTION_GENERATOR.get());

        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.NEROSIUM_GRINDER.get());
        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.NEROSIUM_GRINDER.get());

        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.PASSIVE_GENERATOR.get());
        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.PASSIVE_GENERATOR.get());

        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.UNIVERSAL_PIPE.get());
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
