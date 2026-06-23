package za.co.neroland.nerospace.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.Nerospace;

/**
 * Tier 4 rocket geometry (ART_OVERHAUL_DESIGN.md §4.2): the heavy — a widened core with FOUR
 * strap-on boosters, built for the Heavy Launch Complex. Geometry-only holder, rendered via
 * {@link RocketModel}.
 */
public final class RocketT4Model {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "rocket_t4"), "main");

    private RocketT4Model() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // model_sync:begin
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-7F, -16F, -7F, 14F, 36F, 14F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(0, 56).addBox(-5F, -26F, -5F, 10F, 10F, 10F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("console",
                CubeListBuilder.create().texOffs(44, 68).addBox(-4F, -1.5F, -6.5F, 8F, 5F, 1F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bell",
                CubeListBuilder.create().texOffs(0, 80).addBox(-5F, 20F, -5F, 10F, 3F, 10F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("booster_w",
                CubeListBuilder.create().texOffs(80, 0).addBox(-11F, 2F, -3F, 4F, 18F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("booster_e",
                CubeListBuilder.create().texOffs(80, 0).addBox(7F, 2F, -3F, 4F, 18F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("booster_n",
                CubeListBuilder.create().texOffs(104, 0).addBox(-3F, 2F, -11F, 6F, 18F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("booster_s",
                CubeListBuilder.create().texOffs(104, 0).addBox(-3F, 2F, 7F, 6F, 18F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // model_sync:end

        return LayerDefinition.create(mesh, 128, 128);
    }
}
