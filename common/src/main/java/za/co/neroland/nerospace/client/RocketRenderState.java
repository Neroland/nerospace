package za.co.neroland.nerospace.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;

/** Render state for the rocket: per-tier presentation plus the client-side animation inputs. */
public class RocketRenderState extends EntityRenderState {

    /** Per-tier hull scale (see {@code RocketEntity.visualScale}). */
    public float scale = 1.5F;
    /** Per-tier texture sheet. */
    public Identifier texture =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/entity/rocket_t1.png");
    /** Tier ordinal — picks the per-tier geometry. */
    public int tier;
    /** Whether the ascent is running (engines lit). */
    public boolean launching;
    /** Ascent progress 0..1 (client-side counter, partial-tick smoothed). */
    public float launch;
    /** Ignition ramp 0..1 over the first ticks of the ascent — drives plume growth and the hull shake. */
    public float ignition;
    /** Hatch opening 0 (sealed) .. 1 (open), eased client-side. */
    public float hatch;
    /** An uncrewed cargo hull: the boarding ladder is not drawn. */
    public boolean cargo;
}
