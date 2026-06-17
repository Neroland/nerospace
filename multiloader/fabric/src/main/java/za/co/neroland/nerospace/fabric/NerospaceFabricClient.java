package za.co.neroland.nerospace.fabric;

import net.fabricmc.api.ClientModInitializer;
import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Fabric client entry point. The equivalent of the root project's
 * {@code NerospaceClient} client-only registrations.
 *
 * <p>Migration target for: entity renderers ({@code EntityRendererRegistry}),
 * screens ({@code HandledScreens}), HUD ({@code HudLayerRegistrationCallback}),
 * and ModMenu config screen integration.
 */
public final class NerospaceFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        NerospaceCommon.LOGGER.info("[Nerospace] Fabric client bootstrap");
        // TODO (migration): client-only registrations.
    }
}
