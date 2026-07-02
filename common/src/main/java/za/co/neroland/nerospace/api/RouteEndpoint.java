package za.co.neroland.nerospace.api;

import java.util.Optional;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Read-only, immutable description of a cargo-route endpoint: a dimension a Nerospace rocket can fly
 * cargo between, plus its player-facing display name.
 *
 * <p><b>Public API — semver-stable.</b> Part of {@code za.co.neroland.nerospace.api}, the only supported
 * surface for other Neroland mods. Obtain instances from {@link NerospaceRoutes} — never construct them
 * from internal {@code registry.ModDimensions} / {@code rocket.Destinations} constants. Endpoints include
 * the Overworld ("Home"), which is not a planet, so consumers keying on planets should use
 * {@link #planet()} and handle the empty case.</p>
 *
 * @param dimension the dimension {@link ResourceKey} of this endpoint (interned — {@code ==}/equals safe)
 * @param name      the display name shown to players (e.g. {@code "Home"}, {@code "Orbital Station"},
 *                  {@code "Glacira"})
 */
public record RouteEndpoint(ResourceKey<Level> dimension, String name) {

    public RouteEndpoint {
        if (dimension == null) {
            throw new IllegalArgumentException("RouteEndpoint dimension must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("RouteEndpoint name must not be null");
        }
    }

    /** The dimension identifier, e.g. {@code minecraft:overworld} or {@code nerospace:cindara}. */
    public Identifier id() {
        return this.dimension.identifier();
    }

    /**
     * The Nerospace planet this endpoint is, or empty for the Overworld ("Home") — the only endpoint
     * that is not a Nerospace planet.
     */
    public Optional<PlanetId> planet() {
        return NerospacePlanets.byDimension(this.dimension);
    }

    /** Whether this endpoint is the Overworld ("Home"). */
    public boolean isHome() {
        return this.dimension.equals(Level.OVERWORLD);
    }
}
