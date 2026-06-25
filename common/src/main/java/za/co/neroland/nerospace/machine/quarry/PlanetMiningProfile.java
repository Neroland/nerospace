package za.co.neroland.nerospace.machine.quarry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.registry.ModDimensions;

/**
 * Per-planet mining characteristics. The dense, deep moons mine slower; the established planets mine at
 * the baseline. A pure data lookup keyed by dimension, with a default for anything unlisted.
 */
public record PlanetMiningProfile(double speedMultiplier, double bonusDropChance) {

    private static final PlanetMiningProfile DEFAULT = new PlanetMiningProfile(1.0D, 0.0D);
    private static final PlanetMiningProfile GREENXERTZ = new PlanetMiningProfile(1.0D, 0.0D);
    private static final PlanetMiningProfile CINDARA = new PlanetMiningProfile(0.8D, 0.0D);
    private static final PlanetMiningProfile GLACIRA = new PlanetMiningProfile(0.7D, 0.0D);

    public static PlanetMiningProfile forDimension(ResourceKey<Level> dimension) {
        if (dimension.equals(ModDimensions.GREENXERTZ_LEVEL)) {
            return GREENXERTZ;
        }
        if (dimension.equals(ModDimensions.CINDARA_LEVEL)) {
            return CINDARA;
        }
        if (dimension.equals(ModDimensions.GLACIRA_LEVEL)) {
            return GLACIRA;
        }
        return DEFAULT;
    }
}
