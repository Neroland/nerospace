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

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * A simple rocket model: a tall cylindrical body, a nose cone, and four tail fins. Built with the
 * 26.1 {@code LayerDefinition} mesh API; parts use the standard entity-model convention (feet at the
 * part origin, body drawn upward via negative Y) so the shared render transform stands it upright.
 *
 * <p>Cross-loader port note: the renderer bakes each tier's {@code createBodyLayer()} directly (the
 * Greenxertz-mob pattern), so no model-layer registry is needed on either loader.</p>
 */
public class RocketModel extends EntityModel<EntityRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "rocket"), "main");

    public RocketModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6F, -16F, -6F, 12F, 36F, 12F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(0, 56).addBox(-4F, -24F, -4F, 8F, 8F, 8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("console",
                CubeListBuilder.create().texOffs(44, 68).addBox(-4F, -1.5F, -5.5F, 8F, 5F, 1F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bell",
                CubeListBuilder.create().texOffs(0, 80).addBox(-4F, 20F, -4F, 8F, 3F, 8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("fin_north",
                CubeListBuilder.create().texOffs(64, 0).addBox(-1F, 14F, -10F, 2F, 10F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("fin_south",
                CubeListBuilder.create().texOffs(64, 0).addBox(-1F, 14F, 6F, 2F, 10F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("fin_west",
                CubeListBuilder.create().texOffs(64, 0).addBox(-10F, 14F, -1F, 4F, 10F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("fin_east",
                CubeListBuilder.create().texOffs(64, 0).addBox(6F, 14F, -1F, 4F, 10F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }
}
