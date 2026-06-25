package za.co.neroland.nerospace.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.network.ModNetwork;
import za.co.neroland.nerospace.platform.NetworkPlatform;

/**
 * Fabric side of the networking seam. {@link #registerCommon()} (mod init, both sides) registers every
 * payload <em>type</em> ({@code PayloadTypeRegistry.clientboundPlay()/serverboundPlay()}) and the
 * serverbound receivers; {@link #registerClient()} (client init) registers the clientbound receivers —
 * keeping {@code ClientPlayNetworking} off the dedicated server until then. Send methods implement
 * {@link NetworkPlatform}. Registered via {@code META-INF/services}.
 */
public final class FabricNetwork implements NetworkPlatform {

    /** Mod-init (both sides): payload types + serverbound receivers. */
    public static void registerCommon() {
        for (ModNetwork.Clientbound<? extends @NonNull CustomPacketPayload> cb : ModNetwork.clientbound()) {
            registerClientboundType(cb);
        }
        for (ModNetwork.Serverbound<? extends @NonNull CustomPacketPayload> sb : ModNetwork.serverbound()) {
            registerServerbound(sb);
        }
    }

    /** Client-init: clientbound receivers (client-only API). */
    public static void registerClient() {
        for (ModNetwork.Clientbound<? extends @NonNull CustomPacketPayload> cb : ModNetwork.clientbound()) {
            registerClientReceiver(cb);
        }
    }

    private static <T extends @NonNull CustomPacketPayload> void registerClientboundType(ModNetwork.Clientbound<T> cb) {
        PayloadTypeRegistry.clientboundPlay().register(cb.type(), cb.codec());
    }

    private static <T extends @NonNull CustomPacketPayload> void registerServerbound(ModNetwork.Serverbound<T> sb) {
        PayloadTypeRegistry.serverboundPlay().register(sb.type(), sb.codec());
        ServerPlayNetworking.registerGlobalReceiver(sb.type(), (payload, context) -> {
            ServerPlayer player = context.player();
            MinecraftServer server = player.level().getServer();
            if (server != null) {
                server.execute(() -> sb.handler().accept(payload, player));
            }
        });
    }

    private static <T extends @NonNull CustomPacketPayload> void registerClientReceiver(ModNetwork.Clientbound<T> cb) {
        ClientPlayNetworking.registerGlobalReceiver(cb.type(), (payload, context) ->
                context.client().execute(() -> cb.handler().accept(payload)));
    }

    @Override
    public void sendToPlayer(@NonNull ServerPlayer player, @NonNull CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public void sendToServer(@NonNull CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
