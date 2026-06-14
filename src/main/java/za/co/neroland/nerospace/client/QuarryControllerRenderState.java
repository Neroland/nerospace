package za.co.neroland.nerospace.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/**
 * Render state for the quarry's gantry + drill head. All coordinates are relative to the controller
 * block's corner (the BER's pose origin), computed server-synced in
 * {@link QuarryControllerRenderer#extractRenderState}.
 */
public class QuarryControllerRenderState extends BlockEntityRenderState {

    public boolean active;
    public boolean mining;
    /** Region edges (relative to the controller). */
    public double x0;
    public double x1;
    public double z0;
    public double z1;
    /** Top of the frame plane (relative). */
    public double topY;
    /** Smoothed drill-head centre (relative). */
    public double hx;
    public double hy;
    public double hz;
    /** Tier accent (ARGB) for the gantry rails. */
    public int accent;
    /** The drill head, rendered as a spinning item model. */
    public final ItemStackRenderState head = new ItemStackRenderState();
    /** Drill-head spin (degrees). */
    public float headSpin;
}
