package za.co.neroland.nerospace.neoforge;

//? if >=1.20.5 {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
//? if >=1.21.6 {
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
//?}
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

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
        // optional() only tolerates a peer WITHOUT Nerospace; both sides with it must announce the same version,
        // so a mismatched client/server is refused at login instead of failing to decode mid-game (MC-NEROSPACE-13).
        PayloadRegistrar registrar = event.registrar(Integer.toString(ModNetwork.PROTOCOL_VERSION)).optional();
        for (ModNetwork.Clientbound<?> cb : ModNetwork.clientbound()) {
            registerClientbound(registrar, cb);
        }
        for (ModNetwork.Serverbound<?> sb : ModNetwork.serverbound()) {
            registerServerbound(registrar, sb);
        }
    }

    private static <T extends CustomPacketPayload> void registerClientbound(PayloadRegistrar registrar, ModNetwork.Clientbound<T> cb) {
        registrar.playToClient(cb.type(), cb.codec(),
                (payload, context) -> context.enqueueWork(() -> cb.handler().accept(payload)));
    }

    private static <T extends CustomPacketPayload> void registerServerbound(PayloadRegistrar registrar, ModNetwork.Serverbound<T> sb) {
        registrar.playToServer(sb.type(), sb.codec(),
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        sb.handler().accept(payload, serverPlayer);
                    }
                }));
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        //? if >=1.21.6 {
        ClientPacketDistributor.sendToServer(payload);
        //?} else {
        /*net.neoforged.neoforge.network.PacketDistributor.sendToServer(payload);
        *///?}
    }
}
//?} else if >=1.20.4 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.network.ModNetwork;
import za.co.neroland.nerolandcore.network.legacy.LegacyPayload;
import za.co.neroland.nerolandcore.network.legacy.LegacyStreamCodec;
import za.co.neroland.nerospace.platform.NetworkPlatform;

