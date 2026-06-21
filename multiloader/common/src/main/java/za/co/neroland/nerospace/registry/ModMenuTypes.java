package za.co.neroland.nerospace.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.menu.CombustionGeneratorMenu;
import za.co.neroland.nerospace.menu.NerosiumGrinderMenu;
import za.co.neroland.nerospace.menu.FuelRefineryMenu;
import za.co.neroland.nerospace.menu.FuelTankMenu;
import za.co.neroland.nerospace.menu.HydrationModuleMenu;
import za.co.neroland.nerospace.machine.quarry.QuarryMenu;
import za.co.neroland.nerospace.menu.PassiveGeneratorMenu;
import za.co.neroland.nerospace.menu.PipeConfigMenu;
import za.co.neroland.nerospace.menu.TerraformMonitorMenu;
import za.co.neroland.nerospace.menu.TerraformerMenu;
import za.co.neroland.nerospace.progression.StarGuideMenu;
import za.co.neroland.nerospace.rocket.RocketMenu;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;

/** Menu types, shared via {@link RegistrationProvider} over the vanilla MENU registry. */
public final class ModMenuTypes {

    public static final RegistrationProvider<MenuType<?>> MENUS =
            RegistrationProvider.get(Registries.MENU, NerospaceCommon.MOD_ID);

    public static final RegistryEntry<MenuType<CombustionGeneratorMenu>> COMBUSTION_GENERATOR =
            MENUS.register("combustion_generator",
                    key -> new MenuType<>(CombustionGeneratorMenu::new, FeatureFlags.VANILLA_SET));

    public static final RegistryEntry<MenuType<NerosiumGrinderMenu>> NEROSIUM_GRINDER =
            MENUS.register("nerosium_grinder",
                    key -> new MenuType<>(NerosiumGrinderMenu::new, FeatureFlags.VANILLA_SET));

    public static final RegistryEntry<MenuType<PipeConfigMenu>> PIPE_CONFIG =
            MENUS.register("pipe_config",
                    key -> new MenuType<>(PipeConfigMenu::new, FeatureFlags.VANILLA_SET));

    public static final RegistryEntry<MenuType<PassiveGeneratorMenu>> PASSIVE_GENERATOR =
            MENUS.register("passive_generator",
                    key -> new MenuType<>(PassiveGeneratorMenu::new, FeatureFlags.VANILLA_SET));

    public static final RegistryEntry<MenuType<RocketMenu>> ROCKET =
            MENUS.register("rocket",
                    key -> new MenuType<>(RocketMenu::new, FeatureFlags.VANILLA_SET));

    public static final RegistryEntry<MenuType<FuelTankMenu>> FUEL_TANK =
            MENUS.register("fuel_tank",
                    key -> new MenuType<>(FuelTankMenu::new, FeatureFlags.VANILLA_SET));

    public static final RegistryEntry<MenuType<FuelRefineryMenu>> FUEL_REFINERY =
            MENUS.register("fuel_refinery",
                    key -> new MenuType<>(FuelRefineryMenu::new, FeatureFlags.VANILLA_SET));

    public static final RegistryEntry<MenuType<QuarryMenu>> QUARRY_CONTROLLER =
            MENUS.register("quarry_controller",
                    key -> new MenuType<>(QuarryMenu::new, FeatureFlags.VANILLA_SET));

    public static final RegistryEntry<MenuType<TerraformerMenu>> TERRAFORMER =
            MENUS.register("terraformer",
                    key -> new MenuType<>(TerraformerMenu::new, FeatureFlags.VANILLA_SET));

    public static final RegistryEntry<MenuType<HydrationModuleMenu>> HYDRATION_MODULE =
            MENUS.register("hydration_module",
                    key -> new MenuType<>(HydrationModuleMenu::new, FeatureFlags.VANILLA_SET));

    public static final RegistryEntry<MenuType<TerraformMonitorMenu>> TERRAFORM_MONITOR =
            MENUS.register("terraform_monitor",
                    key -> new MenuType<>(TerraformMonitorMenu::new, FeatureFlags.VANILLA_SET));

    public static final RegistryEntry<MenuType<StarGuideMenu>> STAR_GUIDE =
            MENUS.register("star_guide",
                    key -> new MenuType<>(StarGuideMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenuTypes() {
    }

    public static void init() {
    }
}
