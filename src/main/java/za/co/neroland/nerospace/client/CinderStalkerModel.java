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
 * Cinder Stalker model (Phase 10): a bulky volcanic brute — a thick heavy body, a low blocky head
 * with two horns, on four stout legs. Reuses the shared Greenxertz creature texture layout.
 */
public class CinderStalkerModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "cinder_stalker"), "main");

    public CinderStalkerModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5F, 6F, -4F, 10F, 10F, 8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 14).addBox(-3F, 5F, -8F, 6F, 6F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("horn_left",
                CubeListBuilder.create().texOffs(28, 0).addBox(-3F, 2F, -7F, 2F, 3F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("horn_right",
                CubeListBuilder.create().texOffs(28, 0).addBox(1F, 2F, -7F, 2F, 3F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_front_left",
                CubeListBuilder.create().texOffs(28, 0).addBox(-5F, 16F, -4F, 3F, 8F, 3F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_front_right",
                CubeListBuilder.create().texOffs(28, 0).addBox(2F, 16F, -4F, 3F, 8F, 3F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_back_left",
                CubeListBuilder.create().texOffs(28, 0).addBox(-5F, 16F, 1F, 3F, 8F, 3F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_back_right",
                CubeListBuilder.create().texOffs(28, 0).addBox(2F, 16F, 1F, 3F, 8F, 3F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
