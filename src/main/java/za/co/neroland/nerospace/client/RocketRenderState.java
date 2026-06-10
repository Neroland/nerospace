package za.co.neroland.nerospace.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.Nerospace;

/** Render state for the rocket: per-tier visual scale + texture (cockpit rework). */
public class RocketRenderState extends EntityRenderState {

    /** Per-tier hull scale (see {@code RocketEntity.visualScale}). */
    public float scale = 1.6F;
    /** Per-tier hull texture. */
    public Identifier texture =
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/entity/rocket_t1.png");
    /** Tier ordinal — picks the per-tier geometry (ART_OVERHAUL_DESIGN.md §4.2). */
    public int tier;
}
