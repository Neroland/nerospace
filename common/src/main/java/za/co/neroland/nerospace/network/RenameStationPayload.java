package za.co.neroland.nerospace.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Client → server: a new name for an existing station (by slot), typed into the rename screen opened from
 * its Station Core. The server updates the registry, rebinds the Core, and renames the landing pad.
 * Registered (with its server handler) in {@code ModNetwork.init()}.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> carries only the station slot + the player-chosen name — no identity.</p>
 */
public record RenameStationPayload(int slot, String name) implements CustomPacketPayload {

    public static final Type<RenameStationPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "rename_station"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RenameStationPayload> STREAM_CODEC =
            StreamCodec.of((buf, p) -> {
                buf.writeVarInt(p.slot);
                buf.writeUtf(p.name, 64);
            }, buf -> new RenameStationPayload(buf.readVarInt(), buf.readUtf(64)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
