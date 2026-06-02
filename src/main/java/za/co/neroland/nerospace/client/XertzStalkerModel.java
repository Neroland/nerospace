package za.co.neroland.nerospace.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.Nerospace;

/**
 * Xertz Stalker model (Phase 10): a tall, lean predator — a narrow upright body, a head thrust
 * forward, four long legs and a trailing tail. Reuses the shared Greenxertz creature texture layout
 * ({@code body} at uv 0,0; {@code head} at 0,14; limbs at 28,0) so the existing 64x64 texture maps.
 */
public class XertzStalkerModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "xertz_stalker"), "main");

    public XertzStalkerModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3F, 6F, -2F, 6F, 10F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 14).addBox(-3F, 1F, -8F, 6F, 6F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_front_left",
                CubeListBuilder.create().texOffs(28, 0).addBox(-3F, 16F, -2F, 2F, 8F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_front_right",
                CubeListBuilder.create().texOffs(28, 0).addBox(1F, 16F, -2F, 2F, 8F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_back_left",
                CubeListBuilder.create().texOffs(28, 0).addBox(-3F, 16F, 2F, 2F, 8F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_back_right",
                CubeListBuilder.create().texOffs(28, 0).addBox(1F, 16F, 2F, 2F, 8F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(28, 0).addBox(-1F, 8F, 3F, 2F, 2F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
