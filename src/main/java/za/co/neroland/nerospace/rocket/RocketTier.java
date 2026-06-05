package za.co.neroland.nerospace.rocket;

import java.util.List;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.Tuning;
import za.co.neroland.nerospace.registry.ModDimensions;

/**
 * Rocket progression tiers. Each tier defines its fuel tank capacity, the fuel burned per launch
 * (both in millibuckets), and the ordered list of destinations it can reach. Destinations are
 * cumulative: a higher tier can still fly to every lower-tier destination, and the player picks the
 * target in the in-rocket UI. The tier's <em>signature</em> destination (the newest one it unlocks)
 * is the last entry and the default selection.
 *
 * <p>Progression: Tier 1 reaches the Orbital Station; Tier 2 adds Greenxertz; Tier 3 adds Cindara;
 * Tier 4 adds Glacira (and deploys only on the Heavy Launch Complex — see
 * {@code NEW_DESTINATION_DESIGN.md}).</p>
 */
public enum RocketTier {

    TIER_1(1, 3_000, 1_000, List.of(ModDimensions.STATION_LEVEL)),
    TIER_2(2, 6_000, 2_000, List.of(ModDimensions.STATION_LEVEL, ModDimensions.GREENXERTZ_LEVEL)),
    TIER_3(3, 12_000, 4_000, List.of(
            ModDimensions.STATION_LEVEL, ModDimensions.GREENXERTZ_LEVEL, ModDimensions.CINDARA_LEVEL)),
    TIER_4(4, 24_000, 8_000, List.of(
            ModDimensions.STATION_LEVEL, ModDimensions.GREENXERTZ_LEVEL, ModDimensions.CINDARA_LEVEL,
            ModDimensions.GLACIRA_LEVEL));

    private final int level;
    private final int fuelCapacity;
    private final int fuelPerLaunch;
    private final List<ResourceKey<Level>> destinations;

    RocketTier(int level, int fuelCapacity, int fuelPerLaunch, List<ResourceKey<Level>> destinations) {
        this.level = level;
        this.fuelCapacity = fuelCapacity;
        this.fuelPerLaunch = fuelPerLaunch;
        this.destinations = destinations;
    }

    /** Human-facing tier number (1-based). */
    public int level() {
        return this.level;
    }

    /** Fuel tank capacity, in millibuckets (base value scaled by {@code energyRateMultiplier}). */
    public int fuelCapacity() {
        return Tuning.rocketFuelCapacity(this.fuelCapacity);
    }

    /**
     * Fuel consumed by a single launch, in millibuckets (base value scaled by
     * {@code fuelCostMultiplier}, clamped to the tank size so a launch is always possible).
     */
    public int fuelPerLaunch() {
        return Tuning.rocketFuelPerLaunch(this.fuelPerLaunch, this.fuelCapacity);
    }

    /** Ordered list of reachable destinations (lowest unlock first, signature destination last). */
    public List<ResourceKey<Level>> destinations() {
        return this.destinations;
    }

    /** Whether this tier can fly anywhere. */
    public boolean hasDestination() {
        return !this.destinations.isEmpty();
    }

    /** Default selection: the tier's signature (newest) destination. */
    public int defaultDestinationIndex() {
        return Math.max(0, this.destinations.size() - 1);
    }

    /** Safe destination lookup by index (clamped). */
    public ResourceKey<Level> destination(int index) {
        if (this.destinations.isEmpty()) {
            return null;
        }
        int clamped = Math.floorMod(index, this.destinations.size());
        return this.destinations.get(clamped);
    }

    /** Safe lookup by ordinal for persistence/synced data. */
    public static RocketTier byOrdinal(int ordinal) {
        RocketTier[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return TIER_1;
        }
        return values[ordinal];
    }
}
