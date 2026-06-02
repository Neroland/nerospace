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
 * Xertz Stalker — "Crystal Hunter" (Phase 10c). The hero predator of Greenxertz: a tall, upright
 * crystalline biped that STANDS on two powerful legs, with a layered torso, broad shoulders, a sleek
 * forward head with a heavy brow and jaw, a crest and a row of bladed crystal fins down its back, and
 * long arms that end in down-swept crystal blades. All parts overlap into one cohesive body; the feet
 * are planted on the ground (y=24). UV atlas: body uv (0,0), head/face (0,28), limbs/blades/fins (44,0).
 */
public class XertzStalkerModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "xertz_stalker"), "main");

    public XertzStalkerModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // --- Torso (layered) ---
        root.addOrReplaceChild("pelvis",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3F, 12F, -2.5F, 6F, 4F, 5F),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 4F, -3F, 7F, 9F, 6F),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, 5F, -4F, 5F, 5F, 2F),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("shoulder_left",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6F, 4F, -2.5F, 3F, 4F, 5F),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("shoulder_right",
                CubeListBuilder.create().texOffs(0, 0).addBox(3F, 4F, -2.5F, 3F, 4F, 5F),
                PartPose.offset(0F, 0F, 0F));

        // --- Head ---
        root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2F, 1F, -2F, 4F, 4F, 4F),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 28).addBox(-3F, -3F, -6F, 6F, 5F, 7F),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 28).addBox(-2.5F, 2F, -6F, 5F, 2F, 6F),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("crest",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1F, -7F, -1F, 2F, 6F, 3F),
                PartPose.offsetAndRotation(0F, -2F, 1F, -0.4F, 0F, 0F));

        // --- Bladed crystal fins down the back (connected, angled) ---
        float[] finZ = {1.5F, 4F, 7F};
        float[] finH = {7F, 6F, 4F};
        for (int i = 0; i < finZ.length; i++) {
            root.addOrReplaceChild("fin_" + i,
                    CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -finH[i], -1F, 1F, finH[i], 3F),
                    PartPose.offsetAndRotation(0F, 5F, finZ[i], -0.25F, 0F, 0F));
        }

        // --- Arms ending in down-swept blades ---
        addArm(root, "left", -5F);
        addArm(root, "right", 5F);

        // --- Two powerful standing legs (feet planted at y=24) ---
        addLeg(root, "left", -2.5F);
        addLeg(root, "right", 2.5F);

        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addArm(PartDefinition root, String side, float x) {
        float roll = (x < 0) ? 0.12F : -0.12F;
        root.addOrReplaceChild("arm_upper_" + side,
                CubeListBuilder.create().texOffs(44, 0).addBox(-1.5F, 0F, -1.5F, 3F, 7F, 3F),
                PartPose.offsetAndRotation(x, 5F, 0F, 0F, 0F, roll));
        root.addOrReplaceChild("arm_fore_" + side,
                CubeListBuilder.create().texOffs(44, 0).addBox(-1.5F, 0F, -1.5F, 3F, 6F, 3F),
                PartPose.offset(x, 12F, 0F));
        // A long flat crystal blade sweeping down past the hand.
        root.addOrReplaceChild("blade_" + side,
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, 0F, -2F, 1F, 10F, 5F),
                PartPose.offset(x, 16F, 0F));
    }

    private static void addLeg(PartDefinition root, String side, float x) {
        root.addOrReplaceChild("thigh_" + side,
                CubeListBuilder.create().texOffs(44, 0).addBox(-2F, 0F, -2F, 4F, 6F, 4F),
                PartPose.offset(x, 15F, 0F));
        root.addOrReplaceChild("shin_" + side,
                CubeListBuilder.create().texOffs(44, 0).addBox(-1.5F, 0F, -1.5F, 3F, 4F, 3F),
                PartPose.offset(x, 20F, 0F));
        root.addOrReplaceChild("foot_" + side,
                CubeListBuilder.create().texOffs(44, 0).addBox(-1.5F, 22F, -5F, 3F, 2F, 6F),
                PartPose.offset(x, 0F, 0F));
    }
}
