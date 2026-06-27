package za.co.neroland.nerospace.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Render state for the Launch Controller's glowing console + holographic pad preview. */
public class LaunchControllerRenderState extends BlockEntityRenderState {

    public boolean visible;
    /** The block's horizontal facing — the glowing screen is rotated to match. */
    public Direction facing = Direction.NORTH;
    /** Each ghost cell as an offset from the controller plus block TYPE: {@code [dx, dy, dz, type]}. */
    public final List<int[]> cells = new ArrayList<>();
}