public final class NeoForgeNetwork implements NetworkPlatform {
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeNetwork::onRegister);
    }

    private static void onRegister(RegisterPayloadHandlerEvent event) {
        IPayloadRegistrar registrar = event.registrar(NerospaceCommon.MOD_ID).versioned(Integer.toString(ModNetwork.PROTOCOL_VERSION)).optional();
        ModNetwork.clientbound().forEach(packet -> registerClientbound(registrar, packet));
        ModNetwork.serverbound().forEach(packet -> registerServerbound(registrar, packet));
    }

    private static <T extends LegacyPayload> void registerClientbound(IPayloadRegistrar registrar,
            ModNetwork.Clientbound<T> packet) {
        registrar.play(packet.type().id(),
                buffer -> new WirePayload<>(packet.type(), packet.codec(), packet.codec().decode(buffer)),
                builder -> {
                    builder.client((payload, context) ->
                            context.workHandler().execute(() -> packet.handler().accept(payload.message())));
                });
    }

    private static <T extends LegacyPayload> void registerServerbound(IPayloadRegistrar registrar,
            ModNetwork.Serverbound<T> packet) {
        registrar.play(packet.type().id(),
                buffer -> new WirePayload<>(packet.type(), packet.codec(), packet.codec().decode(buffer)),
                builder -> {
                    builder.server((payload, context) -> context.workHandler().execute(() ->
                            context.player().ifPresent(player -> {
                                if (player instanceof ServerPlayer serverPlayer) {
                                    packet.handler().accept(payload.message(), serverPlayer);
                                }
                            })));
                });
    }

    @Override
    public void sendToPlayer(ServerPlayer player, LegacyPayload payload) {
        if (!NetworkRegistry.getInstance().isConnected(player.connection, payload.type().id())) {
            return;
        }
        for (ModNetwork.Clientbound<?> packet : ModNetwork.clientbound()) {
            if (packet.type().equals(payload.type())) {
                PacketDistributor.PLAYER.with(player).send(wrapClientbound(packet, payload));
                return;
            }
        }
        throw new IllegalArgumentException("Unregistered clientbound payload " + payload.type().id());
    }

    @Override
    public void sendToServer(LegacyPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null || !NetworkRegistry.getInstance().isConnected(connection, payload.type().id())) {
            return;
        }
        for (ModNetwork.Serverbound<?> packet : ModNetwork.serverbound()) {
            if (packet.type().equals(payload.type())) {
                PacketDistributor.SERVER.noArg().send(wrapServerbound(packet, payload));
                return;
            }
        }
        throw new IllegalArgumentException("Unregistered serverbound payload " + payload.type().id());
    }

    private static <T extends LegacyPayload> WirePayload<T> wrapClientbound(ModNetwork.Clientbound<T> packet,
            LegacyPayload payload) {
        return new WirePayload<>(packet.type(), packet.codec(), packet.type().payloadClass().cast(payload));
    }

    private static <T extends LegacyPayload> WirePayload<T> wrapServerbound(ModNetwork.Serverbound<T> packet,
            LegacyPayload payload) {
        return new WirePayload<>(packet.type(), packet.codec(), packet.type().payloadClass().cast(payload));
    }

    private record WirePayload<T extends LegacyPayload>(LegacyPayload.Type<T> type,
            LegacyStreamCodec<? super FriendlyByteBuf, T> codec, T message)
            implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        @Override
        public ResourceLocation id() {
            return type.id();
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            codec.encode(buffer, message);
        }
    }
}
*///?} else {
/*import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.NetworkRegistry;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.PlayNetworkDirection;
import net.neoforged.neoforge.network.simple.SimpleChannel;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.network.ModNetwork;
import za.co.neroland.nerolandcore.network.legacy.LegacyPayload;
import za.co.neroland.nerospace.platform.NetworkPlatform;

// NeoForge 20.2 / 20.3 still carry Forge's SimpleChannel: one indexed channel, missing on the other side allowed
// (a vanilla peer), but a peer announcing a different protocol version is refused at login.
public final class NeoForgeNetwork implements NetworkPlatform {
    private static SimpleChannel channel;

    public static void register(IEventBus modEventBus) {
        String protocol = Integer.toString(ModNetwork.PROTOCOL_VERSION);
        var accepts = NetworkRegistry.acceptMissingOr(protocol);
        channel = NetworkRegistry.newSimpleChannel(new ResourceLocation(NerospaceCommon.MOD_ID, "main"),
                () -> protocol, accepts, accepts);
        int index = 0;
        for (ModNetwork.Clientbound<?> packet : ModNetwork.clientbound()) {
            registerClientbound(packet, index++);
        }
        for (ModNetwork.Serverbound<?> packet : ModNetwork.serverbound()) {
            registerServerbound(packet, index++);
        }
    }

    private static <T extends LegacyPayload> void registerClientbound(ModNetwork.Clientbound<T> packet, int index) {
        channel.messageBuilder(packet.type().payloadClass(), index, PlayNetworkDirection.PLAY_TO_CLIENT)
                .encoder((payload, buffer) -> packet.codec().encode(buffer, payload))
                .decoder(buffer -> packet.codec().decode(buffer))
                .consumerMainThread((payload, context) -> packet.handler().accept(payload))
                .add();
    }

    private static <T extends LegacyPayload> void registerServerbound(ModNetwork.Serverbound<T> packet, int index) {
        channel.messageBuilder(packet.type().payloadClass(), index, PlayNetworkDirection.PLAY_TO_SERVER)
                .encoder((payload, buffer) -> packet.codec().encode(buffer, payload))
                .decoder(buffer -> packet.codec().decode(buffer))
                .consumerMainThread((payload, context) -> {
                    ServerPlayer player = context.getSender();
                    if (player != null) {
                        packet.handler().accept(payload, player);
                    }
                })
                .add();
    }

    @Override
    public void sendToPlayer(ServerPlayer player, LegacyPayload payload) {
        if (channel != null && channel.isRemotePresent(player.connection.connection)) {
            channel.send(PacketDistributor.PLAYER.with(() -> player), payload);
        }
    }

    @Override
    public void sendToServer(LegacyPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (channel != null && connection != null && channel.isRemotePresent(connection.getConnection())) {
            channel.sendToServer(payload);
        }
    }
}
*///?}
