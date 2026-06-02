package za.co.neroland.nerospace.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * Base for the Nerospace creature models (Phase 10d). Subclasses build their geometry with
 * <b>hip-pivoted</b> limb parts (a {@link ModelPart} whose pivot sits at the joint, with the cubes
 * hanging below it) and register each one with {@link #swingLimb}. This base then animates them:
 * every registered limb swings fore/aft (xRot) with the walk cycle, scaled by walk speed — so the
 * mobs actually stride/skitter as they move.
 */
public abstract class GreenxertzMobModel extends EntityModel<LivingEntityRenderState> {

    private record Swing(ModelPart part, float baseXRot, float phase, float amp) {
    }

    private final List<Swing> swings = new ArrayList<>();

    protected GreenxertzMobModel(ModelPart root) {
        super(root);
    }

    /**
     * Registers a hip-pivoted limb to swing with the walk cycle.
     *
     * @param name  the child part name (from {@code createBodyLayer})
     * @param phase phase offset in radians (e.g. {@code Mth.PI} to oppose another limb)
     * @param amp   swing amplitude in radians
     */
    protected final void swingLimb(String name, float phase, float amp) {
        ModelPart part = root().getChild(name);
        this.swings.add(new Swing(part, part.xRot, phase, amp));
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float pos = state.walkAnimationPos;
        float speed = Math.min(1.0F, state.walkAnimationSpeed);
        for (Swing s : this.swings) {
            s.part().xRot = s.baseXRot() + Mth.cos(pos * 0.6662F + s.phase()) * s.amp() * speed;
        }
    }
}
