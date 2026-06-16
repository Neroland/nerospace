package za.co.neroland.nerospace.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.client.ClientMeteorTracker;
import za.co.neroland.nerospace.client.ClientOxygenField;

/**
 * Mod-bus network registration (terraform design §1.7). Registers the server → client oxygen-field
 * snapshot payload. The handler delegates to {@link ClientOxygenField}; that class is only loaded when
 * the handler runs (client only), so it is safe to reference here on a dedicated server.
 */
@EventBusSubscriber(modid = Nerospace.MODID)
public final class ModNetwork {

    private ModNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .optional()
                .playToClient(
                        OxygenFieldSyncPayload.TYPE,
                        OxygenFieldSyncPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> ClientOxygenField.accept(payload)))
                .playToClient(
                        MeteorSyncPayload.TYPE,
                        MeteorSyncPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> ClientMeteorTracker.accept(payload)));
    }
}
