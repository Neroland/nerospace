package za.co.neroland.nerospace.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/**
 * Render state for a single solar panel: the blended tilt angle of its surface (sun-tracking by day,
 * folded flat at night) plus which horizontal neighbours are same-tier panels, so an array of them
 * reads as one continuous, seam-joined surface.
 */
public class SolarPanelRenderState extends BlockEntityRenderState {

    /** Surface tilt in degrees about the east-west (Z) axis: 0 = flat (folded / noon), +-tilt by day. */
    public float angle;

    /** Same-tier neighbour present, indexed N=0, E=1, S=2, W=3 (drives edge-to-edge seam joining). */
    public final boolean[] connect = new boolean[4];

    /** 1-based tier (selects the surface texture). */
    public int tier = 1;
}
