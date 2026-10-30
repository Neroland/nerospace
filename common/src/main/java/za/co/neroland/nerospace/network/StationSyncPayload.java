package za.co.neroland.nerospace.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.rocket.StationDockingRule;
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

    public static final Type<StationSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "station_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StationSyncPayload> STREAM_CODEC =
            StreamCodec.of(StationSyncPayload::write, StationSyncPayload::read);

    /** Snapshot every station in founding order (no viewer filter — server-side tools only). */
    public static StationSyncPayload of(StationRegistry registry) {
        return of(registry.all(), e -> true);
    }

    /**
     * Snapshot for one viewer: a {@code MEMBERS}-policy station the viewer cannot {@code canUse} is left out, so its
     * name never reaches a player who may not dock there (005 §4; the server cycler skips it anyway).
     */
    public static StationSyncPayload of(StationRegistry registry, net.minecraft.server.level.ServerPlayer viewer) {
        return of(registry.all(), e -> StationDockingRule.mayChoose(e.docking(), StationRegistry.canUse(e, viewer)));
    }

    /** Pure core of {@link #of(StationRegistry, net.minecraft.server.level.ServerPlayer)}: keeps the visible rows. */
    public static StationSyncPayload of(List<StationRegistry.StationEntry> all,
            Predicate<StationRegistry.StationEntry> visible) {
        List<StationRegistry.StationEntry> shown = new ArrayList<>();
        for (StationRegistry.StationEntry e : all) {
            if (visible.test(e)) {
                shown.add(e);
            }
        }
        int[] slots = new int[shown.size()];
        String[] names = new String[shown.size()];
        for (int i = 0; i < shown.size(); i++) {
            slots[i] = shown.get(i).slot();
            names[i] = shown.get(i).name();
        }
        return new StationSyncPayload(slots, names);
    }

    private static void write(RegistryFriendlyByteBuf buf, StationSyncPayload payload) {
        buf.writeVarInt(payload.slots.length);
        for (int i = 0; i < payload.slots.length; i++) {
            buf.writeVarInt(payload.slots[i]);
            buf.writeUtf(payload.names[i]);
        }
    }

    private static StationSyncPayload read(RegistryFriendlyByteBuf buf) {
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
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
