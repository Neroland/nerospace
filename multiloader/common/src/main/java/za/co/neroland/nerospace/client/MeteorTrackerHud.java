package za.co.neroland.nerospace.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.meteor.MeteorSite;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Meteor Tracker readout (meteor-events design §6): while the player holds a Meteor Tracker, show the
 * nearest meteor's state (incoming / landed), compass heading and distance in the action bar. Purely
 * presentational — the data arrives server-authoritatively via {@link ClientMeteorTracker}; this just
 * draws it.
 *
 * <p>Cross-loader port note: {@link #tick()} is called once per client tick from each loader's own
 * client-tick hook (NeoForge {@code ClientTickEvent.Post} on the game bus, Fabric
 * {@code ClientTickEvents.END_CLIENT_TICK}). Client-only — never loaded on a dedicated server.</p>
 */
public final class MeteorTrackerHud {

    private static final String[] COMPASS_8 = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private MeteorTrackerHud() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused()) {
            return;
        }
        boolean holding = mc.player.getMainHandItem().is(ModItems.METEOR_TRACKER.get())
                || mc.player.getOffhandItem().is(ModItems.METEOR_TRACKER.get());
        if (!holding) {
            return;
        }
        if (!ClientMeteorTracker.isPresent()) {
            mc.player.sendOverlayMessage(Component.translatable("item.nerospace.meteor_tracker.none"));
            return;
        }
        BlockPos target = ClientMeteorTracker.pos();
        if (target == null) {
            // isPresent() was true above, but pos() is @Nullable — guard the deref explicitly.
            return;
        }
        Vec3 p = mc.player.position();
        double dx = target.getX() + 0.5D - p.x;
        double dz = target.getZ() + 0.5D - p.z;
        int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        // Bearing where North = -Z, East = +X (Minecraft convention).
        double deg = (Math.toDegrees(Math.atan2(dx, -dz)) + 360.0D) % 360.0D;
        String heading = COMPASS_8[(int) Math.round(deg / 45.0D) & 7];
        Component state = Component.translatable(ClientMeteorTracker.state() == MeteorSite.LANDED
                ? "item.nerospace.meteor_tracker.landed"
                : "item.nerospace.meteor_tracker.incoming");
        mc.player.sendOverlayMessage(
                Component.translatable("item.nerospace.meteor_tracker.readout", state, heading, dist));
    }
}
