package za.co.neroland.nerospace.network;

import java.util.UUID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Client → server: add the online player {@code uuid} as a member of station {@code slot},
 * picked from the console's online-player list. No free text, no name: the server re-checks {@code canManage}, that
 * the UUID is online, not the owner, not already a member, and the 16-member cap.
 */
public record StationMemberPayload(int slot, UUID uuid) implements CustomPacketPayload {

    public static final Type<StationMemberPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "station_add_member"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StationMemberPayload> STREAM_CODEC =
            StreamCodec.of((buf, p) -> {
                buf.writeVarInt(p.slot);
                buf.writeUUID(p.uuid);
            }, buf -> new StationMemberPayload(buf.readVarInt(), buf.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
