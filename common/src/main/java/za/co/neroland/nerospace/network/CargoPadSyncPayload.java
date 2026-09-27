package za.co.neroland.nerospace.network;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.api.route.PadHandle;

/**
 * Server → client: the destination list a Cargo Pad GUI may pick from, for one open menu. Sent when the
 * menu opens and again whenever the viewer's visible list changes; {@code ContainerData} is int-only so the
 * names ride here (the same pattern as {@link StationSyncPayload}).
 *
 * <p><b>Privacy (POPIA/GDPR):</b> the list is exactly the pads the <em>viewer</em> may route to (their own,
 * ones they were granted, public ones, stations they manage) — never another player's private pad, and
 * never an owner. Each row is id + label + dimension label + station flag.</p>
 */
public record CargoPadSyncPayload(int containerId, int[] ids, String[] names, String[] dimensions, boolean[] stations)
        implements CustomPacketPayload {

    public static final Type<CargoPadSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "cargo_pad_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CargoPadSyncPayload> STREAM_CODEC =
            StreamCodec.of(CargoPadSyncPayload::write, CargoPadSyncPayload::read);

    public static CargoPadSyncPayload of(int containerId, List<PadHandle> visible) {
        int n = visible.size();
        int[] ids = new int[n];
        String[] names = new String[n];
        String[] dims = new String[n];
        boolean[] stations = new boolean[n];
        for (int i = 0; i < n; i++) {
            PadHandle pad = visible.get(i);
            ids[i] = pad.id();
            names[i] = pad.name();
            dims[i] = za.co.neroland.nerospace.rocket.Destinations.name(pad.dimension());
            stations[i] = pad.isStation();
        }
        return new CargoPadSyncPayload(containerId, ids, names, dims, stations);
    }

    private static void write(RegistryFriendlyByteBuf buf, CargoPadSyncPayload payload) {
        buf.writeVarInt(payload.containerId);
        buf.writeVarInt(payload.ids.length);
        for (int i = 0; i < payload.ids.length; i++) {
            buf.writeVarInt(payload.ids[i]);
            buf.writeUtf(payload.names[i]);
            buf.writeUtf(payload.dimensions[i]);
            buf.writeBoolean(payload.stations[i]);
        }
    }

    private static CargoPadSyncPayload read(RegistryFriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int n = buf.readVarInt();
        int[] ids = new int[n];
        String[] names = new String[n];
        String[] dims = new String[n];
        boolean[] stations = new boolean[n];
        for (int i = 0; i < n; i++) {
            ids[i] = buf.readVarInt();
            names[i] = buf.readUtf();
            dims[i] = buf.readUtf();
            stations[i] = buf.readBoolean();
        }
        return new CargoPadSyncPayload(containerId, ids, names, dims, stations);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
