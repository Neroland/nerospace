package za.co.neroland.nerospace.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerolandcore.registry.BlockEntityTypeCompat;

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
import za.co.neroland.nerospace.progression.StarGuideBlockEntity;
import za.co.neroland.nerospace.rocket.LaunchControllerBlockEntity;
import za.co.neroland.nerospace.rocket.LaunchGantryBlockEntity;
import za.co.neroland.nerospace.rocket.ReturnSiteBlockEntity;
import za.co.neroland.nerospace.rocket.StationCoreBlockEntity;
import za.co.neroland.nerospace.village.VillageCoreBlockEntity;

/**
 * Block-entity types, shared by both loaders via {@link RegistrationProvider} over the vanilla
 * {@code BLOCK_ENTITY_TYPE} registry. Registration is loader-agnostic; only mod-pipe capability
 * exposure (next step) needs the platform seam.
 */
public final class ModBlockEntities {

    public static final RegistrationProvider<BlockEntityType<?>> BLOCK_ENTITIES =
            RegistrationProvider.get(Registries.BLOCK_ENTITY_TYPE, NerospaceCommon.MOD_ID);

    public static final RegistryEntry<BlockEntityType<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR =
            BLOCK_ENTITIES.register("combustion_generator",
                    key -> BlockEntityTypeCompat.create(CombustionGeneratorBlockEntity::new, ModBlocks.COMBUSTION_GENERATOR.get()));

    public static final RegistryEntry<BlockEntityType<NerosiumGrinderBlockEntity>> NEROSIUM_GRINDER =
            BLOCK_ENTITIES.register("nerosium_grinder",
                    key -> BlockEntityTypeCompat.create(NerosiumGrinderBlockEntity::new, ModBlocks.NEROSIUM_GRINDER.get()));

    public static final RegistryEntry<BlockEntityType<PassiveGeneratorBlockEntity>> PASSIVE_GENERATOR =
            BLOCK_ENTITIES.register("passive_generator",
                    key -> BlockEntityTypeCompat.create(PassiveGeneratorBlockEntity::new, ModBlocks.PASSIVE_GENERATOR.get()));

    public static final RegistryEntry<BlockEntityType<UniversalPipeBlockEntity>> UNIVERSAL_PIPE =
            BLOCK_ENTITIES.register("universal_pipe",
                    key -> BlockEntityTypeCompat.create(UniversalPipeBlockEntity::new, ModBlocks.UNIVERSAL_PIPE.get()));

    public static final RegistryEntry<BlockEntityType<OxygenGeneratorBlockEntity>> OXYGEN_GENERATOR =
            BLOCK_ENTITIES.register("oxygen_generator",
                    key -> BlockEntityTypeCompat.create(OxygenGeneratorBlockEntity::new, ModBlocks.OXYGEN_GENERATOR.get()));

    public static final RegistryEntry<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL =
            BLOCK_ENTITIES.register("solar_panel",
                    key -> BlockEntityTypeCompat.create(SolarPanelBlockEntity::new, 
                            ModBlocks.SOLAR_PANEL.get(), ModBlocks.SOLAR_PANEL_T2.get(), ModBlocks.SOLAR_PANEL_T3.get()));

    public static final RegistryEntry<BlockEntityType<FuelTankBlockEntity>> FUEL_TANK =
            BLOCK_ENTITIES.register("fuel_tank",
                    key -> BlockEntityTypeCompat.create(FuelTankBlockEntity::new, ModBlocks.FUEL_TANK.get()));

    public static final RegistryEntry<BlockEntityType<za.co.neroland.nerospace.route.CargoPadBlockEntity>> CARGO_PAD =
            BLOCK_ENTITIES.register("cargo_pad",
                    key -> BlockEntityTypeCompat.create(za.co.neroland.nerospace.route.CargoPadBlockEntity::new,
                            ModBlocks.CARGO_PAD.get()));

    public static final RegistryEntry<BlockEntityType<FuelRefineryBlockEntity>> FUEL_REFINERY =
            BLOCK_ENTITIES.register("fuel_refinery",
                    key -> BlockEntityTypeCompat.create(FuelRefineryBlockEntity::new, ModBlocks.FUEL_REFINERY.get()));

