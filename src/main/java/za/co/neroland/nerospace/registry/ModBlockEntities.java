package za.co.neroland.nerospace.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.machine.CombustionGeneratorBlockEntity;
import za.co.neroland.nerospace.machine.FuelRefineryBlockEntity;
import za.co.neroland.nerospace.machine.FuelTankBlockEntity;
import za.co.neroland.nerospace.machine.HydrationModuleBlockEntity;
import za.co.neroland.nerospace.machine.TerraformMonitorBlockEntity;
import za.co.neroland.nerospace.machine.NerosiumGrinderBlockEntity;
import za.co.neroland.nerospace.machine.OxygenGeneratorBlockEntity;
import za.co.neroland.nerospace.machine.PassiveGeneratorBlockEntity;
import za.co.neroland.nerospace.machine.TerraformerBlockEntity;
import za.co.neroland.nerospace.pipe.UniversalPipeBlockEntity;
import za.co.neroland.nerospace.storage.BatteryBlockEntity;
import za.co.neroland.nerospace.storage.CreativeBatteryBlockEntity;
import za.co.neroland.nerospace.storage.CreativeFluidTankBlockEntity;
import za.co.neroland.nerospace.storage.CreativeGasTankBlockEntity;
import za.co.neroland.nerospace.storage.CreativeItemStoreBlockEntity;
import za.co.neroland.nerospace.storage.FluidTankBlockEntity;
import za.co.neroland.nerospace.storage.GasTankBlockEntity;
import za.co.neroland.nerospace.storage.ItemStoreBlockEntity;

