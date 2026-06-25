package za.co.neroland.nerospace.neoforge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.network.ModNetwork;
import za.co.neroland.nerospace.platform.NetworkPlatform;

/**
 * NeoForge side of the networking seam: registers every {@link ModNetwork} payload during
 * {@code RegisterPayloadHandlersEvent} (clientbound handlers run on the client; serverbound handlers
 * receive the sending {@link ServerPlayer}) and implements the send methods. Server → client uses
 * {@code PacketDistributor.sendToPlayer}; client → server uses the client-only
 * {@code ClientPacketDistributor.sendToServer} (loaded lazily, only when actually sending from a client).
 * Registered via {@code META-INF/services}.
 */
public final class NeoForgeNetwork implements NetworkPlatform {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeNetwork::onRegister);
    }

    private static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        for (ModNetwork.Clientbound<?> cb : ModNetwork.clientbound()) {
            registerClientbound(registrar, cb);
        }
        for (ModNetwork.Serverbound<?> sb : ModNetwork.serverbound()) {
            registerServerbound(registrar, sb);
        }
    }

    private static <T extends @NonNull CustomPacketPayload> void registerClientbound(PayloadRegistrar registrar, ModNetwork.Clientbound<T> cb) {
        registrar.playToClient(cb.type(), cb.codec(),
                (payload, context) -> context.enqueueWork(() -> cb.handler().accept(payload)));
    }

    private static <T extends @NonNull CustomPacketPayload> void registerServerbound(PayloadRegistrar registrar, ModNetwork.Serverbound<T> sb) {
        registrar.playToServer(sb.type(), sb.codec(),
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        sb.handler().accept(payload, serverPlayer);
                    }
                }));
    }

    @Override
    public void sendToPlayer(@NonNull ServerPlayer player, @NonNull CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public void sendToServer(@NonNull CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }
}
