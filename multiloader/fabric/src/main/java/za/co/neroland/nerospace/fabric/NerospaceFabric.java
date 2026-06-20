package za.co.neroland.nerospace.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
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
import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModEntityAttributes;
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

    /** Mod-owned gas lookup; mirrors the NeoForge gas BlockCapability of the same id. */
    public static final BlockApiLookup<NerospaceGasStorage, Direction> GAS =
            BlockApiLookup.get(
                    Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "gas"),
                    NerospaceGasStorage.class, Direction.class);

    @Override
    public void onInitialize() {
        NerospaceCommon.LOGGER.info("[Nerospace] Fabric bootstrap");
        NerospaceCommon.init();

        ModItems.creativeTabItems().forEach((tab, items) ->
                CreativeModeTabEvents.modifyOutputEvent(tab)
                        .register(output -> items.forEach(output::accept)));

        addOverworldOre("nerosium_ore_placed");

        // Default attributes for the ported mobs (counterpart to NeoForge's EntityAttributeCreationEvent).
        ModEntityAttributes.forEach(FabricDefaultAttributeRegistry::register);

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
        GAS.registerForBlockEntity(
                (be, direction) -> be.getGas(),
                ModBlockEntities.UNIVERSAL_PIPE.get());
        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.UNIVERSAL_PIPE.get());

        GAS.registerForBlockEntity(
                (be, direction) -> be.getTank(),
                ModBlockEntities.GAS_TANK.get());

        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.OXYGEN_GENERATOR.get());
        GAS.registerForBlockEntity(
                (be, direction) -> be.getGas(),
                ModBlockEntities.OXYGEN_GENERATOR.get());

        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.SOLAR_PANEL.get());

        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.TRASH_CAN.get());
        FLUID.registerForBlockEntity(
                (be, direction) -> be.getFluid(),
                ModBlockEntities.TRASH_CAN.get());

        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.CREATIVE_BATTERY.get());

        // Fuel Tank: fluid out (pipes), canister in (hoppers/pipes).
        FLUID.registerForBlockEntity(
                (be, direction) -> be.getTank(),
                ModBlockEntities.FUEL_TANK.get());
        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.FUEL_TANK.get());

        // Fuel Refinery: grid power in, refined fuel out, coal + blaze powder in.
        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.FUEL_REFINERY.get());
        FLUID.registerForBlockEntity(
                (be, direction) -> be.getTank(),
                ModBlockEntities.FUEL_REFINERY.get());
        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.FUEL_REFINERY.get());

        // Quarry controller: grid power in, mined output + sucked fluid out, frame casings in.
        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.QUARRY_CONTROLLER.get());
        FLUID.registerForBlockEntity(
                (be, direction) -> be.getTank(),
                ModBlockEntities.QUARRY_CONTROLLER.get());
        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.QUARRY_CONTROLLER.get());
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
