package za.co.neroland.nerospace.api;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.world.gravity.GravityManager;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.PlanetHazards;
import za.co.neroland.nerospace.world.planet.Planets;

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
    /** The Moon ({@code nerospace:luna}) — added in 2.0. Its dimension may not exist on every server yet. */
    public static final PlanetId LUNA = of("luna");
    /** Mars ({@code nerospace:mars}) — added in 2.0. Its dimension may not exist on every server yet. */
    public static final PlanetId MARS = of("mars");
    /** Nyxara ({@code nerospace:nyxara}) — added in 2.0. Its dimension may not exist on every server yet. */
    public static final PlanetId NYXARA = of("nyxara");

    /** 1.x order first, 2.0 planets appended. */
    private static final List<PlanetId> ALL = List.of(GREENXERTZ, CINDARA, GLACIRA, STATION, LUNA, MARS, NYXARA);

    private NerospacePlanets() {
    }

    /** The planet id for a Nerospace dimension path — matches how {@code ModDimensions} builds its keys. */
    private static PlanetId of(String path) {
        return new PlanetId(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, path));
    }

    /**
     * Every Nerospace planet id, in a stable order: Greenxertz, Cindara, Glacira, Station, then (2.0) Luna,
     * Mars, Nyxara. New planets are only ever appended. A planet whose dimension is not present on a given
     * server is still listed; check {@code server.getLevel(id.dimension())} before travelling there.
     * The returned list is immutable.
     */
    public static List<PlanetId> all() {
        return ALL;
    }

    /** The planet for a dimension key, or empty for Earth / any non-Nerospace dimension. */
    public static Optional<PlanetId> byDimension(ResourceKey<Level> dimension) {
        return Planets.byDimension(dimension).map(PlanetDefinition::id);
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

    /**
     * The read-only {@link PlanetTraits} for a planet. {@code defaultGravity} is config-scaled. An id that is
     * not a Nerospace planet yields Earth-like neutral traits (gravity 1.0, breathable, no hazard).
     */
    public static PlanetTraits traits(PlanetId planet) {
        if (planet == null) {
            throw new IllegalArgumentException("planet must not be null");
        }
        ResourceKey<Level> dimension = planet.dimension();
        return Planets.byId(planet)
                .map(p -> p.traits().withDefaultGravity(GravityManager.defaultFactor(dimension)))
                .orElseGet(() -> new PlanetTraits(planet, GravityManager.defaultFactor(dimension), false, Hazard.NONE));
    }

    /**
     * The environmental hazard at an exact position. Usually the planet's primary
     * {@link PlanetTraits#hazard()}, but a biome can carry a different one (Nyxara's day side runs hot, its night
     * side is cold and irradiated) and some weather adds radiation (a Mars dust storm). {@link Hazard#NONE} for
     * any non-Nerospace dimension. Server-side; reads only already-loaded state.
     *
     * @since 2.0.0
     */
    public static Hazard hazardAt(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return Hazard.NONE;
        }
        return PlanetHazards.at(level, pos);
    }

    /**
     * The exact gravity factor at a position ({@code 1.0} = Earth-normal), accounting for biome gravity
     * tags and terraforming overrides — unlike {@link PlanetTraits#defaultGravity()}, which is the flat
     * dimension default. Server-side.
     */
    public static double gravityAt(ServerLevel level, BlockPos pos) {
        return GravityManager.factorAt(level, pos);
    }
}
