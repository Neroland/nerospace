package za.co.neroland.nerospace.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Stable, immutable identity of a Nerospace planet / orbital body.
 *
 * <p><b>Public API — semver-stable.</b> Part of {@code za.co.neroland.nerospace.api}, the only supported
 * surface for other Neroland mods. Wraps a dimension {@link Identifier} (e.g. {@code nerospace:greenxertz})
 * and maps to/from the matching {@link ResourceKey}{@code <Level>}. Obtain instances from
 * {@link NerospacePlanets} — never hard-code the internal {@code registry.ModDimensions} constants.</p>
 *
 * @param id the dimension identifier this planet lives in (namespace {@code nerospace})
 */
public record PlanetId(Identifier id) {

    public PlanetId {
        if (id == null) {
            throw new IllegalArgumentException("PlanetId id must not be null");
        }
    }

    /** The dimension {@link ResourceKey} this planet maps to (interned — {@code ==}/equals safe). */
    public ResourceKey<Level> dimension() {
        return ResourceKey.create(Registries.DIMENSION, this.id);
    }

    /** The canonical string form, e.g. {@code "nerospace:greenxertz"}. */
    public String asString() {
        return this.id.toString();
    }

    @Override
    public String toString() {
        return asString();
    }
}
