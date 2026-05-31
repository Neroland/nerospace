package za.co.neroland.nerospace.rocket;

import javax.annotation.Nullable;

import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;

import za.co.neroland.nerospace.registry.ModDimensions;

/**
 * Rocket progression tiers (Phase 4). Each tier defines its fuel tank capacity, the fuel burned per
 * launch (both in millibuckets), and the destination it can reach. Higher tiers have no destination
 * wired yet — they are the framework for future planets (Phase 7+) and are the natural place to gate
 * craftable add-on modules earned by visiting earlier planets.
 *
 * <p>Fuel is modelled as a liquid quantity (mB) so the rocket reads as a fuel-tank machine; a full
 * NeoForge {@code Fluid}/bucket (with world placement + client fluid rendering) is a deliberate
 * follow-up once it can be validated in {@code runClient}.</p>
 */
public enum RocketTier {

    /** Tier 1 — reaches Greenxertz, the Phase 3 planet. The MVP-completing rocket. */
    TIER_1(1, 3_000, 1_000, ModDimensions.GREENXERTZ_LEVEL),
    /** Tier 2 — larger tank, no destination yet (reserved for the next planet). */
    TIER_2(2, 6_000, 2_000, null),
    /** Tier 3 — deep-space frame, no destination yet. */
    TIER_3(3, 12_000, 4_000, null);

    private final int level;
    private final int fuelCapacity;
    private final int fuelPerLaunch;
    @Nullable
    private final ResourceKey<Level> destination;

    RocketTier(int level, int fuelCapacity, int fuelPerLaunch, @Nullable ResourceKey<Level> destination) {
        this.level = level;
        this.fuelCapacity = fuelCapacity;
        this.fuelPerLaunch = fuelPerLaunch;
        this.destination = destination;
    }

    /** Human-facing tier number (1-based). */
    public int level() {
        return this.level;
    }

    /** Fuel tank capacity, in millibuckets. */
    public int fuelCapacity() {
        return this.fuelCapacity;
    }

    /** Fuel consumed by a single launch, in millibuckets. */
    public int fuelPerLaunch() {
        return this.fuelPerLaunch;
    }

    /** The level this tier can fly to, or {@code null} if no destination is wired yet. */
    @Nullable
    public ResourceKey<Level> destination() {
        return this.destination;
    }

    /** Whether this tier has somewhere to go (false for not-yet-built planets). */
    public boolean hasDestination() {
        return this.destination != null;
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
