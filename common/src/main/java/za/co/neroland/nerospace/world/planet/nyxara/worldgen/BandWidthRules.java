package za.co.neroland.nerospace.world.planet.nyxara.worldgen;

/**
 * Which band width a Nyxara world uses (pure). A stored value wins (clamped to the config range); with nothing stored
 * (first load of the level) the current config value is used and then stored. So a config change applies to new
 * worlds only and never reshapes an existing one.
 */
public final class BandWidthRules {

    private BandWidthRules() {
    }

    /** @param stored the saved width, {@code <= 0} if none; @param config {@code nyxara.twilightBandWidth} */
    public static int resolve(int stored, int config) {
        return FixedXBands.clampWidth(stored > 0 ? stored : config);
    }
}
