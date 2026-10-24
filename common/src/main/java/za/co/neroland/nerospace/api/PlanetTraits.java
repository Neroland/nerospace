package za.co.neroland.nerospace.api;

import java.util.Optional;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Read-only, immutable snapshot of a planet's gameplay traits.
 *
 * <p><b>Public API — semver-stable.</b> Obtain via {@link NerospacePlanets#traits(PlanetId)}. The record grows
 * additively: the first four components and their accessor names ({@code planet()}, {@code defaultGravity()},
 * {@code airless()}, {@code hazard()}) are unchanged since 1.x, and the 1.x four-argument constructor is kept.
 * Consumers that read accessors by name keep working; new components are appended.</p>
 *
 * @param planet          the planet these traits describe
 * @param defaultGravity  the flat dimension-default gravity factor ({@code 1.0} = Earth-normal), config-scaled,
 *                        with no per-position biome-tag or terraforming override applied. For an exact
 *                        per-position reading use {@link NerospacePlanets#gravityAt}.
 * @param airless         {@code true} if the world has no breathable atmosphere
 * @param hazard          the planet's primary environmental {@link Hazard}; a position can carry a different one,
 *                        see {@link NerospacePlanets#hazardAt}
 * @param archetype       the broad kind of world
 * @param temperatureBand coarse surface temperature
 * @param radiation       ambient radiation, 0..3 (weather and biomes can raise the effective value)
 * @param dayLengthTicks  length of one planet day in ticks; {@code 0} means the clock is fixed
 * @param skyProfile      the client sky profile id ({@code assets/<namespace>/sky/<path>.json})
 * @param terraformTarget the mature (stage 3) terraformed biome, empty where terraforming is refused
 * @param villagerCulture the native villager culture, empty where none lives
 * @param colonyProfile   the colony profile a colony mod may use for this planet
 */
public record PlanetTraits(
        PlanetId planet, double defaultGravity, boolean airless, Hazard hazard,
        PlanetArchetype archetype, TemperatureBand temperatureBand, int radiation,
        long dayLengthTicks,
        Identifier skyProfile,
        Optional<ResourceKey<Biome>> terraformTarget,
        Optional<Identifier> villagerCulture,
        Identifier colonyProfile) {

    private static final Identifier VOID_SKY = Identifier.fromNamespaceAndPath("nerospace", "void");
    private static final Identifier GENERIC_COLONY = Identifier.fromNamespaceAndPath("nerospace", "generic");

    public PlanetTraits {
        if (planet == null) {
            throw new IllegalArgumentException("PlanetTraits planet must not be null");
        }
        if (hazard == null) {
            throw new IllegalArgumentException("PlanetTraits hazard must not be null");
        }
        archetype = archetype == null ? PlanetArchetype.BARREN_MOON : archetype;
        temperatureBand = temperatureBand == null ? TemperatureBand.TEMPERATE : temperatureBand;
        radiation = Math.max(0, Math.min(3, radiation));
        dayLengthTicks = Math.max(0L, dayLengthTicks);
        skyProfile = skyProfile == null ? VOID_SKY : skyProfile;
        terraformTarget = terraformTarget == null ? Optional.empty() : terraformTarget;
        villagerCulture = villagerCulture == null ? Optional.empty() : villagerCulture;
        colonyProfile = colonyProfile == null ? GENERIC_COLONY : colonyProfile;
    }

    /**
     * The 1.x constructor, kept for binary compatibility. The components added in 2.0 take neutral defaults
     * (a barren, temperate, radiation-free world with a fixed clock and no culture).
     */
    public PlanetTraits(PlanetId planet, double defaultGravity, boolean airless, Hazard hazard) {
        this(planet, defaultGravity, airless, hazard, PlanetArchetype.BARREN_MOON, TemperatureBand.TEMPERATE, 0,
                0L, VOID_SKY, Optional.empty(), Optional.empty(), GENERIC_COLONY);
    }

    /** Whether the planet's clock is fixed (it has no day length). */
    public boolean fixedClock() {
        return this.dayLengthTicks == 0L;
    }

    /** A copy with a different {@link #defaultGravity()} (used to apply the config multiplier). */
    public PlanetTraits withDefaultGravity(double gravity) {
        return new PlanetTraits(this.planet, gravity, this.airless, this.hazard, this.archetype,
                this.temperatureBand, this.radiation, this.dayLengthTicks, this.skyProfile, this.terraformTarget,
                this.villagerCulture, this.colonyProfile);
    }
}
