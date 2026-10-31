package za.co.neroland.nerospace.client.compat;

// Minecraft 1.21.2 - 1.21.8 only (empty on every other line). Before 1.21.9 an entity renderer drew its render
// state immediately in render(state, poseStack, buffers, light); 1.21.9 replaced that with submit(.., collector,
// camera). This base (same simple name, swapped in by an import rewrite in stonecutter.gradle on those lines)
// declares the 26.x submit(..) and routes the old render(..) call into it, so the renderers compile unchanged.
// super.submit(..) draws what vanilla's render(..) drew (the name tag).

//? if >=1.21.2 && <1.21.9 {
/*import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

public abstract class EntityRenderer<T extends Entity, S extends EntityRenderState>
        extends net.minecraft.client.renderer.entity.EntityRenderer<T, S> {

    private int light;

    protected EntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(S state, PoseStack poseStack, MultiBufferSource buffers, int light) {
        this.light = light;
        submit(state, poseStack, new SubmitNodeCollector(buffers), new CameraRenderState());
    }

    // The packed light of the frame being drawn (1.21.9+ carries it on the render state as lightCoords).
    protected int light() {
        return this.light;
    }

    public void submit(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.render(state, poseStack, collector.buffers(), this.light);
    }
}
*///?} else if <1.21.2 {
/*// Minecraft 1.21.1: there is no render state either, so the base also extracts one from the entity (the 26.x
// extractRenderState(..) shape) before submit(..), and routes the shadow radius and culling box through the same
// state-based / entity-based overrides 1.21.2+ has. super.submit(..) draws what vanilla's render(..) drew (the name tag).
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public abstract class EntityRenderer<T extends Entity, S extends EntityRenderState>
        extends net.minecraft.client.renderer.entity.EntityRenderer<T> {

    private int light;
    private S state;
    private T entity;
    private float entityYaw;
    private float partialTick;

    protected EntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public abstract S createRenderState();

    public void extractRenderState(T entity, S state, float partialTick) {
        EntityRenderState.extractBase(entity, state, partialTick);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
            int light) {
        if (this.state == null) {
            this.state = createRenderState();
        }
        this.entity = entity;
        this.entityYaw = entityYaw;
        this.partialTick = partialTick;
        this.light = light;
        extractRenderState(entity, this.state, partialTick);
        this.state.lightCoords = light;
        submit(this.state, poseStack, new SubmitNodeCollector(buffers), new CameraRenderState());
    }

    // The packed light of the frame being drawn (1.21.9+ carries it on the render state as lightCoords).
    protected int light() {
        return this.light;
    }

    public void submit(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.render(this.entity, this.entityYaw, this.partialTick, poseStack, collector.buffers(), this.light);
    }

    protected float getShadowRadius(S state) {
        return this.shadowRadius;
    }

    @Override
    protected float getShadowRadius(T entity) {
        return this.state != null ? getShadowRadius(this.state) : this.shadowRadius;
    }

    protected AABB getBoundingBoxForCulling(T entity) {
        return entity.getBoundingBoxForCulling();
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double camX, double camY, double camZ) {
        if (!entity.shouldRender(camX, camY, camZ)) {
            return false;
        }
        if (entity.noCulling) {
            return true;
        }
        AABB box = getBoundingBoxForCulling(entity).inflate(0.5);
        if (box.hasNaN() || box.getSize() == 0.0) {
            box = new AABB(entity.getX() - 2.0, entity.getY() - 3.0, entity.getZ() - 2.0,
                    entity.getX() + 2.0, entity.getY() + 2.0, entity.getZ() + 2.0);
        }
        return frustum.isVisible(box);
    }

    // Vanilla 1.21.1 requires an entity texture; these renderers pick theirs per pass while drawing.
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
*///?}
