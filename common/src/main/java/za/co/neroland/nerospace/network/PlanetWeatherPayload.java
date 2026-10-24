package za.co.neroland.nerospace.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Server → client weather for the player's current level: event + state ordinals, intensity and the event's
 * seed (for deterministic cosmetic direction). Render-only and transient (wire ordinals, never persisted). Sent
 * on change, on join / dimension change, and every 100 ticks while an event is active. Carries no player data.
 */
public record PlanetWeatherPayload(Identifier dimension, byte event, byte state, float intensity, long seed)
        implements CustomPacketPayload {

    public static final Type<PlanetWeatherPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "planet_weather"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlanetWeatherPayload> STREAM_CODEC =
            StreamCodec.of(PlanetWeatherPayload::write, PlanetWeatherPayload::read);

    public static PlanetWeatherPayload clear(Identifier dimension) {
        return new PlanetWeatherPayload(dimension, (byte) 0, (byte) 0, 0.0F, 0L);
    }

    private static void write(RegistryFriendlyByteBuf buf, PlanetWeatherPayload p) {
        Identifier.STREAM_CODEC.encode(buf, p.dimension);
        buf.writeByte(p.event);
        buf.writeByte(p.state);
        buf.writeFloat(p.intensity);
        buf.writeLong(p.seed);
    }

    private static PlanetWeatherPayload read(RegistryFriendlyByteBuf buf) {
        return new PlanetWeatherPayload(Identifier.STREAM_CODEC.decode(buf), buf.readByte(), buf.readByte(),
                buf.readFloat(), buf.readLong());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
