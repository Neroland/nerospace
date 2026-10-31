package za.co.neroland.nerospace.client.compat;

// Minecraft 1.21.1 only (empty on every other line). The 1.21.2+ MobRenderer<T, S, M> extracts a render state and
// draws the model from it. This look-alike (same simple name, swapped in by an import rewrite in stonecutter.gradle)
// keeps that shape on 1.21.1: each frame it extracts the state, hands it to the client.compat EntityModel and lets
// vanilla's 1.21.1 MobRenderer draw as usual (shadow, hurt tint, death roll, leash, name tag), routing scale(..),
// the texture and the model tint through the state-based overrides and drawing the state layers on top.

//? if <1.21.2 {
/*import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

public abstract class MobRenderer<T extends Mob, S extends LivingEntityRenderState, M extends EntityModel<S>>
        extends net.minecraft.client.renderer.entity.MobRenderer<T, net.minecraft.client.model.EntityModel<T>>
        implements RenderLayerParent<S, M> {

    private final M stateModel;
    private final List<EyesLayer<S, ?>> stateLayers = new ArrayList<>();
    private S state;

    @SuppressWarnings({"unchecked", "this-escape"})
    protected MobRenderer(EntityRendererProvider.Context context, M model, float shadowRadius) {
        // The look-alike model is a vanilla EntityModel<Entity>; vanilla only ever hands it the entity being drawn.
        super(context, (net.minecraft.client.model.EntityModel<T>) (Object) model, shadowRadius);
        this.stateModel = model;
        super.addLayer(new RenderLayer<T, net.minecraft.client.model.EntityModel<T>>(this) {
            @Override
            public void render(PoseStack poseStack, MultiBufferSource buffers, int light, T entity, float limbSwing,
                    float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                MobRenderer.this.stateModel.setTint(-1);
                for (EyesLayer<S, ?> layer : MobRenderer.this.stateLayers) {
                    layer.render(poseStack, buffers, light, MobRenderer.this.state, netHeadYaw, headPitch);
                }
            }
        });
    }

    @Override
    public M stateModel() {
        return this.stateModel;
    }

    public void addLayer(EyesLayer<S, ?> layer) {
        this.stateLayers.add(layer);
    }

    public abstract S createRenderState();

    public void extractRenderState(T entity, S state, float partialTick) {
        LivingEntityRenderState.extractLiving(entity, state, partialTick);
    }

    protected void scale(S state, PoseStack poseStack) {
    }

    public abstract ResourceLocation getTextureLocation(S state);

    protected int getModelTint(S state) {
        return -1;
    }

    private S stateFor(T entity, float partialTick) {
        if (this.state == null) {
            this.state = createRenderState();
        }
        extractRenderState(entity, this.state, partialTick);
        return this.state;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
            int light) {
        S current = stateFor(entity, partialTick);
        current.lightCoords = light;
        this.stateModel.prepare(current);
        this.stateModel.setTint(getModelTint(current));
        super.render(entity, entityYaw, partialTick, poseStack, buffers, light);
        this.stateModel.setTint(-1);
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        scale(this.state != null ? this.state : stateFor(entity, partialTick), poseStack);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return getTextureLocation(this.state != null ? this.state : stateFor(entity, 1.0F));
    }
}
*///?}
