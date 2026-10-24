package za.co.neroland.nerospace.machine.quarry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;

/**
 * Per-planet mining characteristics. The dense, deep moons mine slower; the established planets mine at
 * the baseline. A pure lookup of the planet definition's quarry speed, with a default for anything unlisted.
 */
public record PlanetMiningProfile(double speedMultiplier, double bonusDropChance) {

    private static final PlanetMiningProfile DEFAULT = new PlanetMiningProfile(1.0D, 0.0D);

    public static PlanetMiningProfile forDimension(ResourceKey<Level> dimension) {
        PlanetDefinition planet = Planets.get(dimension);
        if (planet == null || planet.miningSpeed() == 1.0D) {
            return DEFAULT;
        }
        return new PlanetMiningProfile(planet.miningSpeed(), 0.0D);
    }
}
