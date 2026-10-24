package za.co.neroland.nerospace.rocket;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;

/**
 * Global rocket destination order and display names (used by the in-rocket selector).
 *
 * <p><b>Indices are persisted</b> (rocket NBT, synced menu data, availability bitmask) and are append-only:
 * {@code HOME=0, STATION=1, GREENXERTZ=2, CINDARA=3, GLACIRA=4} are frozen since 1.x; 2.0 appended
 * {@code LUNA=5, MARS=6, NYXARA=7}. A destination whose dimension is absent on the server is never offered.</p>
 */
public final class Destinations {

    public static final int HOME = 0;
    public static final int STATION = 1;
    public static final int GREENXERTZ = 2;
    public static final int CINDARA = 3;
    public static final int GLACIRA = 4;
    public static final int LUNA = 5;
    public static final int MARS = 6;
    public static final int NYXARA = 7;

    private static final List<ResourceKey<Level>> ORDER = List.of(
            ModDimensions.OVERWORLD,
            ModDimensions.STATION_LEVEL,
            ModDimensions.GREENXERTZ_LEVEL,
            ModDimensions.CINDARA_LEVEL,
            ModDimensions.GLACIRA_LEVEL,
            ModDimensions.LUNA_LEVEL,
            ModDimensions.MARS_LEVEL,
            ModDimensions.NYXARA_LEVEL);

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
        return key == null ? defaultIndex(tier, ModDimensions.OVERWORLD) : indexOf(key);
    }

    public static int defaultIndex(RocketTier tier, ResourceKey<Level> currentDimension) {
        int mask = availableMask(tier, currentDimension);
        if (!currentDimension.equals(ModDimensions.OVERWORLD) && (mask & bit(HOME)) != 0) {
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

    /**
     * Destinations this tier may pick from {@code currentDimension}, ignoring server availability (pure; used
     * on load and in tests). Use {@link #availableMask(RocketTier, ResourceKey, MinecraftServer)} when a server
     * is at hand.
     */
    public static int availableMask(RocketTier tier, ResourceKey<Level> currentDimension) {
        return availableMask(tier, currentDimension, null);
    }

    /**
     * Destinations this tier may pick from {@code currentDimension} on {@code server}: planets whose dimension is
     * not present on the server are left out. A null server skips the availability check.
     */
    public static int availableMask(RocketTier tier, ResourceKey<Level> currentDimension,
            @Nullable MinecraftServer server) {
        int mask = 0;
        if (!currentDimension.equals(ModDimensions.OVERWORLD)) {
            mask |= bit(HOME);
        }
        for (ResourceKey<Level> key : tier.destinations()) {
            if (!key.equals(currentDimension) && Planets.isAvailable(server, key)) {
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

    /** Every planet destination (anything but Home) needs a fuel reserve for the return trip. */
    public static boolean needsReturnReserve(ResourceKey<Level> target) {
        return Planets.isPlanet(target);
    }

    /**
     * Stable English name for logs, telemetry, the public route API and network payloads. The five 1.x names
     * ({@code Home}, {@code Orbital Station}, {@code Greenxertz}, {@code Cindara}, {@code Glacira}) are frozen.
     */
    public static String name(ResourceKey<Level> key) {
        if (key.equals(ModDimensions.OVERWORLD)) {
            return "Home";
        }
        PlanetDefinition planet = Planets.get(key);
        return planet == null ? "Unknown" : planet.englishName();
    }

    /**
     * Localised player-facing name ({@code destination.nerospace.<id>}). {@link #name} stays the stable
     * English id for logs, telemetry breadcrumbs, the public route API and network payloads.
     */
    public static Component displayName(ResourceKey<Level> key) {
        return Component.translatable("destination.nerospace." + langId(key));
    }

    /** Localised short label for the cramped destination node buttons. */
    public static Component shortDisplayName(ResourceKey<Level> key) {
        return Component.translatable("destination.nerospace." + langId(key) + ".short");
    }

    private static String langId(ResourceKey<Level> key) {
        if (key.equals(ModDimensions.OVERWORLD)) {
            return "home";
        }
        PlanetDefinition planet = Planets.get(key);
        return planet == null ? "unknown" : planet.langId();
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
