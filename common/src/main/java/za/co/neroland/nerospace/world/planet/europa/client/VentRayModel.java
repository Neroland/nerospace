package za.co.neroland.nerospace.world.planet.europa.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Direction;

import za.co.neroland.nerospace.client.GreenxertzMobModel;

/**
 * Vent Ray (7 bones): a flat body, two wings of two segments each that flap in a slow wave (the outer segment
 * further and a little later), a long tail that sways, and the head lobes. Skin islands: body (0,0) 8x2x12, inner
 * wings (0,16) / (32,16) 6x1x10, outer wings (0,28) / (24,28) 5x1x6, tail (0,36) 1x1x10, lobes (40,0) 6x1x2.
 * The ember gill lines are in the glow layer.
 */
public class VentRayModel extends GreenxertzMobModel<LivingEntityRenderState> {

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public VentRayModel(ModelPart root) {
        super(root);
        breathing(0.05F, 0.3F);
        ambient("wing_left", Direction.Axis.Z, 0.09F, 0F, 0.18F);
        ambient("wing_right", Direction.Axis.Z, 0.09F, 0F, -0.18F);
        ambient("wing_left_tip", Direction.Axis.Z, 0.09F, -0.6F, 0.32F);
        ambient("wing_right_tip", Direction.Axis.Z, 0.09F, -0.6F, -0.32F);
        ambient("tail", Direction.Axis.Y, 0.07F, 0F, 0.25F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, 20F, -6F, 8F, 2F, 12F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wing_left",
                CubeListBuilder.create().texOffs(0, 16).addBox(0F, -0.5F, -5F, 6F, 1F, 10F),
                PartPose.offset(4F, 21F, 0F));
        root.addOrReplaceChild("wing_right",
                CubeListBuilder.create().texOffs(32, 16).addBox(-6F, -0.5F, -5F, 6F, 1F, 10F),
                PartPose.offset(-4F, 21F, 0F));
        root.addOrReplaceChild("wing_left_tip",
                CubeListBuilder.create().texOffs(0, 28).addBox(0F, -0.5F, -3F, 5F, 1F, 6F),
                PartPose.offset(10F, 21F, 0F));
        root.addOrReplaceChild("wing_right_tip",
                CubeListBuilder.create().texOffs(24, 28).addBox(-5F, -0.5F, -3F, 5F, 1F, 6F),
                PartPose.offset(-10F, 21F, 0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 36).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 10F),
                PartPose.offset(0F, 21F, 6F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(40, 0).addBox(-3F, 20.5F, -8F, 6F, 1F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
