package za.co.neroland.nerospace.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import za.co.neroland.nerospace.Nerospace;

/**
 * Greenling — "Sprout" (Phase 10d). A small grounded biped: chubby body, oversized cheeky head, a
 * leaf crest, little arms and two stubby legs. The legs toddle and the arms swing as it walks.
 * Idle (10f): a curious head sway (cheeks track the head) and its signature — the three-frond leaf
 * crest wiggles, each frond out of phase, like leaves in a light breeze.
 */
public class GreenlingModel extends GreenxertzMobModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "greenling"), "main");

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public GreenlingModel(ModelPart root) {
        super(root);
        swingLimb("leg_left", 0F, 0.5F);
        swingLimb("leg_right", Mth.PI, 0.5F);
        swingLimb("arm_left", Mth.PI, 0.3F);
        swingLimb("arm_right", 0F, 0.3F);
        // Idle: curious head sway (the cheek band tracks the head)…
        ambient("head", Direction.Axis.Y, 0.06F, 0F, 0.07F);
        ambient("cheeks", Direction.Axis.Y, 0.06F, 0F, 0.07F);
        // …and the signature leaf-crest wiggle: three fronds swaying out of phase.
        ambient("frond_mid", Direction.Axis.Z, 0.14F, 0F, 0.09F);
        ambient("frond_left", Direction.Axis.Z, 0.14F, 0.9F, 0.09F);
        ambient("frond_right", Direction.Axis.Z, 0.14F, 1.8F, 0.09F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 15F, -3F, 7F, 6F, 6F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("belly",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3F, 19F, -2.5F, 6F, 3F, 5F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 28).addBox(-4F, 7F, -4F, 8F, 8F, 8F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("cheeks",
                CubeListBuilder.create().texOffs(0, 28).addBox(-4.5F, 10F, -3.5F, 9F, 3F, 7F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("frond_mid",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -6F, -0.5F, 1F, 6F, 1F), PartPose.offset(0F, 7F, 0F));
        root.addOrReplaceChild("frond_left",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -5F, -0.5F, 1F, 5F, 1F),
                PartPose.offsetAndRotation(-1.5F, 7F, 0F, 0F, 0F, 0.5F));
        root.addOrReplaceChild("frond_right",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -5F, -0.5F, 1F, 5F, 1F),
                PartPose.offsetAndRotation(1.5F, 7F, 0F, 0F, 0F, -0.5F));

        // Hip/shoulder-pivoted limbs.
        root.addOrReplaceChild("arm_left",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1.5F, 0F, -1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(-3.5F, 15.5F, 0F, 0F, 0F, 0.15F));
        root.addOrReplaceChild("arm_right",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, 0F, -1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(3.5F, 15.5F, 0F, 0F, 0F, -0.15F));
        root.addOrReplaceChild("leg_left",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1.25F, 0F, -1.5F, 2.5F, 3F, 3F),
                PartPose.offset(-1.25F, 21F, 0F));
        root.addOrReplaceChild("leg_right",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1.25F, 0F, -1.5F, 2.5F, 3F, 3F),
                PartPose.offset(1.25F, 21F, 0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
