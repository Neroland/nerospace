package za.co.neroland.nerospace.world.planet.europa.client;

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
 * Shell Skater (9 bones): a low body, a saddle plate on it, a small head, and six splayed legs that stride in a
 * tripod gait (front-left, middle-right, back-left together). Skin islands: body (0,0) 8x4x10, saddle (0,16)
 * 6x1x6, head (28,16) 5x3x4, legs (0,26) 1x10x1 (shared; the bottom rows are the blade feet).
 */
public class ShellSkaterModel extends GreenxertzMobModel<LivingEntityRenderState> {

    private static final float SPLAY = 0.6F;

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public ShellSkaterModel(ModelPart root) {
        super(root);
        for (int i = 0; i < 6; i++) {
            // Tripod gait: legs 0, 3, 4 move together, 1, 2, 5 opposite.
            boolean first = i == 0 || i == 3 || i == 4;
            swingLimb("leg_" + i, first ? 0F : Mth.PI, 0.5F);
        }
        ambient("head", Direction.Axis.Y, 0.05F, 0F, 0.12F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, 13F, -5F, 8F, 4F, 10F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("saddle",
                CubeListBuilder.create().texOffs(0, 16).addBox(-3F, 12F, -3F, 6F, 1F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(28, 16).addBox(-2.5F, 13F, -9F, 5F, 3F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        float[] zs = {-3.5F, 0F, 3.5F};
        int i = 0;
        for (float z : zs) {
            for (int side = -1; side <= 1; side += 2) {
                root.addOrReplaceChild("leg_" + i,
                        CubeListBuilder.create().texOffs(0, 26).addBox(-0.5F, 0F, -0.5F, 1F, 10F, 1F),
                        PartPose.offsetAndRotation(side * 4F, 15F, z, 0F, 0F, -side * SPLAY));
                i++;
            }
        }
        return LayerDefinition.create(mesh, 64, 64);
    }
}
