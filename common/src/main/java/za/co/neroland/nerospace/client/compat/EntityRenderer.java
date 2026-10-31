package za.co.neroland.nerospace.client.compat;

// Minecraft 1.21.2 - 1.21.8 only (empty on every other line). Before 1.21.9 an entity renderer drew its render
// state immediately in render(state, poseStack, buffers, light); 1.21.9 replaced that with submit(.., collector,
// camera). This base (same simple name, swapped in by an import rewrite in stonecutter.gradle on those lines)
// declares the 26.x submit(..) and routes the old render(..) call into it, so the renderers compile unchanged.
// super.submit(..) draws what vanilla's render(..) drew (the name tag).

//? if <1.21.9 {
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
*///?}
