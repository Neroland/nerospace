package za.co.neroland.nerospace.client;

import net.minecraft.core.Direction;

/**
 * The shared per-face colour code for Universal Pipe configuration: the Pipe Configuration GUI
 * tints each face row with these colours, and {@link UniversalPipeRenderer} shades the matching
 * pipe faces in-world while the player holds the Configurator — so "the orange face" means the
 * same thing on screen and in the world.
 */
public final class PipeFaceColors {

    /** ARGB per {@link Direction#get3DDataValue()}: down, up, north, south, west, east. */
    public static final int[] ARGB = {
            0xFF8A97A8, // down  — slate
            0xFFF0F0F0, // up    — white
            0xFFE04848, // north — signal red
            0xFF50D878, // south — green
            0xFFB05AE0, // west  — nerosium purple
            0xFFF0A030, // east  — amber
    };

    public static int of(Direction dir) {
        return ARGB[dir.get3DDataValue()];
    }

    private PipeFaceColors() {
    }
}
