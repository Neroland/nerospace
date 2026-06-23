package za.co.neroland.nerospace.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Render state for the falling meteor: an age value used to spin the rock. */
public class FallingMeteorRenderState extends EntityRenderState {

    /** Entity age (ticks + partial) — drives the tumble rotation. */
    public float ticks;
}
