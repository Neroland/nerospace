package za.co.neroland.nerospace.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import za.co.neroland.nerospace.client.CombustionGeneratorScreen;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/** NeoForge client-only wiring (screen registration). Loaded only behind a Dist.CLIENT guard. */
public final class NeoForgeClientSetup {

    private NeoForgeClientSetup() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeClientSetup::onRegisterScreens);
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.COMBUSTION_GENERATOR.get(), CombustionGeneratorScreen::new);
    }
}
