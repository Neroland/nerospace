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
 * Meadow Loper (DEEPER_TERRAFORM_DESIGN.md §5) — the placid cow-analogue grazer: a deep barrel body
 * on four sturdy legs, a broad low-held head with a wide muzzle and small horn nubs, and a lazy
 * swishing tail. Silhouette: heavy and horizontal, nothing like the existing predators. Idle: slow,
 * deep grazing breaths with the head dipping toward the grass and the tail swatting.
 */
public class MeadowLoperModel extends GreenxertzMobModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "meadow_loper"), "main");

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public MeadowLoperModel(ModelPart root) {
        super(root);
        // A steady quadruped amble.
        swingLimb("leg_fl", 0F, 0.55F);
        swingLimb("leg_br", 0F, 0.55F);
        swingLimb("leg_fr", Mth.PI, 0.55F);
        swingLimb("leg_bl", Mth.PI, 0.55F);
        // Slow, deep grazer breathing.
        breathing(0.05F, 0.7F);
        // Head dips toward the grass; the muzzle follows.
        ambient("head", Direction.Axis.X, 0.045F, 0F, 0.08F);
        ambient("muzzle", Direction.Axis.X, 0.045F, 0F, 0.08F);
        // Lazy tail swat.
        ambient("tail", Direction.Axis.Y, 0.09F, 0.8F, 0.18F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // model_sync:begin
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5F, 8F, -8F, 10F, 9F, 16F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 28).addBox(-3F, 6F, -13F, 6F, 6F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("muzzle",
                CubeListBuilder.create().texOffs(0, 28).addBox(-2F, 9F, -16F, 4F, 3F, 3F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("horn_l",
                CubeListBuilder.create().texOffs(44, 0).addBox(-4F, 4F, -11F, 1F, 2F, 1F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("horn_r",
                CubeListBuilder.create().texOffs(44, 0).addBox(3F, 4F, -11F, 1F, 2F, 1F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, 9F, 8F, 1F, 7F, 1F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_fl",
                CubeListBuilder.create().texOffs(44, 0).addBox(-5F, 17F, -7F, 3F, 7F, 3F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_fr",
                CubeListBuilder.create().texOffs(44, 0).addBox(2F, 17F, -7F, 3F, 7F, 3F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_bl",
                CubeListBuilder.create().texOffs(44, 0).addBox(-5F, 17F, 4F, 3F, 7F, 3F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_br",
                CubeListBuilder.create().texOffs(44, 0).addBox(2F, 17F, 4F, 3F, 7F, 3F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // model_sync:end

        return LayerDefinition.create(mesh, 64, 64);
    }
}
