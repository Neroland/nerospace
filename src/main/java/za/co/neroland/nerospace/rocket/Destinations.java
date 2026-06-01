package za.co.neroland.nerospace.rocket;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.registry.ModDimensions;

/** Display names for rocket destinations (used by the in-rocket selector). */
public final class Destinations {

    private Destinations() {
    }

    public static String name(ResourceKey<Level> key) {
        if (key.equals(ModDimensions.STATION_LEVEL)) {
            return "Orbital Station";
        }
        if (key.equals(ModDimensions.GREENXERTZ_LEVEL)) {
            return "Greenxertz";
        }
        if (key.equals(ModDimensions.CINDARA_LEVEL)) {
            return "Cindara";
        }
        return "Unknown";
    }
}
