package za.co.neroland.nerospace.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.world.OxygenManager;
import za.co.neroland.nerospace.world.gravity.GravityManager;

/**
 * Public, read-only facade over Nerospace planet identity and traits.
 *
 * <p><b>Public API — semver-stable.</b> This and the records it returns ({@link PlanetId},
 * {@link PlanetTraits}, {@link Hazard}) are the only supported way for other Neroland mods (e.g. the Core
 * meteor grinder's {@code currentPlanet} lookup) to address Nerospace dimensions. Everything outside
 * {@code za.co.neroland.nerospace.api} is internal and may change without notice.</p>
 *
 * <p>This is a thin facade: it adds no state, wraps the existing internal registry/managers, and never
 * exposes mutable internals.</p>
 */
public final class NerospacePlanets {

    /** Lush low-gravity moon ({@code nerospace:greenxertz}). */
    public static final PlanetId GREENXERTZ = of("greenxertz");
    /** Volcanic, heat-hazard moon ({@code nerospace:cindara}). */
    public static final PlanetId CINDARA = of("cindara");
    /** Frozen, cold-hazard moon ({@code nerospace:glacira}). */
    public static final PlanetId GLACIRA = of("glacira");
    /** The orbital station void ({@code nerospace:station}) — near-zero gravity, airless. */
    public static final PlanetId STATION = of("station");

    private static final List<PlanetId> ALL = List.of(GREENXERTZ, CINDARA, GLACIRA, STATION);

    /** Dimension key → planet id (keys are interned, so a direct map lookup is exact). */
    private static final Map<ResourceKey<Level>, PlanetId> BY_DIMENSION = Map.of(
            ModDimensions.GREENXERTZ_LEVEL, GREENXERTZ,
            ModDimensions.CINDARA_LEVEL, CINDARA,
            ModDimensions.GLACIRA_LEVEL, GLACIRA,
            ModDimensions.STATION_LEVEL, STATION);

    private NerospacePlanets() {
    }

    /** The planet id for a Nerospace dimension path — matches how {@code ModDimensions} builds its keys. */
    private static PlanetId of(String path) {
        return new PlanetId(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, path));
    }

    /** Every Nerospace planet id, in a stable order. The returned list is immutable. */
    public static List<PlanetId> all() {
        return ALL;
    }

    /** The planet for a dimension key, or empty for Earth / any non-Nerospace dimension. */
    public static Optional<PlanetId> byDimension(ResourceKey<Level> dimension) {
        return Optional.ofNullable(dimension == null ? null : BY_DIMENSION.get(dimension));
    }

    /** The planet for a dimension identifier (e.g. {@code nerospace:cindara}), or empty if not a planet. */
    public static Optional<PlanetId> byId(Identifier id) {
        if (id == null) {
            return Optional.empty();
        }
        for (PlanetId planet : ALL) {
            if (planet.id().equals(id)) {
                return Optional.of(planet);
            }
        }
        return Optional.empty();
    }

    /** The planet for a dimension id string (e.g. {@code "nerospace:glacira"}), or empty if not a planet. */
    public static Optional<PlanetId> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        for (PlanetId planet : ALL) {
            if (planet.asString().equals(id)) {
                return Optional.of(planet);
            }
        }
        return Optional.empty();
    }

    /**
     * The Nerospace planet the entity is currently in, or empty when it is on Earth / a non-Nerospace
     * dimension. This is the exact hook the Core meteor grinder uses for planet-bound material weighting.
     * Works for any {@link Entity}, including {@code Player}.
     */
    public static Optional<PlanetId> currentPlanet(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        Level level = entity.level();
        return level == null ? Optional.empty() : byDimension(level.dimension());
    }

    /** The read-only {@link PlanetTraits} for a planet (gravity default, airless flag, hazard). */
    public static PlanetTraits traits(PlanetId planet) {
        if (planet == null) {
            throw new IllegalArgumentException("planet must not be null");
        }
        ResourceKey<Level> dimension = planet.dimension();
        return new PlanetTraits(
                planet,
                GravityManager.defaultFactor(dimension),
                OxygenManager.isAirless(dimension),
                hazard(dimension));
    }

    /**
     * The exact gravity factor at a position ({@code 1.0} = Earth-normal), accounting for biome gravity
     * tags and terraforming overrides — unlike {@link PlanetTraits#defaultGravity()}, which is the flat
     * dimension default. Server-side.
     */
    public static double gravityAt(ServerLevel level, BlockPos pos) {
        return GravityManager.factorAt(level, pos);
    }

    private static Hazard hazard(ResourceKey<Level> dimension) {
        return switch (OxygenManager.hazardFor(dimension)) {
            case HEAT -> Hazard.HEAT;
            case COLD -> Hazard.COLD;
            case NONE -> Hazard.NONE;
        };
    }
}
