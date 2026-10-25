package za.co.neroland.nerospace.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.rocket.PadRegistry;

/**
 * Server → client snapshot of the selectable landing nodes' labels (dimension id + name, in the
 * {@link PadRegistry#inDimension} order the Rocket screen's pad index addresses), pushed when a player opens a
 * rocket so the planet card can say "Pad: Mare Base" (004 §5).
 *
 * <p><b>Privacy (POPIA/GDPR):</b> pads carry no player identity; only the dimension id and the label travel.</p>
 */
public record PadSyncPayload(List<String> dims, List<String> names) implements CustomPacketPayload {

    public static final Type<PadSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "pad_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PadSyncPayload> STREAM_CODEC =
            StreamCodec.of(PadSyncPayload::write, PadSyncPayload::read);

    /** Snapshot the selectable nodes (launch pads + docks, never pods) in commissioning order. */
    public static PadSyncPayload of(PadRegistry registry) {
        List<String> dims = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (PadRegistry.PadNode node : registry.all()) {
            if (node.kind().selectable()) {
                dims.add(node.dim());
                names.add(node.name());
            }
        }
        return new PadSyncPayload(dims, names);
    }

    private static void write(RegistryFriendlyByteBuf buf, PadSyncPayload payload) {
        buf.writeVarInt(payload.dims.size());
        for (int i = 0; i < payload.dims.size(); i++) {
            buf.writeUtf(payload.dims.get(i));
            buf.writeUtf(payload.names.get(i));
        }
    }

    private static PadSyncPayload read(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<String> dims = new ArrayList<>(n);
        List<String> names = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            dims.add(buf.readUtf());
            names.add(buf.readUtf());
        }
        return new PadSyncPayload(dims, names);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
