package za.co.neroland.nerospace.world.planet.luna.client;

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
 * Moon Hopper: the Greenling body without arms, with two big hind legs and a stub tail. It moves in hops: both hind
 * legs kick together with the walk cycle and the whole body lifts on each kick. The three head fronds read as ears.
 * The skin's extra UV islands (hind legs at (0,46) and (18,46), tail at (36,46)) are painted for this geometry.
 */
public class MoonHopperModel extends GreenxertzMobModel<LivingEntityRenderState> {

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public MoonHopperModel(ModelPart root) {
        super(root);
        swingLimb("leg_hind_left", 0F, 0.9F);
        swingLimb("leg_hind_right", 0F, 0.9F);
        swingLimb("leg_left", Mth.PI, 0.4F);
        swingLimb("leg_right", Mth.PI, 0.4F);
        ambient("head", Direction.Axis.Y, 0.06F, 0F, 0.07F);
        ambient("cheeks", Direction.Axis.Y, 0.06F, 0F, 0.07F);
        ambient("frond_mid", Direction.Axis.Z, 0.14F, 0F, 0.09F);
        ambient("frond_left", Direction.Axis.Z, 0.14F, 0.9F, 0.09F);
        ambient("frond_right", Direction.Axis.Z, 0.14F, 1.8F, 0.09F);
        ambient("tail", Direction.Axis.Y, 0.2F, 0F, 0.15F);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float speed = Math.min(1.0F, state.walkAnimationSpeed);
        // Hop: the body lifts on every kick of the (in-phase) hind legs.
        root().y += -2.0F * Math.abs(Mth.sin(state.walkAnimationPos * 0.6662F)) * speed;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 15F, -3F, 7F, 6F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("belly",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3F, 19F, -2.5F, 6F, 3F, 5F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 28).addBox(-4F, 7F, -4F, 8F, 8F, 8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("cheeks",
                CubeListBuilder.create().texOffs(0, 28).addBox(-4.5F, 10F, -3.5F, 9F, 3F, 7F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("frond_mid",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, 1F, -0.5F, 1F, 6F, 1F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_left",
                CubeListBuilder.create().texOffs(44, 0).addBox(-2.5F, 21F, -1.5F, 2.5F, 3F, 3F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_right",
                CubeListBuilder.create().texOffs(44, 0).addBox(0F, 21F, -1.5F, 2.5F, 3F, 3F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("frond_left",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -5F, -0.5F, 1F, 5F, 1F),
                PartPose.offsetAndRotation(-1.5F, 7F, 0F, 0F, 0F, 0.5F));
        root.addOrReplaceChild("frond_right",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -5F, -0.5F, 1F, 5F, 1F),
                PartPose.offsetAndRotation(1.5F, 7F, 0F, 0F, 0F, -0.5F));
        // Hind legs (hip-pivoted) and the stub tail.
        root.addOrReplaceChild("leg_hind_left",
                CubeListBuilder.create().texOffs(0, 46).addBox(-4F, 0F, -2F, 3F, 5F, 6F),
                PartPose.offset(-2F, 19F, 1F));
        root.addOrReplaceChild("leg_hind_right",
                CubeListBuilder.create().texOffs(18, 46).addBox(-4F, 0F, -2F, 3F, 5F, 6F),
                PartPose.offset(2F, 19F, 1F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(36, 46).addBox(-1F, -1F, 0F, 2F, 3F, 2F),
                PartPose.offset(0F, 18F, 3F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
