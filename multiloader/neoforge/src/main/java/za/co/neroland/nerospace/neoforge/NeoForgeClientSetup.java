package za.co.neroland.nerospace.neoforge;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.client.CombustionGeneratorScreen;
import za.co.neroland.nerospace.client.NerosiumGrinderScreen;
import za.co.neroland.nerospace.client.PassiveGeneratorScreen;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/** NeoForge client-only wiring (screen + fluid-model registration). Loaded only behind Dist.CLIENT. */
public final class NeoForgeClientSetup {

    private NeoForgeClientSetup() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeClientSetup::onRegisterScreens);
        modEventBus.addListener(NeoForgeClientSetup::onRegisterFluidModels);
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.COMBUSTION_GENERATOR.get(), CombustionGeneratorScreen::new);
        event.register(ModMenuTypes.NEROSIUM_GRINDER.get(), NerosiumGrinderScreen::new);
        event.register(ModMenuTypes.PASSIVE_GENERATOR.get(), PassiveGeneratorScreen::new);
    }

    /** Rocket fuel renders as itself (amber still/flow) instead of the default missing art. */
    private static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        Material still = new Material(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "block/rocket_fuel_still"));
        Material flow = new Material(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "block/rocket_fuel_flow"));
        event.register(
                new FluidModel.Unbaked(still, flow, still, FluidTintSources.constant(0xFFFFFFFF)),
                ModFluids.ROCKET_FUEL, ModFluids.ROCKET_FUEL_FLOWING);
    }
}
