package za.co.neroland.nerospace.world.planet.nyxara.client;

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
 * Dusk Moth: the Greenling body and head with its fronds as feathered antennae, and two pairs of flapping wings.
 * The fore and hind wings flap on {@code ageInTicks} (the hind pair smaller and a little behind); the eye-spots and the
 * violet trailing edge are on the wing faces, and the renderer's glow layer draws them full-bright. The skin's wing
 * islands are at (0,46) / (28,46) (fore) and (0,54) / (20,54) (hind).
 */
public class DuskMothModel extends GreenxertzMobModel<LivingEntityRenderState> {

    private final ModelPart wingLeft;
    private final ModelPart wingRight;
    private final ModelPart hindLeft;
    private final ModelPart hindRight;

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public DuskMothModel(ModelPart root) {
        super(root);
        this.wingLeft = root.getChild("wing_left");
        this.wingRight = root.getChild("wing_right");
        this.hindLeft = root.getChild("hindwing_left");
        this.hindRight = root.getChild("hindwing_right");
        ambient("frond_mid", Direction.Axis.Z, 0.14F, 0F, 0.09F);
        ambient("frond_left", Direction.Axis.Z, 0.14F, 0.9F, 0.09F);
        ambient("frond_right", Direction.Axis.Z, 0.14F, 1.8F, 0.09F);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float fore = Mth.cos(state.ageInTicks * 0.9F) * 0.6F;
        float hind = Mth.cos(state.ageInTicks * 0.9F + 0.3F) * 0.45F;
        this.wingLeft.zRot = fore;
        this.wingRight.zRot = -fore;
        this.hindLeft.zRot = hind;
        this.hindRight.zRot = -hind;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 15F, -3F, 7F, 6F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 28).addBox(-4F, 7F, -4F, 8F, 8F, 8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("frond_mid",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, 1F, -0.5F, 1F, 6F, 1F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("frond_left",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -5F, -0.5F, 1F, 5F, 1F),
                PartPose.offsetAndRotation(-1.5F, 7F, 0F, 0F, 0F, 0.5F));
        root.addOrReplaceChild("frond_right",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -5F, -0.5F, 1F, 5F, 1F),
                PartPose.offsetAndRotation(1.5F, 7F, 0F, 0F, 0F, -0.5F));
        root.addOrReplaceChild("wing_left",
                CubeListBuilder.create().texOffs(0, 46).addBox(-8F, 0F, -3F, 8F, 1F, 6F),
                PartPose.offset(-3.5F, 16F, 0F));
        root.addOrReplaceChild("wing_right",
                CubeListBuilder.create().texOffs(28, 46).addBox(0F, 0F, -3F, 8F, 1F, 6F),
                PartPose.offset(3.5F, 16F, 0F));
        root.addOrReplaceChild("hindwing_left",
                CubeListBuilder.create().texOffs(0, 54).addBox(-6F, 0F, -2F, 6F, 1F, 4F),
                PartPose.offset(-3F, 16.5F, 2F));
        root.addOrReplaceChild("hindwing_right",
                CubeListBuilder.create().texOffs(20, 54).addBox(0F, 0F, -2F, 6F, 1F, 4F),
                PartPose.offset(3F, 16.5F, 2F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
