package za.co.neroland.nerospace.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;

/** Render state for the rocket: per-tier visual scale + texture (cockpit rework). */
public class RocketRenderState extends EntityRenderState {

    /** Per-tier hull scale (see {@code RocketEntity.visualScale}). */
    public float scale = 1.6F;
    /** Per-tier hull texture. */
    public @org.jspecify.annotations.NonNull Identifier texture =
            NerospaceCommon.id("textures/entity/rocket_t1.png");
    /** Tier ordinal — picks the per-tier geometry. */
    public int tier;
}
