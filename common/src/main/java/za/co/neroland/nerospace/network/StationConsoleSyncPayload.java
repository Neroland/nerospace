package za.co.neroland.nerospace.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Server → the viewer of one open Station Console: what the int-only container data cannot carry.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> {@code members} and {@code owner} are filled <b>only</b> for a viewer who passes
 * {@code canUse} (owner, member, operator, or anyone on an unowned station); a visitor receives an empty list, no
 * owner and the {@code hasOwner} flag alone. No display name is ever sent — the client resolves names from its own
 * tab list and never stores them. The paired-pad rows carry the station's own paired pads only.</p>
 */
public record StationConsoleSyncPayload(int containerId, int slot, String name, boolean hasOwner,
        Optional<UUID> owner, List<UUID> members, int viewerRole, int docking, int classCooldownTicks,
        int sealed, int airtight, int powerFe, int solarWings, int revealCount, int inbound,
        List<PairedPad> pairedPads, Optional<BlockPos> blockedAt) implements CustomPacketPayload {

    /** Viewer roles (drawing hints only; the server re-checks every action). */
    public static final int ROLE_VISITOR = 0;
    public static final int ROLE_MEMBER = 1;
    public static final int ROLE_OWNER = 2;
    public static final int ROLE_OPERATOR = 3;
    public static final int ROLE_UNOWNED = 4;

    /** One pad on another world named after this station ("paired pad", D5 rule 3). */
    public record PairedPad(String dimension, String padName, boolean standing) {
    }

    public static final Type<StationConsoleSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "station_console_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StationConsoleSyncPayload> STREAM_CODEC =
            StreamCodec.of(StationConsoleSyncPayload::write, StationConsoleSyncPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, StationConsoleSyncPayload p) {
        buf.writeVarInt(p.containerId);
        buf.writeVarInt(p.slot);
        buf.writeUtf(p.name, 64);
        buf.writeBoolean(p.hasOwner);
        buf.writeBoolean(p.owner.isPresent());
        p.owner.ifPresent(buf::writeUUID);
        buf.writeVarInt(p.members.size());
        for (UUID m : p.members) {
            buf.writeUUID(m);
        }
        buf.writeVarInt(p.viewerRole);
        buf.writeVarInt(p.docking);
        buf.writeVarInt(p.classCooldownTicks);
        buf.writeVarInt(p.sealed);
        buf.writeVarInt(p.airtight);
        buf.writeVarInt(p.powerFe);
        buf.writeVarInt(p.solarWings);
        buf.writeVarInt(p.revealCount);
        buf.writeVarInt(p.inbound);
        buf.writeVarInt(p.pairedPads.size());
        for (PairedPad pad : p.pairedPads) {
            buf.writeUtf(pad.dimension(), 128);
            buf.writeUtf(pad.padName(), 64);
            buf.writeBoolean(pad.standing());
        }
        buf.writeBoolean(p.blockedAt.isPresent());
        p.blockedAt.ifPresent(buf::writeBlockPos);
    }

    private static StationConsoleSyncPayload read(RegistryFriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int slot = buf.readVarInt();
        String name = buf.readUtf(64);
        boolean hasOwner = buf.readBoolean();
        Optional<UUID> owner = buf.readBoolean() ? Optional.of(buf.readUUID()) : Optional.empty();
        int n = Math.min(buf.readVarInt(), 64);
        List<UUID> members = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            members.add(buf.readUUID());
        }
        int role = buf.readVarInt();
        int docking = buf.readVarInt();
        int cooldown = buf.readVarInt();
        int sealed = buf.readVarInt();
        int airtight = buf.readVarInt();
        int power = buf.readVarInt();
        int wings = buf.readVarInt();
        int reveal = buf.readVarInt();
        int inbound = buf.readVarInt();
        int pads = Math.min(buf.readVarInt(), 256);
        List<PairedPad> paired = new ArrayList<>(pads);
        for (int i = 0; i < pads; i++) {
            paired.add(new PairedPad(buf.readUtf(128), buf.readUtf(64), buf.readBoolean()));
        }
        Optional<BlockPos> blocked = buf.readBoolean() ? Optional.of(buf.readBlockPos()) : Optional.empty();
        return new StationConsoleSyncPayload(containerId, slot, name, hasOwner, owner, List.copyOf(members), role,
                docking, cooldown, sealed, airtight, power, wings, reveal, inbound, List.copyOf(paired), blocked);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
