package za.co.neroland.nerospace.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

import za.co.neroland.nerospace.rocket.RocketEntity;

/**
 * Entity renderer for the rocket (Phase 4).
 *
 * <p>26.1 reworked entity rendering around render-state submission ({@code SubmitNodeCollector},
 * {@code CameraRenderState}). This first pass registers a stable renderer (base render state, no
 * custom geometry) so the entity is interactable and ascent particles read clearly, while the
 * detailed 3D rocket model + launch animation are layered on in a follow-up that can be validated
 * live in {@code runClient}.</p>
 */
public class RocketRenderer extends EntityRenderer<RocketEntity, EntityRenderState> {

    public RocketRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
