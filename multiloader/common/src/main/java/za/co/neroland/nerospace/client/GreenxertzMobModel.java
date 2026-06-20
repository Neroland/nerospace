package za.co.neroland.nerospace.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * Base for the Nerospace creature models (Phase 10d/10f). Subclasses build their geometry with
 * <b>hip-pivoted</b> limb parts (a {@link ModelPart} whose pivot sits at the joint, with the cubes
 * hanging below it) and register them with {@link #swingLimb}: every registered limb swings fore/aft
 * (xRot) with the walk cycle, scaled by walk speed — so the mobs actually stride/skitter as they move.
 *
 * <p>On top of the walk cycle this base drives <b>idle/ambient</b> motion from
 * {@code state.ageInTicks} (Phase 10f), all of it fading out as the walk speed rises so it never
 * fights the stride:
 * <ul>
 *   <li><b>Breathing bob</b> — every part except the planted (swing-registered) legs bobs gently on
 *       Y; rate/depth tunable per creature via {@link #breathing} (e.g. the Magma Hulk breathes slow
 *       and heavy).</li>
 *   <li><b>Ambient oscillators</b> — {@link #ambient} registers a per-part sine on a chosen rotation
 *       axis (head sway, blade-arm flex, frond wiggle, at-rest leg ripple…). Oscillators on the walk
 *       axis of a swing-registered limb stack additively on the walk pose; everything else is posed
 *       absolutely from the part's build-time rotation.</li>
 * </ul>
 */
public abstract class GreenxertzMobModel<S extends LivingEntityRenderState> extends EntityModel<S> {

    private record Swing(ModelPart part, float baseXRot, float phase, float amp) {
    }

    /** An idle sine on one rotation axis of a part: rot = base + sin(age * freq + phase) * amp. */
    private record Ambient(ModelPart part, Direction.Axis axis, float baseRot, float freq, float phase, float amp) {
    }

    private final List<Swing> swings = new ArrayList<>();
    private final List<Ambient> ambients = new ArrayList<>();
    /** The walk-swung (planted) limb parts — their xRot is owned by the walk cycle. */
    private final Set<ModelPart> swungParts = Collections.newSetFromMap(new IdentityHashMap<>());
    /** Body parts that bob with the idle breathing (everything except the planted legs) → base Y. */
    private final Map<ModelPart, Float> breatheBaseY = new IdentityHashMap<>();
    private boolean breatheCaptured;
    /** Idle breathing rate (radians per tick) and bob depth (pixels); see {@link #breathing}. */
    private float breatheFreq = 0.08F;
    private float breatheAmp = 0.5F;

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
        this.swungParts.add(part);
    }

    /**
     * Registers an idle/ambient oscillator on one rotation axis of a part, driven by
     * {@code ageInTicks} and faded out by walk speed.
     *
     * @param name  the child part name (from {@code createBodyLayer})
     * @param axis  rotation axis to oscillate
     * @param freq  oscillation rate in radians per tick (~0.04 slow … ~0.14 lively)
     * @param phase phase offset in radians (stagger siblings for ripple/wiggle effects)
     * @param amp   oscillation amplitude in radians (keep subtle: ~0.03–0.1)
     */
    protected final void ambient(String name, Direction.Axis axis, float freq, float phase, float amp) {
        ModelPart part = root().getChild(name);
        float base = switch (axis) {
            case X -> part.xRot;
            case Y -> part.yRot;
            case Z -> part.zRot;
        };
        this.ambients.add(new Ambient(part, axis, base, freq, phase, amp));
    }

    /**
     * Tunes the idle breathing bob (default {@code freq=0.08, amp=0.5}) — e.g. the Magma Hulk
     * breathes slower and deeper.
     */
    protected final void breathing(float freq, float amp) {
        this.breatheFreq = freq;
        this.breatheAmp = amp;
    }

    @Override
    public void setupAnim(S state) {
        super.setupAnim(state);
        float pos = state.walkAnimationPos;
        float speed = Math.min(1.0F, state.walkAnimationSpeed);
        for (Swing s : this.swings) {
            s.part().xRot = s.baseXRot() + Mth.cos(pos * 0.6662F + s.phase()) * s.amp() * speed;
        }
        captureBreatheParts();
        // All ambient motion settles as the mob walks so it never fights the stride.
        float idle = 1.0F - speed;
        // Idle breathing: bob the BODY parts only (legs are swing-registered and stay planted, so the
        // feet don't lift off the ground).
        float bob = Mth.sin(state.ageInTicks * this.breatheFreq) * this.breatheAmp * idle;
        for (Map.Entry<ModelPart, Float> e : this.breatheBaseY.entrySet()) {
            e.getKey().y = e.getValue() + bob;
        }
        // Ambient oscillators (head sway, blade-arm flex, frond wiggle, at-rest leg ripple…).
        for (Ambient a : this.ambients) {
            float delta = Mth.sin(state.ageInTicks * a.freq() + a.phase()) * a.amp() * idle;
            ModelPart part = a.part();
            switch (a.axis()) {
                // X is the walk axis: stack on the walk pose for swung limbs (it already reset xRot
                // absolutely above), pose absolutely otherwise.
                case X -> part.xRot = (this.swungParts.contains(part) ? part.xRot : a.baseRot()) + delta;
                case Y -> part.yRot = a.baseRot() + delta;
                case Z -> part.zRot = a.baseRot() + delta;
            }
        }
    }

    /** Lazily record which parts breathe: every part except the planted (swing-registered) legs. */
    private void captureBreatheParts() {
        if (this.breatheCaptured) {
            return;
        }
        ModelPart rootPart = root();
        rootPart.getAllParts().forEach(part -> {
            if (part != rootPart && !this.swungParts.contains(part)) {
                this.breatheBaseY.put(part, part.y);
            }
        });
        this.breatheCaptured = true;
    }
}
