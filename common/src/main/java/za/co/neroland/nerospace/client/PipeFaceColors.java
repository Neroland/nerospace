package za.co.neroland.nerospace.client;

import net.minecraft.core.Direction;

/**
 * The shared per-face colour code for Universal Pipe configuration: the Pipe Configuration GUI
 * tints each face row with these colours, and {@link UniversalPipeRenderer} shades the matching
 * pipe faces in-world while the player holds the Configurator — so "the orange face" means the
 * same thing on screen and in the world.
 */
public final class PipeFaceColors {

    /** ARGB per {@link Direction#get3DDataValue()}: down, up, north, south, west, east.
     *  Six VIVID hues — every side must read as a colour in-world (white/grey wash out). */
    public static final int[] ARGB = {
            0xFF3C64F0, // down  — blue
            0xFFF0D032, // up    — yellow
            0xFFE04040, // north — signal red
            0xFF3CD46C, // south — green
            0xFFB050E8, // west  — nerosium purple
            0xFFF09028, // east  — amber orange
    };

    public static int of(Direction dir) {
        return ARGB[dir.get3DDataValue()];
    }

    private PipeFaceColors() {
    }
}
