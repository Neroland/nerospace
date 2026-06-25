package za.co.neroland.nerospace.network;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Server → client oxygen-field snapshot (terraform design §1.7). Carries the range-limited set of
 * oxygenated cells around a player as packed world positions + concentrations. The client keeps a
 * small local copy ({@link za.co.neroland.nerospace.client.ClientOxygenField}) for the particle /
 * boundary visual layers.
 */
public record OxygenFieldSyncPayload(long[] positions, byte[] values) implements CustomPacketPayload {

    public static final CustomPacketPayload.@NonNull Type<@NonNull OxygenFieldSyncPayload> TYPE =
            new Type<>(NerospaceCommon.id("oxygen_field_sync"));

    public static final @NonNull StreamCodec<RegistryFriendlyByteBuf, @NonNull OxygenFieldSyncPayload> STREAM_CODEC =
            StreamCodec.of(OxygenFieldSyncPayload::write, OxygenFieldSyncPayload::read);

    public static OxygenFieldSyncPayload of(Long2ByteMap field) {
        long[] pos = new long[field.size()];
        byte[] val = new byte[field.size()];
        int i = 0;
        for (Long2ByteMap.Entry e : field.long2ByteEntrySet()) {
            pos[i] = e.getLongKey();
            val[i] = e.getByteValue();
            i++;
        }
        return new OxygenFieldSyncPayload(pos, val);
    }

    public Long2ByteMap toMap() {
        Long2ByteOpenHashMap map = new Long2ByteOpenHashMap(this.positions.length);
        map.defaultReturnValue((byte) 0);
        for (int i = 0; i < this.positions.length; i++) {
            map.put(this.positions[i], this.values[i]);
        }
        return map;
    }

    private static void write(RegistryFriendlyByteBuf buf, OxygenFieldSyncPayload payload) {
        buf.writeVarInt(payload.positions.length);
        for (int i = 0; i < payload.positions.length; i++) {
            buf.writeLong(payload.positions[i]);
            buf.writeByte(payload.values[i]);
        }
    }

    private static @NonNull OxygenFieldSyncPayload read(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        long[] pos = new long[n];
        byte[] val = new byte[n];
        for (int i = 0; i < n; i++) {
            pos[i] = buf.readLong();
            val[i] = buf.readByte();
        }
        return new OxygenFieldSyncPayload(pos, val);
    }

    @Override
    public CustomPacketPayload.@NonNull Type<? extends @NonNull CustomPacketPayload> type() {
        return TYPE;
    }
}
