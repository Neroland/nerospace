package za.co.neroland.nerospace.world.planet.mars.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import za.co.neroland.nerospace.client.GreenxertzMobModel;

/**
 * Salvage Drone: a hover chassis (the Quartz Crawler's dome, shell, rim and sensor head, its crystals as antenna
 * masts) on four thruster pods instead of legs, with a manipulator arm under the nose that sways slowly. The whole
 * model bobs one pixel on {@code ageInTicks}; the renderer lifts it off the ground. The pod bottoms are cyan thruster
 * plates in the glow layer. Skin islands: pods at (0,56) / (12,56) / (24,56) / (36,56), arm at (48,40).
 */
public class SalvageDroneModel extends GreenxertzMobModel<LivingEntityRenderState> {

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public SalvageDroneModel(ModelPart root) {
        super(root);
        ambient("head", Direction.Axis.Y, 0.05F, 0F, 0.08F);
        ambient("arm", Direction.Axis.X, 0.04F, 0F, 0.2F);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        root().y += Mth.sin(state.ageInTicks * 0.1F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("dome",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, 12F, -4F, 8F, 3F, 8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("shell",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5F, 15F, -5F, 10F, 4F, 10F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("rim",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.5F, 17F, -5.5F, 11F, 2F, 11F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 28).addBox(-3F, 15F, -9F, 6F, 4F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("crystal_a",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1F, -4F, -1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(-1.5F, 12F, 0F, -0.2F, 0F, 0.3F));
        root.addOrReplaceChild("crystal_b",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1F, -5F, -1F, 2F, 6F, 2F), PartPose.offset(1F, 12F, -1F));
        root.addOrReplaceChild("crystal_c",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1F, -3F, -1F, 2F, 4F, 2F),
                PartPose.offsetAndRotation(0F, 12F, 2.5F, -0.3F, 0F, -0.2F));
        // Four thruster pods at the corners of the rim.
        float[][] pods = {{-4F, -4F}, {4F, -4F}, {-4F, 4F}, {4F, 4F}};
        for (int i = 0; i < pods.length; i++) {
            root.addOrReplaceChild("pod_" + i,
                    CubeListBuilder.create().texOffs(i * 12, 56).addBox(-1.5F, 0F, -1.5F, 3F, 3F, 3F),
                    PartPose.offset(pods[i][0], 19F, pods[i][1]));
        }
        root.addOrReplaceChild("arm",
                CubeListBuilder.create().texOffs(48, 40).addBox(-1F, 0F, -1F, 2F, 8F, 2F),
                PartPose.offset(0F, 19F, -4F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
