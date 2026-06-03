package za.co.neroland.nerospace.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Emissive glow layer for the Greenxertz/Cindara creatures: re-renders the model with a full-bright
 * {@code eyes} render type using a per-creature glow texture (transparent except the eyes / crystal /
 * ember accents), so those pixels glow in the dark regardless of light level.
 */
public class GlowEyesLayer extends EyesLayer<LivingEntityRenderState, EntityModel<LivingEntityRenderState>> {

    private final RenderType type;

    public GlowEyesLayer(RenderLayerParent<LivingEntityRenderState, EntityModel<LivingEntityRenderState>> parent,
                         Identifier glowTexture) {
        super(parent);
        this.type = RenderTypes.eyes(glowTexture);
    }

    @Override
    public RenderType renderType() {
        return this.type;
    }
}
