package za.co.neroland.nerospace.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.Nerospace;

/**
 * A lumpy meteor: a chunky charred core with a couple of bumps for an irregular silhouette. Built
 * with the 26.1 {@code LayerDefinition} mesh API; the renderer tumbles it and the entity trails fire.
 * Authored purely in Java (not registered with {@code model_sync.py}), so the build never depends on
 * a Blockbench source for it.
 */
public class FallingMeteorModel extends EntityModel<EntityRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "falling_meteor"), "main");

    public FallingMeteorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6F, -6F, -6F, 12F, 12F, 12F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bump",
                CubeListBuilder.create().texOffs(0, 28).addBox(3F, -8F, -2F, 6F, 6F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
