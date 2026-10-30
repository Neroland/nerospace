package za.co.neroland.nerospace.client.ui;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.registry.ModDimensions;

/**
 * Planet accent colours: one accent and one dark stripe colour per planet, HOME as the fallback.
 * The accent is used for card borders, rules and sky strips; the dark pair only for 1-px stripes, never text.
 */
public final class PlanetAccents {

    public static final int HOME = 0xFF4A90D9;
    public static final int HOME_DARK = 0xFF25487A;

    private PlanetAccents() {
    }

    /** The planet's accent (ARGB), HOME for the overworld or an unknown dimension. */
    public static int of(@Nullable ResourceKey<Level> dimension) {
        return pair(dimension)[0];
    }

    /** The planet's dark stripe colour (ARGB). */
    public static int dark(@Nullable ResourceKey<Level> dimension) {
        return pair(dimension)[1];
    }

    private static int[] pair(@Nullable ResourceKey<Level> d) {
        if (d == null) {
            return new int[] { HOME, HOME_DARK };
        }
        if (d.equals(ModDimensions.STATION_LEVEL)) {
            return new int[] { 0xFF2EC4B6, 0xFF17625B };
        }
        if (d.equals(ModDimensions.LUNA_LEVEL)) {
            return new int[] { 0xFFC9C9C2, 0xFF64645F };
        }
        if (d.equals(ModDimensions.GREENXERTZ_LEVEL)) {
            return new int[] { 0xFF3FBF6F, 0xFF1D4D2B };
        }
        if (d.equals(ModDimensions.MARS_LEVEL)) {
            return new int[] { 0xFFD9A066, 0xFF7A4A2E };
        }
        if (d.equals(ModDimensions.CINDARA_LEVEL)) {
            return new int[] { 0xFFFF6B35, 0xFFA3321C };
        }
        if (d.equals(ModDimensions.GLACIRA_LEVEL)) {
            return new int[] { 0xFF6CC2EF, 0xFF2D6F9E };
        }
        if (d.equals(ModDimensions.NYXARA_LEVEL)) {
            return new int[] { 0xFF9B5DE5, 0xFF5B2D8E };
        }
        return new int[] { HOME, HOME_DARK };
    }
}
