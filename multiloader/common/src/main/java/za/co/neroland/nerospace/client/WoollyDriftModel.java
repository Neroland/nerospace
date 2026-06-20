package za.co.neroland.nerospace.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Woolly Drift (DEEPER_TERRAFORM_DESIGN.md §5) — the shaggy sheep-analogue of terraformed Glacira:
 * a big rounded fleece block on stubby legs, ridged with wind-packed snow tufts, a small bare face
 * and drooped ears. Idle: slow huddled breathing, ear twitches and a gentle ripple through the
 * fleece tufts like wind over a snowdrift.
 */
public class WoollyDriftModel extends GreenxertzMobModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "woolly_drift"), "main");

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public WoollyDriftModel(ModelPart root) {
        super(root);
        // A short-strided trundle on stubby legs.
        swingLimb("leg_fl", 0F, 0.5F);
        swingLimb("leg_br", 0F, 0.5F);
        swingLimb("leg_fr", Mth.PI, 0.5F);
        swingLimb("leg_bl", Mth.PI, 0.5F);
        // Slow, huddled-in-the-cold breathing.
        breathing(0.06F, 0.55F);
        // Ear twitches and the wind-ripple through the fleece tufts (staggered phases).
        ambient("ear_l", Direction.Axis.Z, 0.13F, 0F, 0.08F);
        ambient("ear_r", Direction.Axis.Z, 0.13F, 1.4F, 0.08F);
        ambient("tuft_0", Direction.Axis.Z, 0.08F, 0.0F, 0.04F);
        ambient("tuft_1", Direction.Axis.Z, 0.08F, 1.5F, 0.04F);
        ambient("tuft_2", Direction.Axis.Z, 0.08F, 3.0F, 0.04F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // model_sync:begin
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5F, 8F, -7F, 10F, 9F, 14F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("tuft_0",
                CubeListBuilder.create().texOffs(44, 0).addBox(-3F, 8F, -5.5F, 6F, 2F, 3F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("tuft_1",
                CubeListBuilder.create().texOffs(44, 0).addBox(-3F, 8F, -1.5F, 6F, 2F, 3F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("tuft_2",
                CubeListBuilder.create().texOffs(44, 0).addBox(-3F, 8F, 2.5F, 6F, 2F, 3F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 28).addBox(-2.5F, 7F, -11F, 5F, 5F, 5F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("ear_l",
                CubeListBuilder.create().texOffs(44, 0).addBox(-4F, 8F, -9F, 1.5F, 3F, 1F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("ear_r",
                CubeListBuilder.create().texOffs(44, 0).addBox(2.5F, 8F, -9F, 1.5F, 3F, 1F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_fl",
                CubeListBuilder.create().texOffs(44, 0).addBox(-4F, 17F, -5.5F, 2F, 7F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_fr",
                CubeListBuilder.create().texOffs(44, 0).addBox(2F, 17F, -5.5F, 2F, 7F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_bl",
                CubeListBuilder.create().texOffs(44, 0).addBox(-4F, 17F, 3.5F, 2F, 7F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_br",
                CubeListBuilder.create().texOffs(44, 0).addBox(2F, 17F, 3.5F, 2F, 7F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // model_sync:end

        return LayerDefinition.create(mesh, 64, 64);
    }
}
