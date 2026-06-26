package za.co.neroland.nerospace.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Render state for the launch gantry tower: how far it has reclined and which way its arm points. */
public class LaunchGantryRenderState extends BlockEntityRenderState {

    /** Signed lean in degrees (0 = upright); applied about {@link #axisX}'s axis. */
    public float lean;
    /** {@code true} → recline about the X axis (pad to the north/south); {@code false} → about Z. */
    public boolean axisX;
    /** Cardinal direction from the tower toward the pad (the service arm reaches this way). */
    public int armDx = 1;
    public int armDz = 0;
    /** Whether a rocket is docked and clamped (arm engaged) vs. released. */
    public boolean attached;
}
