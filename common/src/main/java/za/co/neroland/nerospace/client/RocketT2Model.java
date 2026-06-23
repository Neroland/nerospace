package za.co.neroland.nerospace.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Tier 2 rocket geometry: the classic hull plus twin side boosters. Geometry-only holder; the
 * renderer bakes this layer and feeds it to the shared {@link RocketModel} class.
 */
public final class RocketT2Model {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "rocket_t2"), "main");

    private RocketT2Model() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6F, -16F, -6F, 12F, 36F, 12F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(0, 56).addBox(-4F, -24F, -4F, 8F, 8F, 8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("console",
                CubeListBuilder.create().texOffs(44, 68).addBox(-4F, -1.5F, -5.5F, 8F, 5F, 1F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bell",
                CubeListBuilder.create().texOffs(0, 80).addBox(-4F, 20F, -4F, 8F, 3F, 8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("booster_w",
                CubeListBuilder.create().texOffs(80, 0).addBox(-10F, 2F, -2.5F, 4F, 18F, 5F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("booster_e",
                CubeListBuilder.create().texOffs(80, 0).addBox(6F, 2F, -2.5F, 4F, 18F, 5F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("fin_north",
                CubeListBuilder.create().texOffs(64, 0).addBox(-1F, 14F, -10F, 2F, 10F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("fin_south",
                CubeListBuilder.create().texOffs(64, 0).addBox(-1F, 14F, 6F, 2F, 10F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }
}
