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
 * A simple shared quadruped model for the Phase 5 Greenxertz creatures (body + head + four legs).
 * The three creatures reuse this single mesh with different textures; bespoke per-creature models are
 * a later art pass. Built with the 26.1 {@code LayerDefinition} mesh API.
 */
public class GreenxertzCreatureModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "greenxertz_creature"), "main");

    public GreenxertzCreatureModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // model_sync:begin
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, 8F, -3F, 8F, 8F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 14).addBox(-3F, 6F, -7F, 6F, 6F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_front_left",
                CubeListBuilder.create().texOffs(28, 0).addBox(-4F, 16F, -3F, 2F, 8F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_front_right",
                CubeListBuilder.create().texOffs(28, 0).addBox(2F, 16F, -3F, 2F, 8F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_back_left",
                CubeListBuilder.create().texOffs(28, 0).addBox(-4F, 16F, 1F, 2F, 8F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_back_right",
                CubeListBuilder.create().texOffs(28, 0).addBox(2F, 16F, 1F, 2F, 8F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // model_sync:end


        return LayerDefinition.create(mesh, 64, 64);
    }
}
