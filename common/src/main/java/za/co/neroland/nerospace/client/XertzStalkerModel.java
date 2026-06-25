package za.co.neroland.nerospace.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Xertz Stalker — "Crystal Hunter" (Phase 10d). The hero predator: a tall upright crystalline biped
 * with a layered torso, broad shoulders, a browed head, a crest, a row of bladed back-fins, long
 * blade-arms and two legs. Each arm and leg is a single hip-pivoted part (thigh+shin+foot /
 * upper+forearm+blade) that swings as it stalks forward. Idle (10f): subtle head sway plus its
 * signature — the crystal blade-arms slowly flex out and back at the shoulder, like a hunter
 * keeping its blades limber.
 */
public class XertzStalkerModel extends GreenxertzMobModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            NerospaceCommon.id("xertz_stalker"), "main");

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public XertzStalkerModel(ModelPart root) {
        super(root);
        swingLimb("leg_l", 0F, 0.5F);
        swingLimb("leg_r", Mth.PI, 0.5F);
        swingLimb("arm_l", Mth.PI, 0.22F);
        swingLimb("arm_r", 0F, 0.22F);
        // Idle: slow predatory head sway (jaw tracks the head)…
        ambient("head", Direction.Axis.Y, 0.055F, 0F, 0.06F);
        ambient("jaw", Direction.Axis.Y, 0.055F, 0F, 0.06F);
        // …and the signature blade-arm flex: both blades roll outward together at the shoulder.
        ambient("arm_l", Direction.Axis.Z, 0.09F, 0F, 0.05F);
        ambient("arm_r", Direction.Axis.Z, 0.09F, Mth.PI, 0.05F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // model_sync:begin
        root.addOrReplaceChild("pelvis",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3F, 12F, -2.5F, 6F, 4F, 5F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 4F, -3F, 7F, 9F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, 5F, -4F, 5F, 5F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("shoulder_left",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6F, 4F, -2.5F, 3F, 4F, 5F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("shoulder_right",
                CubeListBuilder.create().texOffs(0, 0).addBox(3F, 4F, -2.5F, 3F, 4F, 5F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2F, 1F, -2F, 4F, 4F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 28).addBox(-3F, -3F, -6F, 6F, 5F, 7F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 28).addBox(-2.5F, 2F, -6F, 5F, 2F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // model_sync:end (crest/fins are rotated and the limbs are multi-cube — Java-authoritative)
        root.addOrReplaceChild("crest",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1F, -7F, -1F, 2F, 6F, 3F),
                PartPose.offsetAndRotation(0F, -2F, 1F, -0.4F, 0F, 0F));
        float[] finZ = {1.5F, 4F, 7F};
        float[] finH = {7F, 6F, 4F};
        for (int i = 0; i < finZ.length; i++) {
            root.addOrReplaceChild("fin_" + i,
                    CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -finH[i], -1F, 1F, finH[i], 3F),
                    PartPose.offsetAndRotation(0F, 5F, finZ[i], -0.25F, 0F, 0F));
        }

        // Arms: single hip-pivoted parts (upper + forearm + down-swept blade).
        arm(root, "arm_l", -5F, 0.12F);
        arm(root, "arm_r", 5F, -0.12F);
        // Legs: single hip-pivoted parts (thigh + shin + foot).
        leg(root, "leg_l", -2.5F);
        leg(root, "leg_r", 2.5F);

        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void arm(PartDefinition root, String name, float x, float roll) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                        .texOffs(44, 0).addBox(-1.5F, 0F, -1.5F, 3F, 7F, 3F)
                        .texOffs(44, 0).addBox(-1.5F, 7F, -1.5F, 3F, 6F, 3F)
                        .texOffs(44, 0).addBox(-0.5F, 11F, -2F, 1F, 10F, 5F),
                PartPose.offsetAndRotation(x, 5F, 0F, 0F, 0F, roll));
    }

    private static void leg(PartDefinition root, String name, float x) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                        .texOffs(44, 0).addBox(-2F, 0F, -2F, 4F, 6F, 4F)
                        .texOffs(44, 0).addBox(-1.5F, 6F, -1.5F, 3F, 3F, 3F)
                        .texOffs(44, 0).addBox(-1.5F, 7F, -5F, 3F, 2F, 6F),
                PartPose.offset(x, 15F, 0F));
    }
}
