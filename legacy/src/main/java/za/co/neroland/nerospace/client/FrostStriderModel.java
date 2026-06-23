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
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import za.co.neroland.nerospace.Nerospace;

/**
 * Frost Strider (NEW_DESTINATION_DESIGN.md §4) — a tall, gangly ice predator stalking Glacira on
 * four stilt legs: a slim raised body, a long low-slung neck and angular browed head, a row of
 * ice-shard back spines, and hip-pivoted stilt legs that trot diagonally. Silhouette is deliberately
 * distinct from the four existing creatures (upright biped / low six-leg dome / chubby toddler /
 * heavy quadruped): this one is all legs. Idle: a quick, shallow, bird-like breath under a wary
 * side-to-side head scan, with a faint shimmer-tremble in the back shards.
 */
public class FrostStriderModel extends GreenxertzMobModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "frost_strider"), "main");

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public FrostStriderModel(ModelPart root) {
        super(root);
        // Diagonal trot on long stilts; modest amplitude keeps the feet planted.
        swingLimb("leg_fl", 0F, 0.4F);
        swingLimb("leg_br", 0F, 0.4F);
        swingLimb("leg_fr", Mth.PI, 0.4F);
        swingLimb("leg_bl", Mth.PI, 0.4F);
        // Signature idle: quick, shallow, bird-like breathing.
        breathing(0.12F, 0.3F);
        // Wary head scan (neck + jaw track the head)…
        ambient("head", Direction.Axis.Y, 0.07F, 0F, 0.07F);
        ambient("neck", Direction.Axis.Y, 0.07F, 0F, 0.05F);
        ambient("jaw", Direction.Axis.Y, 0.07F, 0F, 0.07F);
        // …and a faint shimmer-tremble through the ice shards (staggered phases ripple back-to-front).
        ambient("shard_0", Direction.Axis.Z, 0.11F, 0.0F, 0.03F);
        ambient("shard_1", Direction.Axis.Z, 0.11F, 1.6F, 0.03F);
        ambient("shard_2", Direction.Axis.Z, 0.11F, 3.2F, 0.03F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // model_sync:begin
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, 2F, -7F, 8F, 5F, 14F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("haunch",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 0F, 3F, 7F, 3F, 5F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -3F, -11F, 3F, 6F, 5F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 28).addBox(-2.5F, -6F, -17F, 5F, 4F, 7F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("brow",
                CubeListBuilder.create().texOffs(0, 28).addBox(-3F, -7F, -15F, 6F, 1F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 28).addBox(-2F, -2F, -16F, 4F, 1F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // model_sync:end (raked shards are rotated; the stilt legs are two-cube — Java-authoritative)
        // A row of ice-shard spines along the spine, raked back like wind-blown icicles.
        float[] shardZ = {-4F, 0F, 4F};
        float[] shardH = {6F, 7F, 5F};
        for (int i = 0; i < shardZ.length; i++) {
            root.addOrReplaceChild("shard_" + i,
                    CubeListBuilder.create().texOffs(44, 0).addBox(-1F, -shardH[i], -1F, 2F, shardH[i], 2F),
                    PartPose.offsetAndRotation(0F, 2.5F, shardZ[i], -0.3F, 0F, 0F));
        }

        // Four stilt legs: hip pivot at the body line, thin shafts dropping to a small splayed foot.
        leg(root, "leg_fl", -3F, -5F);
        leg(root, "leg_fr", 3F, -5F);
        leg(root, "leg_bl", -3F, 5F);
        leg(root, "leg_br", 3F, 5F);

        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void leg(PartDefinition root, String name, float x, float z) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                        .texOffs(44, 0).addBox(-1F, 0F, -1F, 2F, 15F, 2F)
                        .texOffs(44, 0).addBox(-1.5F, 15F, -2F, 3F, 2F, 4F),
                PartPose.offset(x, 7F, z));
    }
}
