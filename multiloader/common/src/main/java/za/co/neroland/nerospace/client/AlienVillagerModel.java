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

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Alien Villager (Phase 0) — an upright humanoid: a domed head, a robed torso, two crystalline
 * shoulder growths (the Greenxertz silhouette cue), and hip/shoulder-pivoted arms and legs that
 * stride as it walks. Idle: a slow head sway and a faint wobble of the shoulder crystals.
 *
 * <p>Geometry split (model_sync rules): the static torso/head/crystals live in the marker block
 * (one cube per bone, no rotation), so they round-trip to {@code alien_villager.bbmodel}. The
 * pivoted limbs sit OUTSIDE the block (Java-authoritative) because their pivots are at the joint,
 * which the marker form can't express.
 */
public class AlienVillagerModel extends GreenxertzMobModel<AlienVillagerRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "alien_villager"), "main");

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public AlienVillagerModel(ModelPart root) {
        super(root);
        // Opposed stride: legs lead, arms counter-swing.
        swingLimb("leg_left", 0F, 0.6F);
        swingLimb("leg_right", Mth.PI, 0.6F);
        swingLimb("arm_left", Mth.PI, 0.4F);
        swingLimb("arm_right", 0F, 0.4F);
        // Idle: a slow, calm head sway and a faint out-of-phase wobble of the shoulder crystals.
        ambient("head", Direction.Axis.Y, 0.05F, 0F, 0.06F);
        ambient("crystal_left", Direction.Axis.Z, 0.08F, 0F, 0.04F);
        ambient("crystal_right", Direction.Axis.Z, 0.08F, 1.4F, 0.04F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // model_sync:begin
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 28).addBox(-4F, -6F, -4F, 8F, 8F, 8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, 2F, -2F, 8F, 12F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("crystal_left",
                CubeListBuilder.create().texOffs(44, 0).addBox(-6F, 0F, -1F, 2F, 5F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("crystal_right",
                CubeListBuilder.create().texOffs(44, 0).addBox(4F, 0F, -1F, 2F, 5F, 2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // model_sync:end (pivoted limbs below are Java-authoritative — joints, not origin-anchored)

        // Shoulder-pivoted arms (pivot at the shoulder, cubes hang below).
        root.addOrReplaceChild("arm_left",
                CubeListBuilder.create().texOffs(44, 8).addBox(-1F, 0F, -2F, 2F, 11F, 4F),
                PartPose.offset(-5F, 3F, 0F));
        root.addOrReplaceChild("arm_right",
                CubeListBuilder.create().texOffs(44, 8).addBox(-1F, 0F, -2F, 2F, 11F, 4F),
                PartPose.offset(5F, 3F, 0F));

        // Hip-pivoted legs (pivot at the hip; feet rest on the ground at java y=24).
        root.addOrReplaceChild("leg_left",
                CubeListBuilder.create().texOffs(44, 24).addBox(-1.5F, 0F, -2F, 3F, 10F, 4F),
                PartPose.offset(-2F, 14F, 0F));
        root.addOrReplaceChild("leg_right",
                CubeListBuilder.create().texOffs(44, 24).addBox(-1.5F, 0F, -2F, 3F, 10F, 4F),
                PartPose.offset(2F, 14F, 0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
