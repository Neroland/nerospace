package za.co.neroland.nerospace.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.client.CombustionGeneratorScreen;
import za.co.neroland.nerospace.client.NerosiumGrinderScreen;
import za.co.neroland.nerospace.client.PassiveGeneratorScreen;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/** Fabric client entry point — screen registration (vanilla {@code MenuScreens}). */
public final class NerospaceFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        NerospaceCommon.LOGGER.info("[Nerospace] Fabric client bootstrap");
        MenuScreens.register(ModMenuTypes.COMBUSTION_GENERATOR.get(), CombustionGeneratorScreen::new);
        MenuScreens.register(ModMenuTypes.NEROSIUM_GRINDER.get(), NerosiumGrinderScreen::new);
        MenuScreens.register(ModMenuTypes.PASSIVE_GENERATOR.get(), PassiveGeneratorScreen::new);
    }
}
