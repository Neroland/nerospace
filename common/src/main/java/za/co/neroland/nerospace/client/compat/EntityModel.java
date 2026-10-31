package za.co.neroland.nerospace.client.compat;

// Minecraft 1.21.1 only (empty on every other line). Before 1.21.2 an entity model was posed from the entity
// (setupAnim(entity, limbSwing, ..)) and owned no root part. This look-alike (same simple name, swapped in by an import
// rewrite in stonecutter.gradle) keeps the 26.x shape - a root part and setupAnim(state) that starts from the baked
// pose - and still is a vanilla EntityModel, so vanilla's MobRenderer can draw it: the client.compat renderers hand it
// the frame's render state before vanilla asks it to pose itself.

//? if <1.21.2 {
/*import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;

public abstract class EntityModel<S extends EntityRenderState> extends net.minecraft.client.model.EntityModel<Entity> {

    private final ModelPart root;
    private S pending;
    private int tint = -1;

    protected EntityModel(ModelPart root) {
        this(root, RenderType::entityCutoutNoCull);
    }

    protected EntityModel(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
        super(renderType);
        this.root = root;
    }

    public final ModelPart root() {
        return this.root;
    }

    // As 1.21.2+: every frame starts from the baked pose.
    public void setupAnim(S state) {
        resetPose();
    }

    public final void resetPose() {
        this.root.getAllParts().forEach(ModelPart::resetPose);
    }

    // The render state the next vanilla setupAnim(entity, ..) call poses from.
    public void prepare(S state) {
        this.pending = state;
    }

    // Multiplied into the colour of the next draws (the 1.21.2+ MobRenderer.getModelTint); -1 for none.
    public void setTint(int tint) {
        this.tint = tint;
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        if (this.pending != null) {
            setupAnim(this.pending);
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
        this.root.render(poseStack, consumer, light, overlay,
                this.tint == -1 ? color : FastColor.ARGB32.multiply(color, this.tint));
    }
}
*///?}
