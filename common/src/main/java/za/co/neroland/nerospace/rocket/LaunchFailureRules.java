package za.co.neroland.nerospace.rocket;

import net.minecraft.world.phys.Vec3;

/**
 * What a finished ascent does when the destination dimension is not loaded. The hull, its fuel and the seated
 * crew stay; the rocket is put back on the pad it lifted from.
 */
public final class LaunchFailureRules {

    /** Told to every seated player. The destination name is omitted: it may not resolve on this server. */
    public static final String DESTINATION_MISSING = "entity.nerospace.rocket.destination_missing";

    private LaunchFailureRules() {
    }

    /** {@code true} when the launch must abort without spending the rocket or its fuel. */
    public static boolean abort(boolean destinationLevelPresent) {
        return !destinationLevelPresent;
    }

    /** Feet position of a rocket standing on a launch-pad block. */
    public static Vec3 padStand(int x, int y, int z) {
        return new Vec3(x + 0.5D, y + LandingResolver.SURFACE_HEIGHT, z + 0.5D);
    }
}
