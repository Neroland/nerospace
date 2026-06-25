package za.co.neroland.nerospace.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Emissive glow layer for the Greenxertz/Cindara creatures: re-renders the model with a full-bright
 * {@code eyes} render type using a per-creature glow texture (transparent except the eyes / crystal /
 * ember accents), so those pixels glow in the dark regardless of light level.
 */
public class GlowEyesLayer<S extends LivingEntityRenderState> extends EyesLayer<S, EntityModel<S>> {

    private final @NonNull RenderType type;

    public GlowEyesLayer(RenderLayerParent<S, EntityModel<S>> parent,
                         @NonNull Identifier glowTexture) {
        super(parent);
        this.type = NerospaceCommon.requireNonNull(RenderTypes.eyes(glowTexture));
    }

    @Override
    public @NonNull RenderType renderType() {
        return this.type;
    }
}
