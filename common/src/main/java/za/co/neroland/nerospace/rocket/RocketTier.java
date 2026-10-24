package za.co.neroland.nerospace.rocket;

import java.util.List;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.registry.ModDimensions;

/**
 * Rocket progression tiers. Each tier defines its fuel tank capacity, the fuel burned per launch
 * (both in millibuckets), and the ordered list of destinations it can reach. Destinations are
 * cumulative: a higher tier can still fly to every lower-tier destination, and the player picks the
 * target in the in-rocket UI. The tier's <em>signature</em> destination is pinned explicitly (it is the
 * default selection, and is the 1.x default for every tier — appending a destination never moves it).
 *
 * <p><b>Destination progression:</b> Tier 1 reaches the Orbital Station and the Moon; Tier 2 adds
 * Greenxertz and Mars; Tier 3 adds Cindara; Tier 4 adds Glacira and Nyxara. Lists are append-only per tier
 * (pre-global rocket saves index into them).</p>
 *
 * <p><b>Launch-pad progression</b> (the footprint a rocket of this tier must stand on — a higher pad
 * always satisfies a lower-tier rocket, see {@link LaunchPadMultiblock#padTier}):</p>
 * <ul>
 *   <li>Tier 1 — a single Launch Pad block.</li>
 *   <li>Tier 2 — a complete 3x3 pad.</li>
 *   <li>Tier 3 — a 3x3 pad ringed with Station Wall (5x5 outline).</li>
 *   <li>Tier 4 — a 5x5 Heavy Launch Complex (a Launch Gantry on the ring).</li>
 * </ul>
 *
 * <p>Cross-loader port note: the root scales these by {@code Config}/{@code Tuning} multipliers; the
 * multiloader inlines the base values (identity multiplier) until the config seam is ported.</p>
 */
public enum RocketTier {

    // Persisted by ORDINAL: never reorder. Destination lists: 1.x prefix unchanged, 2.0 entries appended.
    TIER_1(1, 3_000, 1_000, 2_000, 4_000, ModDimensions.STATION_LEVEL,
            List.of(ModDimensions.STATION_LEVEL, ModDimensions.LUNA_LEVEL)),
    TIER_2(2, 6_000, 2_000, 4_000, 8_000, ModDimensions.GREENXERTZ_LEVEL,
            List.of(ModDimensions.STATION_LEVEL, ModDimensions.GREENXERTZ_LEVEL, ModDimensions.MARS_LEVEL)),
    TIER_3(3, 12_000, 4_000, 8_000, 16_000, ModDimensions.CINDARA_LEVEL, List.of(
            ModDimensions.STATION_LEVEL, ModDimensions.GREENXERTZ_LEVEL, ModDimensions.CINDARA_LEVEL)),
    TIER_4(4, 24_000, 8_000, 16_000, 30_000, ModDimensions.GLACIRA_LEVEL, List.of(
            ModDimensions.STATION_LEVEL, ModDimensions.GREENXERTZ_LEVEL, ModDimensions.CINDARA_LEVEL,
            ModDimensions.GLACIRA_LEVEL, ModDimensions.NYXARA_LEVEL));

    private final int level;
    private final int fuelCapacity;
    private final int fuelPerLaunch;
    private final int oxygenCapacity;
    private final int powerCapacity;
    private final ResourceKey<Level> signature;
    private final List<ResourceKey<Level>> destinations;

    RocketTier(int level, int fuelCapacity, int fuelPerLaunch, int oxygenCapacity, int powerCapacity,
            ResourceKey<Level> signature, List<ResourceKey<Level>> destinations) {
        this.signature = signature;
        this.level = level;
        this.fuelCapacity = fuelCapacity;
        this.fuelPerLaunch = fuelPerLaunch;
        this.oxygenCapacity = oxygenCapacity;
        this.powerCapacity = powerCapacity;
        this.destinations = destinations;
    }

    /** Human-facing tier number (1-based). */
    public int level() {
        return this.level;
    }

    /** Fuel tank capacity, in millibuckets. */
    public int fuelCapacity() {
        return this.fuelCapacity;
    }

    /**
     * Onboard oxygen (life-support) tank capacity, in millibuckets. Loaded before launch from an Oxygen
     * Generator via a gas pipe into the launch pad; keeps the rider breathing during ascent and seeds a
     * slow-draining surface reserve on arrival. Bigger tiers carry far more (longer off-world stays).
     */
    public int oxygenCapacity() {
        return this.oxygenCapacity;
    }

    /** Onboard power buffer capacity, in FE — loaded by a Launch Controller and carried for the trip. */
    public int powerCapacity() {
        return this.powerCapacity;
    }

    /** Fuel consumed by a single launch, in millibuckets (config-scaled, clamped to the tank so a launch is always possible). */
    public int fuelPerLaunch() {
        return Math.min(NerospaceConfig.scale(this.fuelPerLaunch, NerospaceConfig.fuelCostMultiplier()),
                this.fuelCapacity);
    }

    /** Ordered list of reachable destinations (1.x entries first, 2.0 entries appended). */
    public List<ResourceKey<Level>> destinations() {
        return this.destinations;
    }

    /** Whether this tier can fly anywhere. */
    public boolean hasDestination() {
        return !this.destinations.isEmpty();
    }

    /** The tier's signature destination: its default selection, pinned to the 1.x value. */
    public ResourceKey<Level> signature() {
        return this.signature;
    }

    /** Default selection: the index of the tier's {@link #signature()} in {@link #destinations()}. */
    public int defaultDestinationIndex() {
        return Math.max(0, this.destinations.indexOf(this.signature));
    }

    /** Safe destination lookup by index (clamped). */
    public ResourceKey<Level> destination(int index) {
        if (this.destinations.isEmpty()) {
            return null;
        }
        int clamped = Math.floorMod(index, this.destinations.size());
        return this.destinations.get(clamped);
    }

    /**
     * Lang key explaining the launch pad this tier requires, shown when a deploy or launch is gated by
     * an insufficient pad. Mirrors the {@link LaunchPadMultiblock#padTier} progression.
     */
    public String padRequirementKey() {
        return switch (this.level) {
            case 4 -> "item.nerospace.rocket.pad_heavy_required";
            case 3 -> "item.nerospace.rocket.pad_ring_required";
            case 2 -> "item.nerospace.rocket.pad_incomplete";
            default -> "item.nerospace.rocket.pad_none";
        };
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
