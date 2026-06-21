package za.co.neroland.nerospace.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;
import za.co.neroland.nerospace.machine.CombustionGeneratorBlockEntity;
import za.co.neroland.nerospace.machine.FuelRefineryBlockEntity;
import za.co.neroland.nerospace.machine.FuelTankBlockEntity;
import za.co.neroland.nerospace.machine.NerosiumGrinderBlockEntity;
import za.co.neroland.nerospace.machine.quarry.QuarryControllerBlockEntity;
import za.co.neroland.nerospace.machine.quarry.QuarryLandmarkBlockEntity;
import za.co.neroland.nerospace.machine.OxygenGeneratorBlockEntity;
import za.co.neroland.nerospace.machine.PassiveGeneratorBlockEntity;
import za.co.neroland.nerospace.machine.HydrationModuleBlockEntity;
import za.co.neroland.nerospace.machine.SolarPanelBlockEntity;
import za.co.neroland.nerospace.machine.TerraformMonitorBlockEntity;
import za.co.neroland.nerospace.machine.TerraformerBlockEntity;
import za.co.neroland.nerospace.meteor.MeteorCoreBlockEntity;
import za.co.neroland.nerospace.pipe.UniversalPipeBlockEntity;
import za.co.neroland.nerospace.storage.CreativeBatteryBlockEntity;
import za.co.neroland.nerospace.storage.CreativeFluidTankBlockEntity;
import za.co.neroland.nerospace.storage.CreativeGasTankBlockEntity;
import za.co.neroland.nerospace.storage.CreativeItemStoreBlockEntity;
import za.co.neroland.nerospace.storage.GasTankBlockEntity;
import za.co.neroland.nerospace.storage.TrashCanBlockEntity;
import za.co.neroland.nerospace.storage.BatteryBlockEntity;
import za.co.neroland.nerospace.storage.FluidTankBlockEntity;
import za.co.neroland.nerospace.storage.ItemStoreBlockEntity;

/**
 * Block-entity types, shared by both loaders via {@link RegistrationProvider} over the vanilla
 * {@code BLOCK_ENTITY_TYPE} registry. Registration is loader-agnostic; only mod-pipe capability
 * exposure (next step) needs the platform seam.
 */
public final class ModBlockEntities {

    public static final RegistrationProvider<BlockEntityType<?>> BLOCK_ENTITIES =
            RegistrationProvider.get(Registries.BLOCK_ENTITY_TYPE, NerospaceCommon.MOD_ID);

    public static final RegistryEntry<BlockEntityType<ItemStoreBlockEntity>> ITEM_STORE =
            BLOCK_ENTITIES.register("item_store",
                    key -> new BlockEntityType<>(ItemStoreBlockEntity::new, java.util.Set.of(ModBlocks.ITEM_STORE.get())));

    public static final RegistryEntry<BlockEntityType<BatteryBlockEntity>> BATTERY =
            BLOCK_ENTITIES.register("battery",
                    key -> new BlockEntityType<>(BatteryBlockEntity::new, java.util.Set.of(ModBlocks.BATTERY.get())));

    public static final RegistryEntry<BlockEntityType<FluidTankBlockEntity>> FLUID_TANK =
            BLOCK_ENTITIES.register("fluid_tank",
                    key -> new BlockEntityType<>(FluidTankBlockEntity::new, java.util.Set.of(ModBlocks.FLUID_TANK.get())));

    public static final RegistryEntry<BlockEntityType<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR =
            BLOCK_ENTITIES.register("combustion_generator",
                    key -> new BlockEntityType<>(CombustionGeneratorBlockEntity::new, java.util.Set.of(ModBlocks.COMBUSTION_GENERATOR.get())));

    public static final RegistryEntry<BlockEntityType<NerosiumGrinderBlockEntity>> NEROSIUM_GRINDER =
            BLOCK_ENTITIES.register("nerosium_grinder",
                    key -> new BlockEntityType<>(NerosiumGrinderBlockEntity::new, java.util.Set.of(ModBlocks.NEROSIUM_GRINDER.get())));

    public static final RegistryEntry<BlockEntityType<PassiveGeneratorBlockEntity>> PASSIVE_GENERATOR =
            BLOCK_ENTITIES.register("passive_generator",
                    key -> new BlockEntityType<>(PassiveGeneratorBlockEntity::new, java.util.Set.of(ModBlocks.PASSIVE_GENERATOR.get())));

    public static final RegistryEntry<BlockEntityType<UniversalPipeBlockEntity>> UNIVERSAL_PIPE =
            BLOCK_ENTITIES.register("universal_pipe",
                    key -> new BlockEntityType<>(UniversalPipeBlockEntity::new, java.util.Set.of(ModBlocks.UNIVERSAL_PIPE.get())));

    public static final RegistryEntry<BlockEntityType<TrashCanBlockEntity>> TRASH_CAN =
            BLOCK_ENTITIES.register("trash_can",
                    key -> new BlockEntityType<>(TrashCanBlockEntity::new, java.util.Set.of(ModBlocks.TRASH_CAN.get())));

