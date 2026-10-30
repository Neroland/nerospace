package za.co.neroland.nerospace.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Server → client planet clock for the player's current level. Render-only: the client recomputes the phase
 * from its own synced game time, so this is sent on join, dimension change and ops changes — never per tick.
 * {@code dayLength == 0} means a fixed clock at {@code pinnedPhase}; {@code mirrorVanilla} means the level's own
 * day time is the clock (Greenxertz). {@code planet == false} (see {@link #none}) clears the client clock.
 * {@code bandWidth} is Nyxara's twilight band width as bound for this world ({@code 0} elsewhere), so the sky and the
 * ambience line up with the terrain even after the config changed. Carries no player data.
 */
public record PlanetClockPayload(Identifier dimension, long dayLength, long epochTicks, long offsetTicks,
        float pinnedPhase, boolean mirrorVanilla, boolean frozen, long frozenAt, long dayNumber, int bandWidth)
        implements CustomPacketPayload {

    public static final Type<PlanetClockPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "planet_clock"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlanetClockPayload> STREAM_CODEC =
            StreamCodec.of(PlanetClockPayload::write, PlanetClockPayload::read);

    /** "Not on a planet": the client drops its clock. */
    public static PlanetClockPayload none(Identifier dimension) {
        return new PlanetClockPayload(dimension, -1L, 0L, 0L, 0.25F, false, false, 0L, 0L, 0);
    }

    public boolean isPlanet() {
        return this.dayLength >= 0L;
    }

    private static void write(RegistryFriendlyByteBuf buf, PlanetClockPayload p) {
        Identifier.STREAM_CODEC.encode(buf, p.dimension);
        buf.writeVarLong(p.dayLength + 1L);
        buf.writeLong(p.epochTicks);
        buf.writeLong(p.offsetTicks);
        buf.writeFloat(p.pinnedPhase);
        buf.writeBoolean(p.mirrorVanilla);
        buf.writeBoolean(p.frozen);
        buf.writeLong(p.frozenAt);
        ByteBufCodecs.VAR_LONG.encode(buf, p.dayNumber);
        ByteBufCodecs.VAR_INT.encode(buf, Math.max(0, p.bandWidth));
    }

    private static PlanetClockPayload read(RegistryFriendlyByteBuf buf) {
        Identifier dim = Identifier.STREAM_CODEC.decode(buf);
        long dayLength = buf.readVarLong() - 1L;
        long epoch = buf.readLong();
        long offset = buf.readLong();
        float pinned = buf.readFloat();
        boolean mirror = buf.readBoolean();
        boolean frozen = buf.readBoolean();
        long frozenAt = buf.readLong();
        long day = ByteBufCodecs.VAR_LONG.decode(buf);
        int band = ByteBufCodecs.VAR_INT.decode(buf);
        return new PlanetClockPayload(dim, dayLength, epoch, offset, pinned, mirror, frozen, frozenAt, day, band);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
