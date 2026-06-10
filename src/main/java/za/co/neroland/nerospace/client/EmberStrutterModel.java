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
 * Ember Strutter (DEEPER_TERRAFORM_DESIGN.md §5) — the skittish chicken-analogue of terraformed
 * Cindara: a plump little body on two quick legs, an upright neck with a small combed head and
 * beak, stubby wing slabs and a raked tail fan. Idle: rapid bird breathing, sharp pecky head bobs
 * and nervous wing flicks.
 */
public class EmberStrutterModel extends GreenxertzMobModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "ember_strutter"), "main");

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public EmberStrutterModel(ModelPart root) {
        super(root);
        // Quick alternating two-leg strut.
        swingLimb("leg_l", 0F, 0.7F);
        swingLimb("leg_r", Mth.PI, 0.7F);
        // Rapid little bird breaths.
        breathing(0.18F, 0.25F);
        // Sharp pecky head bobs (neck and head together) and nervous wing flicks.
        ambient("head", Direction.Axis.X, 0.16F, 0F, 0.1F);
        ambient("neck", Direction.Axis.X, 0.16F, 0F, 0.07F);
        ambient("wing_l", Direction.Axis.Z, 0.14F, 0.5F, 0.06F);
        ambient("wing_r", Direction.Axis.Z, 0.14F, 2.1F, 0.06F);
        ambient("tail_fan", Direction.Axis.X, 0.1F, 1.0F, 0.05F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // model_sync:begin
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3F, 14F, -4F, 6F, 6F, 8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(0, 28).addBox(-1.5F, 9F, -5F, 3F, 5F, 3F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 28).addBox(-2F, 5F, -6F, 4F, 4F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1F, 7F, -8F, 2F, 1F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("comb",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, 3F, -5F, 1F, 2F, 3F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing_l",
                CubeListBuilder.create().texOffs(44, 0).addBox(-4F, 14F, -3F, 1F, 4F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing_r",
                CubeListBuilder.create().texOffs(44, 0).addBox(3F, 14F, -3F, 1F, 4F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail_fan",
                CubeListBuilder.create().texOffs(44, 0).addBox(-2F, 12F, 3.5F, 4F, 3F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(44, 0).addBox(-2F, 20F, -0.5F, 1F, 4F, 1F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(44, 0).addBox(1F, 20F, -0.5F, 1F, 4F, 1F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // model_sync:end

        return LayerDefinition.create(mesh, 64, 64);
    }
}
