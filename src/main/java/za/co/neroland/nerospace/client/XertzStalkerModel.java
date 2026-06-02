package za.co.neroland.nerospace.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import za.co.neroland.nerospace.Nerospace;

/**
 * Xertz Stalker — "Crystal Hunter" (Phase 10d). The hero predator: a tall upright crystalline biped
 * with a layered torso, broad shoulders, a browed head, a crest, a row of bladed back-fins, long
 * blade-arms and two legs. Each arm and leg is a single hip-pivoted part (thigh+shin+foot /
 * upper+forearm+blade) that swings as it stalks forward.
 */
public class XertzStalkerModel extends GreenxertzMobModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "xertz_stalker"), "main");

    public XertzStalkerModel(ModelPart root) {
        super(root);
        swingLimb("leg_l", 0F, 0.5F);
        swingLimb("leg_r", Mth.PI, 0.5F);
        swingLimb("arm_l", Mth.PI, 0.22F);
        swingLimb("arm_r", 0F, 0.22F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("pelvis",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3F, 12F, -2.5F, 6F, 4F, 5F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 4F, -3F, 7F, 9F, 6F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, 5F, -4F, 5F, 5F, 2F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("shoulder_left",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6F, 4F, -2.5F, 3F, 4F, 5F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("shoulder_right",
                CubeListBuilder.create().texOffs(0, 0).addBox(3F, 4F, -2.5F, 3F, 4F, 5F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2F, 1F, -2F, 4F, 4F, 4F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 28).addBox(-3F, -3F, -6F, 6F, 5F, 7F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 28).addBox(-2.5F, 2F, -6F, 5F, 2F, 6F), PartPose.offset(0F, 0F, 0F));
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
