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
 * Cinder Stalker — "Magma Hulk" (Phase 10c). A grounded, heavy volcanic quadruped: a thick layered
 * body, a hump of shoulders, a big low head with a heavy brow and jaw, a ridge of angled obsidian
 * plates along its spine, short blunt horns, and four planted molten legs. Everything overlaps into
 * one solid mass and the feet sit on the ground (feet at y=24).
 * UV atlas: body uv (0,0), head/face (0,28), plates/horns/legs (44,0).
 */
public class CinderStalkerModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "cinder_stalker"), "main");

    public CinderStalkerModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // --- Body mass (layered for bulk) ---
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6F, 8F, -6F, 12F, 9F, 11F),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("shoulders",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5F, 5F, -5F, 10F, 4F, 8F),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("belly",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5F, 16F, -5F, 10F, 3F, 9F),
                PartPose.offset(0F, 0F, 0F));

        // --- Head (connects to the body front) ---
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 28).addBox(-4F, 9F, -13F, 8F, 8F, 8F),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("brow",
                CubeListBuilder.create().texOffs(0, 28).addBox(-4.5F, 8F, -11F, 9F, 2F, 6F),
                PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 28).addBox(-3.5F, 15F, -13F, 7F, 2F, 8F),
                PartPose.offset(0F, 0F, 0F));

        // Blunt horns angled back off the brow.
        root.addOrReplaceChild("horn_left",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1F, -4F, -1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(-3F, 9F, -9F, -0.5F, 0F, 0.25F));
        root.addOrReplaceChild("horn_right",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1F, -4F, -1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(3F, 9F, -9F, -0.5F, 0F, -0.25F));

        // --- Obsidian back plates (sit on the spine, angled, connected to the body top) ---
        float[] plateZ = {-3F, 1F, 5F};
        for (int i = 0; i < plateZ.length; i++) {
            root.addOrReplaceChild("plate_" + i,
                    CubeListBuilder.create().texOffs(44, 0).addBox(-3F, -4F, -1F, 6F, 5F, 2F),
                    PartPose.offsetAndRotation(0F, 6F, plateZ[i], -0.35F, 0F, 0F));
        }

        // --- Four planted legs (connect to the belly, feet at y=24) ---
        addLeg(root, "leg_fl", -6F, -5F);
        addLeg(root, "leg_fr", 2F, -5F);
        addLeg(root, "leg_bl", -6F, 1F);
        addLeg(root, "leg_br", 2F, 1F);

        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addLeg(PartDefinition root, String name, float x, float z) {
        root.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(44, 0).addBox(x, 17F, z, 4F, 7F, 4F),
                PartPose.offset(0F, 0F, 0F));
    }
}
