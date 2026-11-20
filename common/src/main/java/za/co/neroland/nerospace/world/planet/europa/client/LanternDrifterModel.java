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
 * Lantern Drifter (6 bones): a bell, a glowing core hanging under it, and four ribbons from the bell's rim that
 * ripple out of step. The whole body bobs slowly on {@code ageInTicks}. Skin islands: bell (0,0) 8x6x8, core
 * (0,16) 4x3x4, ribbons (32,0) 1x9x1 (shared). The core and ribbon tips are in the glow layer.
 */
public class LanternDrifterModel extends GreenxertzMobModel<LivingEntityRenderState> {

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public LanternDrifterModel(ModelPart root) {
        super(root);
        breathing(0.12F, 0.6F);
        ambient("bell", Direction.Axis.Y, 0.03F, 0F, 0.2F);
        for (int i = 0; i < 4; i++) {
            ambient("ribbon_" + i, Direction.Axis.X, 0.15F, i * 1.6F, 0.35F);
        }
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        root().y += Mth.sin(state.ageInTicks * 0.06F) * 1.5F;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bell",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, 8F, -4F, 8F, 6F, 8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 14F, -2F, 4F, 3F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        float[][] rim = {{-3F, -3F}, {3F, -3F}, {-3F, 3F}, {3F, 3F}};
        for (int i = 0; i < rim.length; i++) {
            root.addOrReplaceChild("ribbon_" + i,
                    CubeListBuilder.create().texOffs(32, 0).addBox(-0.5F, 0F, -0.5F, 1F, 9F, 1F),
                    PartPose.offset(rim[i][0], 14F, rim[i][1]));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }
}
