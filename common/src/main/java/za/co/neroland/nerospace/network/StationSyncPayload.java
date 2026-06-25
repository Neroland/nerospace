package za.co.neroland.nerospace.network;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.rocket.StationRegistry;

/**
 * Server → client snapshot of the founded stations' display names (slot → name), pushed to a player
 * when they open a rocket so the in-rocket "Dock:" cycler can show the real charter name rather than
 * the generic "Station N" label. {@link net.minecraft.world.inventory.ContainerData} is int-only, so
 * the names can't ride the menu's synced data — this small parallel-array payload carries them instead.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> carries only the station <em>slot</em> and its <em>display name</em>
 * (chosen by whoever founded it). No player identity — no names, UUIDs or founders — is ever sent, in
 * keeping with {@link StationRegistry}'s deliberately identity-free storage.</p>
 *
 * <p>Cross-loader note: registered (with its client handler {@code ClientStations::accept}) in
 * {@code ModNetwork.init()}; both loaders' networking seams pick it up from the clientbound list.</p>
 */
public record StationSyncPayload(int[] slots, String[] names) implements CustomPacketPayload {

    public static final CustomPacketPayload.@NonNull Type<@NonNull StationSyncPayload> TYPE =
            new Type<>(NerospaceCommon.id("station_sync"));

    public static final @NonNull StreamCodec<RegistryFriendlyByteBuf, @NonNull StationSyncPayload> STREAM_CODEC =
            StreamCodec.of(StationSyncPayload::write, StationSyncPayload::read);

    /** Snapshot the registry's stations in founding order. */
    public static StationSyncPayload of(StationRegistry registry) {
        List<StationRegistry.StationEntry> all = registry.all();
        int[] slots = new int[all.size()];
        String[] names = new String[all.size()];
        for (int i = 0; i < all.size(); i++) {
            slots[i] = all.get(i).slot();
            names[i] = all.get(i).name();
        }
        return new StationSyncPayload(slots, names);
    }

    private static void write(RegistryFriendlyByteBuf buf, StationSyncPayload payload) {
        buf.writeVarInt(payload.slots.length);
        for (int i = 0; i < payload.slots.length; i++) {
            buf.writeVarInt(payload.slots[i]);
            buf.writeUtf(NerospaceCommon.requireNonNull(payload.names[i]));
        }
    }

    private static @NonNull StationSyncPayload read(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        int[] slots = new int[n];
        String[] names = new String[n];
        for (int i = 0; i < n; i++) {
            slots[i] = buf.readVarInt();
            names[i] = buf.readUtf();
        }
        return new StationSyncPayload(slots, names);
    }

    @Override
    public CustomPacketPayload.@NonNull Type<? extends @NonNull CustomPacketPayload> type() {
        return TYPE;
    }
}
