package za.co.neroland.nerospace.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.machine.CombustionGeneratorMenu;
import za.co.neroland.nerospace.machine.FuelRefineryMenu;
import za.co.neroland.nerospace.machine.FuelTankMenu;
import za.co.neroland.nerospace.machine.HydrationModuleMenu;
import za.co.neroland.nerospace.machine.TerraformMonitorMenu;
import za.co.neroland.nerospace.machine.NerosiumGrinderMenu;
import za.co.neroland.nerospace.machine.OxygenGeneratorMenu;
import za.co.neroland.nerospace.machine.PassiveGeneratorMenu;
import za.co.neroland.nerospace.machine.TerraformerMenu;
import za.co.neroland.nerospace.rocket.RocketMenu;

/**
 * Menu types (Phase 2). The {@code MenuSupplier} is the client-side constructor.
 */
public final class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, Nerospace.MODID);

    public static final Supplier<MenuType<NerosiumGrinderMenu>> NEROSIUM_GRINDER = MENU_TYPES.register(
            "nerosium_grinder",
            () -> new MenuType<>(NerosiumGrinderMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<OxygenGeneratorMenu>> OXYGEN_GENERATOR = MENU_TYPES.register(
            "oxygen_generator",
            () -> new MenuType<>(OxygenGeneratorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<FuelTankMenu>> FUEL_TANK = MENU_TYPES.register(
            "fuel_tank",
            () -> new MenuType<>(FuelTankMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<FuelRefineryMenu>> FUEL_REFINERY = MENU_TYPES.register(
            "fuel_refinery",
            () -> new MenuType<>(FuelRefineryMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<TerraformerMenu>> TERRAFORMER = MENU_TYPES.register(
            "terraformer",
            () -> new MenuType<>(TerraformerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    // Hydration Module (DEEPER_TERRAFORM_DESIGN.md §3.1).
    public static final Supplier<MenuType<HydrationModuleMenu>> HYDRATION_MODULE = MENU_TYPES.register(
            "hydration_module",
            () -> new MenuType<>(HydrationModuleMenu::new, FeatureFlags.DEFAULT_FLAGS));

    // Terraform Monitor (DEEPER_TERRAFORM_DESIGN.md §6) — pure readout, no slots.
    public static final Supplier<MenuType<TerraformMonitorMenu>> TERRAFORM_MONITOR = MENU_TYPES.register(
            "terraform_monitor",
            () -> new MenuType<>(TerraformMonitorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<CombustionGeneratorMenu>> COMBUSTION_GENERATOR = MENU_TYPES.register(
            "combustion_generator",
            () -> new MenuType<>(CombustionGeneratorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<PassiveGeneratorMenu>> PASSIVE_GENERATOR = MENU_TYPES.register(
            "passive_generator",
            () -> new MenuType<>(PassiveGeneratorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    /**
     * Rocket menu. Uses {@link IMenuTypeExtension#create} so the entity id can be written to the
     * opening buffer and read back by the client constructor to resolve the {@link RocketMenu}'s
     * rocket.
     */
    public static final Supplier<MenuType<RocketMenu>> ROCKET = MENU_TYPES.register(
            "rocket",
            () -> IMenuTypeExtension.create(RocketMenu::new));

    /** Star Guide progression tree (no slots; per-chapter bitmask data). */
    public static final Supplier<MenuType<za.co.neroland.nerospace.progression.StarGuideMenu>> STAR_GUIDE =
            MENU_TYPES.register("star_guide",
                    () -> new MenuType<>(za.co.neroland.nerospace.progression.StarGuideMenu::new,
                            FeatureFlags.DEFAULT_FLAGS));

    private ModMenuTypes() {
    }

    public static void register(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}
