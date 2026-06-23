package za.co.neroland.nerospace.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import za.co.neroland.nerospace.Nerospace;

/**
 * Ruin Warden model — a hulking crystalline construct: a heavy head, a broad torso, jagged crystal
 * shoulder spires, and thick hip/shoulder-pivoted limbs that stride heavily. Idle: a slow heave and
 * a faint flicker of the shoulder spires.
 */
public class RuinWardenModel extends GreenxertzMobModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "ruin_warden"), "main");

    @SuppressWarnings("this-escape")
    public RuinWardenModel(ModelPart root) {
        super(root);
        breathing(0.05F, 0.8F);
        swingLimb("leg_left", 0F, 0.5F);
        swingLimb("leg_right", Mth.PI, 0.5F);
        swingLimb("arm_left", Mth.PI, 0.35F);
        swingLimb("arm_right", 0F, 0.35F);
        ambient("crystal_left", Direction.Axis.Z, 0.10F, 0F, 0.05F);
        ambient("crystal_right", Direction.Axis.Z, 0.10F, 1.6F, 0.05F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // model_sync:begin
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 28).addBox(-5F, -10F, -5F, 10F, 10F, 10F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6F, 0F, -3F, 12F, 14F, 6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("crystal_left",
                CubeListBuilder.create().texOffs(44, 0).addBox(-9F, -2F, -2F, 3F, 8F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("crystal_right",
                CubeListBuilder.create().texOffs(44, 0).addBox(6F, -2F, -2F, 3F, 8F, 4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // model_sync:end (pivoted limbs below are Java-authoritative)

        root.addOrReplaceChild("arm_left",
                CubeListBuilder.create().texOffs(44, 12).addBox(-2F, 0F, -2F, 4F, 14F, 4F),
                PartPose.offset(-8F, 1F, 0F));
        root.addOrReplaceChild("arm_right",
                CubeListBuilder.create().texOffs(44, 12).addBox(-2F, 0F, -2F, 4F, 14F, 4F),
                PartPose.offset(8F, 1F, 0F));
        root.addOrReplaceChild("leg_left",
                CubeListBuilder.create().texOffs(44, 32).addBox(-2.5F, 0F, -2.5F, 5F, 10F, 5F),
                PartPose.offset(-3F, 14F, 0F));
        root.addOrReplaceChild("leg_right",
                CubeListBuilder.create().texOffs(44, 32).addBox(-2.5F, 0F, -2.5F, 5F, 10F, 5F),
                PartPose.offset(3F, 14F, 0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
