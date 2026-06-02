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
 * Greenling — "Sprout" (Phase 10c). A small, grounded, friendly alien critter: a rounded body, an
 * oversized cheeky head with two big eyes, little arms, two stubby legs planted on the ground, and a
 * three-frond leaf crest. Built from overlapping cubes so it reads as one chubby creature, standing
 * (feet at y=24). UV atlas: body uv (0,0), head/face (0,28), limbs/fronds (44,0).
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

        // Rounded body (two stacked cubes for a chubby shape).
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 15F, -3F, 7F, 6F, 6F),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("belly",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3F, 19F, -2.5F, 6F, 3F, 5F),
                PartPose.offset(0F, 0F, 0F));

        // Big head (with a slightly wider mid-band for roundness).
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 28).addBox(-4F, 7F, -4F, 8F, 8F, 8F),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("cheeks",
                CubeListBuilder.create().texOffs(0, 28).addBox(-4.5F, 10F, -3.5F, 9F, 3F, 7F),
                PartPose.offset(0F, 0F, 0F));

        // Three-frond leaf crest on the crown (connected to the head top).
        root.addOrReplaceChild("frond_mid",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -6F, -0.5F, 1F, 6F, 1F),
                PartPose.offset(0F, 7F, 0F));
        root.addOrReplaceChild("frond_left",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -5F, -0.5F, 1F, 5F, 1F),
                PartPose.offsetAndRotation(-1.5F, 7F, 0F, 0F, 0F, 0.5F));
        root.addOrReplaceChild("frond_right",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -5F, -0.5F, 1F, 5F, 1F),
                PartPose.offsetAndRotation(1.5F, 7F, 0F, 0F, 0F, -0.5F));

        // Little arms hanging at the sides (connect to the body).
        root.addOrReplaceChild("arm_left",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1.5F, 0F, -1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(-3.5F, 15.5F, 0F, 0F, 0F, 0.15F));
        root.addOrReplaceChild("arm_right",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, 0F, -1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(3.5F, 15.5F, 0F, 0F, 0F, -0.15F));

        // Two stubby legs (feet at y=24).
        root.addOrReplaceChild("leg_left",
                CubeListBuilder.create().texOffs(44, 0).addBox(-2.5F, 21F, -1.5F, 2.5F, 3F, 3F),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("leg_right",
                CubeListBuilder.create().texOffs(44, 0).addBox(0F, 21F, -1.5F, 2.5F, 3F, 3F),
                PartPose.offset(0F, 0F, 0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
