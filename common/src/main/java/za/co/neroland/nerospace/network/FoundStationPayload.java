package za.co.neroland.nerospace.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Client → server: the station name typed into the Station Charter naming screen. The server founds a new
 * station with that name (consuming one charter) — without teleporting the player, so the rocket remains
 * the way to actually travel there. Registered (with its server handler) in {@code ModNetwork.init()}.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> carries only the player-chosen station name — no identity.</p>
 */
public record FoundStationPayload(String name) implements CustomPacketPayload {

    public static final Type<FoundStationPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "found_station"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FoundStationPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> buf.writeUtf(payload.name, 64),
                    buf -> new FoundStationPayload(buf.readUtf(64)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
