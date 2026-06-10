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
 * Tier 2 rocket geometry (ART_OVERHAUL_DESIGN.md §4.2): the classic hull plus the twin side
 * boosters the design docs always promised T2. Rendered through {@link RocketModel} (geometry-only
 * holder; the renderer bakes this layer and feeds it to the shared model class).
 */
public final class RocketT2Model {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "rocket_t2"), "main");

    private RocketT2Model() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // model_sync:begin
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6F, -16F, -6F, 12F, 36F, 12F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(0, 48).addBox(-4F, -24F, -4F, 8F, 8F, 8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("console",
                CubeListBuilder.create().texOffs(32, 48).addBox(-4F, -1.5F, -5.5F, 8F, 5F, 1F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bell",
                CubeListBuilder.create().texOffs(0, 48).addBox(-4F, 20F, -4F, 8F, 3F, 8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("booster_w",
                CubeListBuilder.create().texOffs(48, 0).addBox(-10F, 2F, -2.5F, 4F, 18F, 5F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("booster_e",
                CubeListBuilder.create().texOffs(48, 0).addBox(6F, 2F, -2.5F, 4F, 18F, 5F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("fin_north",
                CubeListBuilder.create().texOffs(48, 0).addBox(-1F, 14F, -10F, 2F, 10F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("fin_south",
                CubeListBuilder.create().texOffs(48, 0).addBox(-1F, 14F, 6F, 2F, 10F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // model_sync:end

        return LayerDefinition.create(mesh, 64, 64);
    }
}
