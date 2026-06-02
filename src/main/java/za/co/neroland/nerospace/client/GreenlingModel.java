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
 * Greenling model (Phase 10): a small, timid critter — a little body dominated by an oversized head,
 * on two stubby legs. Reuses the shared Greenxertz creature texture layout.
 */
public class GreenlingModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "greenling"), "main");

    public GreenlingModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, 16F, -2.5F, 5F, 5F, 5F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 14).addBox(-3F, 10F, -4F, 6F, 6F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_left",
                CubeListBuilder.create().texOffs(28, 0).addBox(-2F, 21F, -1F, 2F, 3F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_right",
                CubeListBuilder.create().texOffs(28, 0).addBox(0F, 21F, -1F, 2F, 3F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
