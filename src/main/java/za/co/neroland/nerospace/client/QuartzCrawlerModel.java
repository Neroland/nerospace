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
 * Quartz Crawler — "Geode Skitterer" (Phase 10c). A grounded six-legged crystalline insect: a low
 * domed carapace with an overhanging rim, a cluster of small glowing crystals on its back, a forward
 * sensor-head, and six tapered legs that bend out and plant on the ground. UV atlas: body uv (0,0),
 * head/face (0,28), legs/crystals (44,0).
 */
public class QuartzCrawlerModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "quartz_crawler"), "main");

    public QuartzCrawlerModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Domed carapace (raised centre + shell + overhanging rim).
        root.addOrReplaceChild("dome",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, 12F, -4F, 8F, 3F, 8F),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("shell",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5F, 15F, -5F, 10F, 4F, 10F),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("rim",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.5F, 17F, -5.5F, 11F, 2F, 11F),
                PartPose.offset(0F, 0F, 0F));

        // Forward sensor-head.
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 28).addBox(-3F, 15F, -9F, 6F, 4F, 4F),
                PartPose.offset(0F, 0F, 0F));

        // Glowing crystal cluster on the dome (short, connected nubs).
        root.addOrReplaceChild("crystal_a",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1F, -4F, -1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(-1.5F, 12F, 0F, -0.2F, 0F, 0.3F));
        root.addOrReplaceChild("crystal_b",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1F, -5F, -1F, 2F, 6F, 2F),
                PartPose.offset(1F, 12F, -1F));
        root.addOrReplaceChild("crystal_c",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1F, -3F, -1F, 2F, 4F, 2F),
                PartPose.offsetAndRotation(0F, 12F, 2.5F, -0.3F, 0F, -0.2F));

        // Six legs that bend out and plant on the ground (feet near y=24).
        float[] zs = {-3.5F, 0F, 3.5F};
        for (int i = 0; i < zs.length; i++) {
            root.addOrReplaceChild("leg_left_" + i,
                    CubeListBuilder.create().texOffs(44, 0).addBox(-1F, 0F, -1F, 2F, 10F, 2F),
                    PartPose.offsetAndRotation(-5F, 16F, zs[i], 0F, 0F, 0.55F));
            root.addOrReplaceChild("leg_right_" + i,
                    CubeListBuilder.create().texOffs(44, 0).addBox(-1F, 0F, -1F, 2F, 10F, 2F),
                    PartPose.offsetAndRotation(5F, 16F, zs[i], 0F, 0F, -0.55F));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }
}
