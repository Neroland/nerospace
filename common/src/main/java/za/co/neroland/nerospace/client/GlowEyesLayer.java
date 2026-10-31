package za.co.neroland.nerospace.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Emissive glow layer for the creatures and villagers: re-renders the model with a full-bright {@code eyes} render
 * type using a glow texture (transparent except the eyes / crystal / ember accents), so those pixels glow in the dark
 * regardless of light level. The glow texture is either fixed or resolved per render state (the villager cultures each
 * carry their own markings).
 */
public class GlowEyesLayer<S extends LivingEntityRenderState> extends EyesLayer<S, EntityModel<S>> {

    private final Function<S, Identifier> glowTexture;
    private final Map<Identifier, RenderType> types = new HashMap<>();
    /** The type for the state being submitted (rendering is single-threaded; set just before the parent submits). */
    private RenderType current;

    public GlowEyesLayer(RenderLayerParent<S, EntityModel<S>> parent, Identifier glowTexture) {
        this(parent, state -> glowTexture);
    }

    public GlowEyesLayer(RenderLayerParent<S, EntityModel<S>> parent, Function<S, Identifier> glowTexture) {
        super(parent);
        this.glowTexture = glowTexture;
    }

    //? if >=1.21.9 {
    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state,
            float yRot, float xRot) {
        this.current = this.types.computeIfAbsent(this.glowTexture.apply(state), RenderTypes::eyes);
        super.submit(poseStack, submitNodeCollector, lightCoords, state, yRot, xRot);
    }
    //?} else {
    /*@Override
    public void render(PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffers, int lightCoords,
            S state, float yRot, float xRot) {
        this.current = this.types.computeIfAbsent(this.glowTexture.apply(state), RenderTypes::eyes);
        super.render(poseStack, buffers, lightCoords, state, yRot, xRot);
    }
    *///?}

    @Override
    public RenderType renderType() {
        return this.current;
    }
}
