package za.co.neroland.nerospace.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Render state for the Launch Controller's holographic pad preview. */
public class LaunchControllerRenderState extends BlockEntityRenderState {

    public boolean visible;
    /** Each ghost cell as an offset from the controller plus colour: {@code [dx, dy, dz, argb]}. */
    public final List<int[]> cells = new ArrayList<>();
}
