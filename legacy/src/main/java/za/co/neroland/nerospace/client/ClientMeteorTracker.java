package za.co.neroland.nerospace.client;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

import za.co.neroland.nerospace.network.MeteorSyncPayload;

/**
 * Client-side holder for the latest nearest-meteor snapshot (meteor-events design §6). Fed by
 * {@link MeteorSyncPayload}; read by the tracker readout in {@code NerospaceClient.onClientTick}.
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
