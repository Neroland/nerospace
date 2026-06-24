package za.co.neroland.nerospace.rocket;

import java.util.List;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.registry.ModDimensions;

/** Global rocket destination order and display names (used by the in-rocket selector). */
public final class Destinations {

    public static final int HOME = 0;
    public static final int STATION = 1;
    public static final int GREENXERTZ = 2;
    public static final int CINDARA = 3;
    public static final int GLACIRA = 4;

    private static final List<ResourceKey<Level>> ORDER = List.of(
            Level.OVERWORLD,
            ModDimensions.STATION_LEVEL,
            ModDimensions.GREENXERTZ_LEVEL,
            ModDimensions.CINDARA_LEVEL,
            ModDimensions.GLACIRA_LEVEL);

    private Destinations() {
    }

    public static List<ResourceKey<Level>> all() {
        return ORDER;
    }

    @Nullable
    public static ResourceKey<Level> byIndex(int index) {
        return index >= 0 && index < ORDER.size() ? ORDER.get(index) : null;
    }

    public static int indexOf(ResourceKey<Level> key) {
        for (int i = 0; i < ORDER.size(); i++) {
            if (ORDER.get(i).equals(key)) {
                return i;
            }
        }
        return -1;
    }

    public static int legacyIndex(RocketTier tier, int legacyDestination) {
        ResourceKey<Level> key = tier.destination(legacyDestination);
        return key == null ? defaultIndex(tier, Level.OVERWORLD) : indexOf(key);
    }

    public static int defaultIndex(RocketTier tier, ResourceKey<Level> currentDimension) {
        int mask = availableMask(tier, currentDimension);
        if (!currentDimension.equals(Level.OVERWORLD) && (mask & bit(HOME)) != 0) {
            return HOME;
        }
        int signature = indexOf(tier.destination(tier.defaultDestinationIndex()));
        if ((mask & bit(signature)) != 0) {
            return signature;
        }
        return firstAvailable(mask);
    }

    public static int sanitizeIndex(RocketTier tier, ResourceKey<Level> currentDimension, int index) {
        int mask = availableMask(tier, currentDimension);
        return (mask & bit(index)) != 0 ? index : firstAvailable(mask);
    }

    public static int availableMask(RocketTier tier, ResourceKey<Level> currentDimension) {
        int mask = 0;
        if (!currentDimension.equals(Level.OVERWORLD)) {
            mask |= bit(HOME);
        }
        for (ResourceKey<Level> key : tier.destinations()) {
            if (!key.equals(currentDimension)) {
                int index = indexOf(key);
                if (index >= 0) {
                    mask |= bit(index);
                }
            }
        }
        return mask;
    }

    public static boolean isAvailable(RocketTier tier, ResourceKey<Level> currentDimension, int index) {
        return (availableMask(tier, currentDimension) & bit(index)) != 0;
    }

    public static boolean needsReturnReserve(ResourceKey<Level> target) {
        return target.equals(ModDimensions.STATION_LEVEL)
                || target.equals(ModDimensions.GREENXERTZ_LEVEL)
                || target.equals(ModDimensions.CINDARA_LEVEL)
                || target.equals(ModDimensions.GLACIRA_LEVEL);
    }

    public static String name(ResourceKey<Level> key) {
        if (key.equals(Level.OVERWORLD)) {
            return "Home";
        }
        if (key.equals(ModDimensions.STATION_LEVEL)) {
            return "Orbital Station";
        }
        if (key.equals(ModDimensions.GREENXERTZ_LEVEL)) {
            return "Greenxertz";
        }
        if (key.equals(ModDimensions.CINDARA_LEVEL)) {
            return "Cindara";
        }
        if (key.equals(ModDimensions.GLACIRA_LEVEL)) {
            return "Glacira";
        }
        return "Unknown";
    }

    private static int firstAvailable(int mask) {
        for (int i = 0; i < ORDER.size(); i++) {
            if ((mask & bit(i)) != 0) {
                return i;
            }
        }
        return HOME;
    }

    private static int bit(int index) {
        return index < 0 ? 0 : 1 << index;
    }
}
