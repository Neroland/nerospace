package za.co.neroland.nerospace.client.compat;

// Minecraft 1.21.1 only (empty on every other line): the emissive "eyes" layer look-alike for a client.compat
// MobRenderer. It redraws the already-posed model full-bright with the layer's render type, as vanilla's EyesLayer does.

//? if <1.21.2 {
/*import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

public abstract class EyesLayer<S extends LivingEntityRenderState, M extends EntityModel<S>> {

    private static final int FULL_BRIGHT = 0xF00000;

    private final RenderLayerParent<S, M> parent;

    protected EyesLayer(RenderLayerParent<S, M> parent) {
        this.parent = parent;
    }

    public abstract RenderType renderType();

    public void render(PoseStack poseStack, MultiBufferSource buffers, int lightCoords, S state, float yRot,
            float xRot) {
        M model = this.parent.stateModel();
        model.renderToBuffer(poseStack, buffers.getBuffer(renderType()), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
    }
}
*///?}