    public static final RegistryEntry<BlockEntityType<CreativeBatteryBlockEntity>> CREATIVE_BATTERY =
            BLOCK_ENTITIES.register("creative_battery",
                    key -> new BlockEntityType<>(CreativeBatteryBlockEntity::new, java.util.Set.of(ModBlocks.CREATIVE_BATTERY.get())));

    public static final RegistryEntry<BlockEntityType<GasTankBlockEntity>> GAS_TANK =
            BLOCK_ENTITIES.register("gas_tank",
                    key -> new BlockEntityType<>(GasTankBlockEntity::new, java.util.Set.of(ModBlocks.GAS_TANK.get())));

    public static final RegistryEntry<BlockEntityType<OxygenGeneratorBlockEntity>> OXYGEN_GENERATOR =
            BLOCK_ENTITIES.register("oxygen_generator",
                    key -> new BlockEntityType<>(OxygenGeneratorBlockEntity::new, java.util.Set.of(ModBlocks.OXYGEN_GENERATOR.get())));

    public static final RegistryEntry<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL =
            BLOCK_ENTITIES.register("solar_panel",
                    key -> new BlockEntityType<>(SolarPanelBlockEntity::new, java.util.Set.of(ModBlocks.SOLAR_PANEL.get())));

    public static final RegistryEntry<BlockEntityType<FuelTankBlockEntity>> FUEL_TANK =
            BLOCK_ENTITIES.register("fuel_tank",
                    key -> new BlockEntityType<>(FuelTankBlockEntity::new, java.util.Set.of(ModBlocks.FUEL_TANK.get())));

    public static final RegistryEntry<BlockEntityType<FuelRefineryBlockEntity>> FUEL_REFINERY =
            BLOCK_ENTITIES.register("fuel_refinery",
                    key -> new BlockEntityType<>(FuelRefineryBlockEntity::new, java.util.Set.of(ModBlocks.FUEL_REFINERY.get())));

    public static final RegistryEntry<BlockEntityType<CreativeFluidTankBlockEntity>> CREATIVE_FLUID_TANK =
            BLOCK_ENTITIES.register("creative_fluid_tank",
                    key -> new BlockEntityType<>(CreativeFluidTankBlockEntity::new, java.util.Set.of(ModBlocks.CREATIVE_FLUID_TANK.get())));

    public static final RegistryEntry<BlockEntityType<CreativeGasTankBlockEntity>> CREATIVE_GAS_TANK =
            BLOCK_ENTITIES.register("creative_gas_tank",
                    key -> new BlockEntityType<>(CreativeGasTankBlockEntity::new, java.util.Set.of(ModBlocks.CREATIVE_GAS_TANK.get())));

    public static final RegistryEntry<BlockEntityType<CreativeItemStoreBlockEntity>> CREATIVE_ITEM_STORE =
            BLOCK_ENTITIES.register("creative_item_store",
                    key -> new BlockEntityType<>(CreativeItemStoreBlockEntity::new, java.util.Set.of(ModBlocks.CREATIVE_ITEM_STORE.get())));

    public static final RegistryEntry<BlockEntityType<QuarryControllerBlockEntity>> QUARRY_CONTROLLER =
            BLOCK_ENTITIES.register("quarry_controller",
                    key -> new BlockEntityType<>(QuarryControllerBlockEntity::new, java.util.Set.of(ModBlocks.QUARRY_CONTROLLER.get())));

    public static final RegistryEntry<BlockEntityType<QuarryLandmarkBlockEntity>> QUARRY_LANDMARK =
            BLOCK_ENTITIES.register("quarry_landmark",
                    key -> new BlockEntityType<>(QuarryLandmarkBlockEntity::new, java.util.Set.of(ModBlocks.QUARRY_LANDMARK.get())));

    public static final RegistryEntry<BlockEntityType<MeteorCoreBlockEntity>> METEOR_CORE =
            BLOCK_ENTITIES.register("meteor_core",
                    key -> new BlockEntityType<>(MeteorCoreBlockEntity::new, java.util.Set.of(ModBlocks.METEOR_CORE.get())));

    public static final RegistryEntry<BlockEntityType<TerraformerBlockEntity>> TERRAFORMER =
            BLOCK_ENTITIES.register("terraformer",
                    key -> new BlockEntityType<>(TerraformerBlockEntity::new, java.util.Set.of(ModBlocks.TERRAFORMER.get())));

    public static final RegistryEntry<BlockEntityType<HydrationModuleBlockEntity>> HYDRATION_MODULE =
            BLOCK_ENTITIES.register("hydration_module",
                    key -> new BlockEntityType<>(HydrationModuleBlockEntity::new, java.util.Set.of(ModBlocks.HYDRATION_MODULE.get())));

    public static final RegistryEntry<BlockEntityType<TerraformMonitorBlockEntity>> TERRAFORM_MONITOR =
            BLOCK_ENTITIES.register("terraform_monitor",
                    key -> new BlockEntityType<>(TerraformMonitorBlockEntity::new, java.util.Set.of(ModBlocks.TERRAFORM_MONITOR.get())));

    private ModBlockEntities() {
    }

    public static void init() {
    }
}
