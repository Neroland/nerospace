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
 * Quartz Crawler — "Geode Skitterer" (Phase 10d). A low domed carapace with a back crystal cluster, a
 * sensor-head, and six hip-pivoted legs that ripple front-to-back as it skitters. Idle (10f): the
 * sensor-head scans side to side and its signature — the six legs keep a faint front-to-back ripple
 * even at rest, like an insect that never quite settles.
 */
public class QuartzCrawlerModel extends GreenxertzMobModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "quartz_crawler"), "main");

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public QuartzCrawlerModel(ModelPart root) {
        super(root);
        for (int i = 0; i < 3; i++) {
            swingLimb("leg_left_" + i, i * 2.1F, 0.3F);
            swingLimb("leg_right_" + i, Mth.PI + i * 2.1F, 0.3F);
            // Signature idle: a faint at-rest leg ripple, staggered front-to-back like the skitter.
            ambient("leg_left_" + i, Direction.Axis.X, 0.12F, i * 2.1F, 0.05F);
            ambient("leg_right_" + i, Direction.Axis.X, 0.12F, Mth.PI + i * 2.1F, 0.05F);
        }
        // The sensor-head scans slowly side to side.
        ambient("head", Direction.Axis.Y, 0.05F, 0F, 0.08F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("dome",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, 12F, -4F, 8F, 3F, 8F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("shell",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5F, 15F, -5F, 10F, 4F, 10F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("rim",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.5F, 17F, -5.5F, 11F, 2F, 11F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 28).addBox(-3F, 15F, -9F, 6F, 4F, 4F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("crystal_a",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1F, -4F, -1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(-1.5F, 12F, 0F, -0.2F, 0F, 0.3F));
        root.addOrReplaceChild("crystal_b",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1F, -5F, -1F, 2F, 6F, 2F), PartPose.offset(1F, 12F, -1F));
        root.addOrReplaceChild("crystal_c",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1F, -3F, -1F, 2F, 4F, 2F),
                PartPose.offsetAndRotation(0F, 12F, 2.5F, -0.3F, 0F, -0.2F));

        // Six hip-pivoted legs (roll outward, swing fore/aft for the skitter).
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