    public static final RegistryEntry<BlockEntityType<QuarryControllerBlockEntity>> QUARRY_CONTROLLER =
            BLOCK_ENTITIES.register("quarry_controller",
                    key -> BlockEntityTypeCompat.create(QuarryControllerBlockEntity::new, ModBlocks.QUARRY_CONTROLLER.get()));

    public static final RegistryEntry<BlockEntityType<QuarryLandmarkBlockEntity>> QUARRY_LANDMARK =
            BLOCK_ENTITIES.register("quarry_landmark",
                    key -> BlockEntityTypeCompat.create(QuarryLandmarkBlockEntity::new, ModBlocks.QUARRY_LANDMARK.get()));

    public static final RegistryEntry<BlockEntityType<MeteorCoreBlockEntity>> METEOR_CORE =
            BLOCK_ENTITIES.register("meteor_core",
                    key -> BlockEntityTypeCompat.create(MeteorCoreBlockEntity::new, ModBlocks.METEOR_CORE.get()));

    public static final RegistryEntry<BlockEntityType<TerraformerBlockEntity>> TERRAFORMER =
            BLOCK_ENTITIES.register("terraformer",
                    key -> BlockEntityTypeCompat.create(TerraformerBlockEntity::new, ModBlocks.TERRAFORMER.get()));

    public static final RegistryEntry<BlockEntityType<HydrationModuleBlockEntity>> HYDRATION_MODULE =
            BLOCK_ENTITIES.register("hydration_module",
                    key -> BlockEntityTypeCompat.create(HydrationModuleBlockEntity::new, ModBlocks.HYDRATION_MODULE.get()));

    public static final RegistryEntry<BlockEntityType<TerraformMonitorBlockEntity>> TERRAFORM_MONITOR =
            BLOCK_ENTITIES.register("terraform_monitor",
                    key -> BlockEntityTypeCompat.create(TerraformMonitorBlockEntity::new, ModBlocks.TERRAFORM_MONITOR.get()));

    public static final RegistryEntry<BlockEntityType<StarGuideBlockEntity>> STAR_GUIDE =
            BLOCK_ENTITIES.register("star_guide",
                    key -> BlockEntityTypeCompat.create(StarGuideBlockEntity::new, ModBlocks.STAR_GUIDE.get()));

    public static final RegistryEntry<BlockEntityType<StationCoreBlockEntity>> STATION_CORE =
            BLOCK_ENTITIES.register("station_core",
                    key -> BlockEntityTypeCompat.create(StationCoreBlockEntity::new, ModBlocks.STATION_CORE.get()));

    public static final RegistryEntry<BlockEntityType<LaunchGantryBlockEntity>> LAUNCH_GANTRY =
            BLOCK_ENTITIES.register("launch_gantry",
                    key -> BlockEntityTypeCompat.create(LaunchGantryBlockEntity::new, ModBlocks.LAUNCH_GANTRY.get()));

    public static final RegistryEntry<BlockEntityType<LaunchControllerBlockEntity>> LAUNCH_CONTROLLER =
            BLOCK_ENTITIES.register("launch_controller",
                    key -> BlockEntityTypeCompat.create(LaunchControllerBlockEntity::new, ModBlocks.LAUNCH_CONTROLLER.get()));

    public static final RegistryEntry<BlockEntityType<ReturnSiteBlockEntity>> RETURN_SITE =
            BLOCK_ENTITIES.register("return_site",
                    key -> BlockEntityTypeCompat.create(ReturnSiteBlockEntity::new, 
                            ModBlocks.LANDING_POD.get(), ModBlocks.DOCKING_PORT.get()));

    public static final RegistryEntry<BlockEntityType<VillageCoreBlockEntity>> VILLAGE_CORE =
            BLOCK_ENTITIES.register("village_core",
                    key -> BlockEntityTypeCompat.create(VillageCoreBlockEntity::new, ModBlocks.VILLAGE_CORE.get()));

    private ModBlockEntities() {
    }

    public static void init() {
    }
}
