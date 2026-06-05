package za.co.neroland.nerospace.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.Nerospace;

/**
 * A simple rocket model (Phase 4 follow-up): a tall cylindrical body, a nose cone, and four tail fins.
 * Built with the 26.1 {@code LayerDefinition} mesh API; parts use the standard entity-model convention
 * (feet at the part origin, body drawn upward via negative Y) so the shared render transform stands it
 * upright. Proportions are a first pass — fine-tune in {@code runClient}.
 */
public class RocketModel extends EntityModel<EntityRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "rocket"), "main");

    public RocketModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // model_sync:begin
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6F, -16F, -6F, 12F, 36F, 12F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(0, 48).addBox(-4F, -24F, -4F, 8F, 8F, 8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("console",
                CubeListBuilder.create().texOffs(32, 48).addBox(-4F, -1.5F, -5.5F, 8F, 5F, 1F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("fin_north",
                CubeListBuilder.create().texOffs(48, 0).addBox(-1F, 14F, -10F, 2F, 10F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("fin_south",
                CubeListBuilder.create().texOffs(48, 0).addBox(-1F, 14F, 6F, 2F, 10F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("fin_west",
                CubeListBuilder.create().texOffs(48, 0).addBox(-10F, 14F, -1F, 4F, 10F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("fin_east",
                CubeListBuilder.create().texOffs(48, 0).addBox(6F, 14F, -1F, 4F, 10F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // model_sync:end


        return LayerDefinition.create(mesh, 64, 64);
    }
}
