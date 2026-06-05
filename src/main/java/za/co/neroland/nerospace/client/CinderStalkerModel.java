package za.co.neroland.nerospace.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import za.co.neroland.nerospace.Nerospace;

/**
 * Cinder Stalker — "Magma Hulk" (Phase 10d). Grounded volcanic quadruped with a layered body, big
 * browed head, horns, an obsidian back-ridge, and four hip-pivoted legs that trot diagonally.
 * Idle (10f): its signature is slow, heavy breathing — a deep bob at roughly half the default rate —
 * under a ponderous side-to-side sweep of the big browed head.
 */
public class CinderStalkerModel extends GreenxertzMobModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "cinder_stalker"), "main");

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public CinderStalkerModel(ModelPart root) {
        super(root);
        swingLimb("leg_fl", 0F, 0.6F);
        swingLimb("leg_br", 0F, 0.6F);
        swingLimb("leg_fr", Mth.PI, 0.6F);
        swingLimb("leg_bl", Mth.PI, 0.6F);
        // Signature idle: slow, HEAVY breathing — half the default rate, nearly double the depth.
        breathing(0.045F, 0.9F);
        // Ponderous head sweep (brow + jaw track the head).
        ambient("head", Direction.Axis.Y, 0.04F, 0F, 0.05F);
        ambient("brow", Direction.Axis.Y, 0.04F, 0F, 0.05F);
        ambient("jaw", Direction.Axis.Y, 0.04F, 0F, 0.05F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6F, 8F, -6F, 12F, 9F, 11F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("shoulders",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5F, 5F, -5F, 10F, 4F, 8F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("belly",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5F, 16F, -5F, 10F, 3F, 9F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 28).addBox(-4F, 9F, -13F, 8F, 8F, 8F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("brow",
                CubeListBuilder.create().texOffs(0, 28).addBox(-4.5F, 8F, -11F, 9F, 2F, 6F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 28).addBox(-3.5F, 15F, -13F, 7F, 2F, 8F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("horn_left",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1F, -4F, -1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(-3F, 9F, -9F, -0.5F, 0F, 0.25F));
        root.addOrReplaceChild("horn_right",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1F, -4F, -1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(3F, 9F, -9F, -0.5F, 0F, -0.25F));
        float[] plateZ = {-3F, 1F, 5F};
        for (int i = 0; i < plateZ.length; i++) {
            root.addOrReplaceChild("plate_" + i,
                    CubeListBuilder.create().texOffs(44, 0).addBox(-3F, -4F, -1F, 6F, 5F, 2F),
                    PartPose.offsetAndRotation(0F, 6F, plateZ[i], -0.35F, 0F, 0F));
        }

        // Hip-pivoted legs (pivot at the joint, cube hangs to the floor).
        leg(root, "leg_fl", -4F, -3F);
        leg(root, "leg_fr", 4F, -3F);
        leg(root, "leg_bl", -4F, 3F);
        leg(root, "leg_br", 4F, 3F);

        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void leg(PartDefinition root, String name, float x, float z) {
        root.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(44, 0).addBox(-2F, 0F, -2F, 4F, 8F, 4F),
                PartPose.offset(x, 16F, z));
    }
}
