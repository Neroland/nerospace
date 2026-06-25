package za.co.neroland.nerospace.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Tier 3 rocket geometry: a stretched nose over a four-slab ring skirt — the sleek long-range
 * silhouette. Geometry-only holder, rendered via {@link RocketModel}.
 */
public final class RocketT3Model {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            NerospaceCommon.id("rocket_t3"), "main");

    private RocketT3Model() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6F, -16F, -6F, 12F, 36F, 12F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(0, 56).addBox(-4F, -30F, -4F, 8F, 14F, 8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("tip",
                CubeListBuilder.create().texOffs(44, 56).addBox(-2F, -34F, -2F, 4F, 4F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("console",
                CubeListBuilder.create().texOffs(44, 68).addBox(-4F, -1.5F, -5.5F, 8F, 5F, 1F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bell",
                CubeListBuilder.create().texOffs(0, 80).addBox(-4F, 20F, -4F, 8F, 3F, 8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("skirt_n",
                CubeListBuilder.create().texOffs(64, 32).addBox(-7F, 12F, -7F, 14F, 4F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("skirt_s",
                CubeListBuilder.create().texOffs(64, 32).addBox(-7F, 12F, 5F, 14F, 4F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("skirt_w",
                CubeListBuilder.create().texOffs(64, 48).addBox(-7F, 12F, -5F, 2F, 4F, 10F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("skirt_e",
                CubeListBuilder.create().texOffs(64, 48).addBox(5F, 12F, -5F, 2F, 4F, 10F),
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
