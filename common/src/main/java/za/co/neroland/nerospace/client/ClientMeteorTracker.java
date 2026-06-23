package za.co.neroland.nerospace.client;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;

import za.co.neroland.nerospace.network.MeteorSyncPayload;

/**
 * Client-side holder for the latest nearest-meteor snapshot (meteor-events design §6). Fed by
 * {@link MeteorSyncPayload} (the clientbound handler registered in {@code ModNetwork.init()}); read by
 * {@link MeteorTrackerHud} each client tick. Pure data — no client-only imports — so it loads safely
 * even where the handler is registered from common code.
 */
public final class ClientMeteorTracker {

    private static boolean present;
    @Nullable
    private static BlockPos pos;
    private static int state;

    private ClientMeteorTracker() {
    }

    public static void accept(MeteorSyncPayload payload) {
        present = payload.present();
        pos = payload.present() ? BlockPos.of(payload.pos()) : null;
        state = payload.state();
    }

    public static boolean isPresent() {
        return present && pos != null;
    }

    @Nullable
    public static BlockPos pos() {
        return pos;
    }

    public static int state() {
        return state;
    }
}
