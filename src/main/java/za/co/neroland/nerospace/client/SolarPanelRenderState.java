package za.co.neroland.nerospace.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/**
 * Render state for a single solar panel: the tilt angle of its deck (sun-tracking by day, folded flat
 * at night) plus which horizontal faces have a power hookup (so a connector stub is drawn there).
 */
public class SolarPanelRenderState extends BlockEntityRenderState {

    /** Surface tilt in degrees about the east-west (Z) axis: 0 = flat (folded / noon), +-tilt by day. */
    public float angle;

    /** Energy hookup (cable/machine, any mod) present on a face, indexed N=0, E=1, S=2, W=3. */
    public final boolean[] connector = new boolean[4];

    /** 1-based tier (selects the surface texture). */
    public int tier = 1;

    /** Footprint edge length (1 = T1 single tracker, >1 = N×N multiblock — one big deck on a mast). */
    public int footprint = 1;

    /** Whether this cell is its unit's anchor — only the anchor draws the deck. */
    public boolean anchor = true;
}
