package za.co.neroland.nerospace.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/**
 * Render state for a single solar panel: the tilt angle of its deck (sun-tracking by day, folded flat
 * at night), its tier (selects the surface texture), and its footprint + whether this cell is the
 * anchor (so a multiblock draws one big deck only on its min-corner cell).
 *
 * <p>Cross-loader port: identical to the standalone mod minus the per-face connector stubs (dropped for
 * the cross-loader slice — they needed client-side energy-cap queries).</p>
 */
public class SolarPanelRenderState extends BlockEntityRenderState {

    /** Surface tilt in degrees about the east-west (Z) axis: 0 = flat (folded / noon), ±tilt by day. */
    public float angle;

    /** 1-based tier (selects the surface texture). */
    public int tier = 1;

    /** Footprint edge length (1 = T1 single tracker, >1 = N×N multiblock — one big deck on a mast). */
    public int footprint = 1;

    /** Whether this cell is its unit's anchor — only the anchor draws the deck. */
    public boolean anchor = true;
}
