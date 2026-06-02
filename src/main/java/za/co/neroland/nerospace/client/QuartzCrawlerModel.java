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
 * Quartz Crawler model (Phase 10): a low, wide, six-legged crawler — a flat broad body close to the
 * ground, a forward-jutting head, and three short legs splayed out each side. Reuses the shared
 * Greenxertz creature texture layout.
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

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5F, 14F, -4F, 10F, 5F, 8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 14).addBox(-3F, 13F, -9F, 6F, 5F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Three short legs splayed out per side.
        int[] zs = {-3, 0, 3};
        int i = 0;
        for (int z : zs) {
            root.addOrReplaceChild("leg_left_" + i,
                    CubeListBuilder.create().texOffs(28, 0).addBox(-7F, 19F, (float) z, 2F, 5F, 2F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("leg_right_" + i,
                    CubeListBuilder.create().texOffs(28, 0).addBox(5F, 19F, (float) z, 2F, 5F, 2F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            i++;
        }

        return LayerDefinition.create(mesh, 64, 64);
    }
}
