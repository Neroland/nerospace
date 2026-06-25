package za.co.neroland.nerospace.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

import org.jspecify.annotations.NonNull;

/** Render state for the Star Guide pedestal hologram: the floating next-step icon + animation. */
public class StarGuideHologramRenderState extends BlockEntityRenderState {

    /** Whether the pedestal is loaded (hologram visible). */
    public boolean visible;
    /** Y-spin in degrees. */
    public float spin;
    /** Vertical bob offset (blocks). */
    public float bob;
    /** The hologram icon's pooled item render state. */
    public final @NonNull ItemStackRenderState renderState = new ItemStackRenderState();
}