/**
 * Block entity types (Phase 2).
 */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Nerospace.MODID);

    public static final Supplier<BlockEntityType<NerosiumGrinderBlockEntity>> NEROSIUM_GRINDER = BLOCK_ENTITY_TYPES.register(
            "nerosium_grinder",
            () -> new BlockEntityType<>(
                    NerosiumGrinderBlockEntity::new,
                    // Only allow OP players to load NBT data: false.
                    false,
                    ModBlocks.NEROSIUM_GRINDER.get()));

    public static final Supplier<BlockEntityType<FuelTankBlockEntity>> FUEL_TANK = BLOCK_ENTITY_TYPES.register(
            "fuel_tank",
            () -> new BlockEntityType<>(
                    FuelTankBlockEntity::new,
                    false,
                    ModBlocks.FUEL_TANK.get()));

    public static final Supplier<BlockEntityType<FuelRefineryBlockEntity>> FUEL_REFINERY = BLOCK_ENTITY_TYPES.register(
            "fuel_refinery",
            () -> new BlockEntityType<>(
                    FuelRefineryBlockEntity::new,
                    false,
                    ModBlocks.FUEL_REFINERY.get()));

    public static final Supplier<BlockEntityType<OxygenGeneratorBlockEntity>> OXYGEN_GENERATOR = BLOCK_ENTITY_TYPES.register(
            "oxygen_generator",
            () -> new BlockEntityType<>(
                    OxygenGeneratorBlockEntity::new,
                    false,
                    ModBlocks.OXYGEN_GENERATOR.get()));

    public static final Supplier<BlockEntityType<TerraformerBlockEntity>> TERRAFORMER = BLOCK_ENTITY_TYPES.register(
            "terraformer",
            () -> new BlockEntityType<>(
                    TerraformerBlockEntity::new,
                    false,
                    ModBlocks.TERRAFORMER.get()));

    // Hydration Module (DEEPER_TERRAFORM_DESIGN.md §3.1).
    public static final Supplier<BlockEntityType<HydrationModuleBlockEntity>> HYDRATION_MODULE =
            BLOCK_ENTITY_TYPES.register("hydration_module",
                    () -> new BlockEntityType<>(
                            HydrationModuleBlockEntity::new,
                            false,
                            ModBlocks.HYDRATION_MODULE.get()));

    // Terraform Monitor (DEEPER_TERRAFORM_DESIGN.md §6).
    public static final Supplier<BlockEntityType<TerraformMonitorBlockEntity>> TERRAFORM_MONITOR =
            BLOCK_ENTITY_TYPES.register("terraform_monitor",
                    () -> new BlockEntityType<>(
                            TerraformMonitorBlockEntity::new,
                            false,
                            ModBlocks.TERRAFORM_MONITOR.get()));

    // Quarry / Miner (MINER_DESIGN).
    public static final Supplier<BlockEntityType<za.co.neroland.nerospace.machine.quarry.QuarryControllerBlockEntity>> QUARRY_CONTROLLER =
            BLOCK_ENTITY_TYPES.register("quarry_controller",
                    () -> new BlockEntityType<>(
                            za.co.neroland.nerospace.machine.quarry.QuarryControllerBlockEntity::new,
                            false,
                            ModBlocks.QUARRY_CONTROLLER.get()));

    public static final Supplier<BlockEntityType<za.co.neroland.nerospace.machine.quarry.QuarryLandmarkBlockEntity>> QUARRY_LANDMARK =
            BLOCK_ENTITY_TYPES.register("quarry_landmark",
                    () -> new BlockEntityType<>(
                            za.co.neroland.nerospace.machine.quarry.QuarryLandmarkBlockEntity::new,
                            false,
                            ModBlocks.QUARRY_LANDMARK.get()));

    public static final Supplier<BlockEntityType<UniversalPipeBlockEntity>> UNIVERSAL_PIPE = BLOCK_ENTITY_TYPES.register(
            "universal_pipe",
            () -> new BlockEntityType<>(
                    UniversalPipeBlockEntity::new,
                    false,
                    ModBlocks.UNIVERSAL_PIPE.get()));

    public static final Supplier<BlockEntityType<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR =
            BLOCK_ENTITY_TYPES.register("combustion_generator",
                    () -> new BlockEntityType<>(
                            CombustionGeneratorBlockEntity::new, false, ModBlocks.COMBUSTION_GENERATOR.get()));

    public static final Supplier<BlockEntityType<PassiveGeneratorBlockEntity>> PASSIVE_GENERATOR =
            BLOCK_ENTITY_TYPES.register("passive_generator",
                    () -> new BlockEntityType<>(
                            PassiveGeneratorBlockEntity::new, false, ModBlocks.PASSIVE_GENERATOR.get()));

    public static final Supplier<BlockEntityType<za.co.neroland.nerospace.solar.SolarPanelBlockEntity>> SOLAR_PANEL =
            BLOCK_ENTITY_TYPES.register("solar_panel",
                    () -> new BlockEntityType<>(za.co.neroland.nerospace.solar.SolarPanelBlockEntity::new,
                            false, ModBlocks.SOLAR_PANEL_T1.get(),
                            ModBlocks.SOLAR_PANEL_T2.get(), ModBlocks.SOLAR_PANEL_T3.get()));

    // Storage endpoints + creative sources.
    public static final Supplier<BlockEntityType<BatteryBlockEntity>> BATTERY =
            BLOCK_ENTITY_TYPES.register("battery",
                    () -> new BlockEntityType<>(BatteryBlockEntity::new, false, ModBlocks.BATTERY.get()));

    public static final Supplier<BlockEntityType<CreativeBatteryBlockEntity>> CREATIVE_BATTERY =
            BLOCK_ENTITY_TYPES.register("creative_battery",
                    () -> new BlockEntityType<>(CreativeBatteryBlockEntity::new, false, ModBlocks.CREATIVE_BATTERY.get()));

    public static final Supplier<BlockEntityType<FluidTankBlockEntity>> FLUID_TANK =
            BLOCK_ENTITY_TYPES.register("fluid_tank",
                    () -> new BlockEntityType<>(FluidTankBlockEntity::new, false, ModBlocks.FLUID_TANK.get()));

    public static final Supplier<BlockEntityType<CreativeFluidTankBlockEntity>> CREATIVE_FLUID_TANK =
            BLOCK_ENTITY_TYPES.register("creative_fluid_tank",
                    () -> new BlockEntityType<>(CreativeFluidTankBlockEntity::new, false, ModBlocks.CREATIVE_FLUID_TANK.get()));

    public static final Supplier<BlockEntityType<GasTankBlockEntity>> GAS_TANK =
            BLOCK_ENTITY_TYPES.register("gas_tank",
                    () -> new BlockEntityType<>(GasTankBlockEntity::new, false, ModBlocks.GAS_TANK.get()));

    public static final Supplier<BlockEntityType<CreativeGasTankBlockEntity>> CREATIVE_GAS_TANK =
            BLOCK_ENTITY_TYPES.register("creative_gas_tank",
                    () -> new BlockEntityType<>(CreativeGasTankBlockEntity::new, false, ModBlocks.CREATIVE_GAS_TANK.get()));

    public static final Supplier<BlockEntityType<ItemStoreBlockEntity>> ITEM_STORE =
            BLOCK_ENTITY_TYPES.register("item_store",
                    () -> new BlockEntityType<>(ItemStoreBlockEntity::new, false, ModBlocks.ITEM_STORE.get()));

    public static final Supplier<BlockEntityType<CreativeItemStoreBlockEntity>> CREATIVE_ITEM_STORE =
            BLOCK_ENTITY_TYPES.register("creative_item_store",
                    () -> new BlockEntityType<>(CreativeItemStoreBlockEntity::new, false, ModBlocks.CREATIVE_ITEM_STORE.get()));

    public static final Supplier<BlockEntityType<za.co.neroland.nerospace.storage.TrashCanBlockEntity>> TRASH_CAN =
            BLOCK_ENTITY_TYPES.register("trash_can",
                    () -> new BlockEntityType<>(
                            za.co.neroland.nerospace.storage.TrashCanBlockEntity::new, false, ModBlocks.TRASH_CAN.get()));

    // Station Core (MULTI_STATION_DESIGN.md).
    public static final Supplier<BlockEntityType<za.co.neroland.nerospace.rocket.StationCoreBlockEntity>> STATION_CORE =
            BLOCK_ENTITY_TYPES.register("station_core",
                    () -> new BlockEntityType<>(
                            za.co.neroland.nerospace.rocket.StationCoreBlockEntity::new,
                            false,
                            ModBlocks.STATION_CORE.get()));

    // Star Guide pedestal (progression block, 1.0).
    public static final Supplier<BlockEntityType<za.co.neroland.nerospace.progression.StarGuideBlockEntity>> STAR_GUIDE =
            BLOCK_ENTITY_TYPES.register("star_guide",
                    () -> new BlockEntityType<>(
                            za.co.neroland.nerospace.progression.StarGuideBlockEntity::new,
                            false,
                            ModBlocks.STAR_GUIDE.get()));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
