package za.co.neroland.nerospace.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import za.co.neroland.nerospace.client.RocketGeometry.Pass;
import za.co.neroland.nerospace.rocket.RocketTier;

/**
 * The animated rocket model. One instance per tier and per render {@link Pass}; all passes share
 * the bone skeleton generated in {@link RocketGeometry}, so the same {@link #setupAnim} drives every
 * pass and the hull, its glass, its lamps and its exhaust always move together.
 *
 * <p>Everything here is a pure function of the render state (the base class resets every part to
 * its build pose first), so the model is safe to share between several rockets in view.</p>
 *
 * <p>What moves:</p>
 * <ul>
 *   <li><b>On the pad</b> — the hatch swings open while nobody is aboard, radar dishes turn or scan,
 *       navigation beacons blink red/green, the strobe flashes, solar wings stand deployed.</li>
 *   <li><b>Ignition</b> — plumes bloom under every bell, the hull shudders, the hatch seals, solar
 *       wings fold in, the dish boom folds up.</li>
 *   <li><b>Ascent</b> — landing legs sweep out and up along the hull, engine bells gimbal, grid fins
 *       deploy, and strap-on boosters cut off and fall away past two-thirds of the climb.</li>
 * </ul>
 */
public class RocketModel extends EntityModel<RocketRenderState> {

    private static final String[] CORNERS = {"ne", "se", "sw", "nw"};
    private static final String[] SIDES = {"e", "w", "n", "s"};
    /** Booster drop (model px) and outward drift at full separation; tilt in radians. */
    private static final float SEP_DROP = 70.0F;
    private static final float SEP_DRIFT = 12.0F;
    private static final float SEP_TILT = 0.45F;

    private final Pass pass;
    private final Map<String, ModelPart> parts = new HashMap<>();

    public RocketModel(ModelPart root, RocketTier tier, Pass pass, Function<Identifier, RenderType> renderType) {
        super(root, renderType);
        this.pass = pass;
        for (String path : RocketGeometry.paths(tier)) {
            ModelPart part = root;
            String name = path;
            for (String step : path.split("/")) {
                part = part.getChild(step);
                name = step;
            }
            this.parts.put(name, part);
        }
    }

    /** Bakes the tier's skeleton for one pass, with that pass's render type. */
    public static RocketModel bake(RocketTier tier, Pass pass) {
        Function<Identifier, RenderType> renderType = switch (pass) {
            case HULL -> RenderTypes::entityCutout;
            case GLASS -> RenderTypes::entityTranslucent;
            case GLOW -> RenderTypes::entityTranslucentEmissive;
            case PLUME -> RenderTypes::eyes;
        };
        return new RocketModel(RocketGeometry.layer(tier, pass).bakeRoot(), tier, pass, renderType);
    }

    public Pass pass() {
        return this.pass;
    }

    @Nullable
    private ModelPart part(String name) {
        return this.parts.get(name);
    }

    @Override
    public void setupAnim(RocketRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float t = s.launch;
        float ign = s.ignition;
        boolean lit = s.launching && ign > 0.01F;

        // Hull shudder: hardest at ignition, settling as the climb smooths out.
        ModelPart body = part("body");
        if (body != null) {
            float vib = ign * (1.0F - 0.6F * t);
            body.x += (Mth.sin(age * 2.3F) * 0.5F + Mth.sin(age * 5.1F) * 0.3F) * vib;
            body.z += (Mth.cos(age * 1.9F) * 0.5F + Mth.sin(age * 4.3F) * 0.3F) * vib;
        }

        // Landing legs sweep out and up along the hull once the pad falls away.
        float legFold = smooth((t - 0.06F) / 0.28F);
        for (String tag : CORNERS) {
            ModelPart arm = part("leg_" + tag + "_arm");
            if (arm != null) {
                float deployed = arm.getInitialPose().zRot();
                arm.zRot = Mth.lerp(legFold, deployed, -Mth.PI);
            }
        }

        // Boosters cut off and fall away past two-thirds of the climb.
        float sep = smooth((t - 0.62F) / 0.3F);
        for (String tag : SIDES) {
            ModelPart booster = part("booster_" + tag);
            if (booster == null) {
                continue;
            }
            booster.visible = sep < 1.0F;
            booster.y += SEP_DROP * sep * sep;
            switch (tag) {
                case "e" -> {
                    booster.x += SEP_DRIFT * sep;
                    booster.zRot += SEP_TILT * sep;
                }
                case "w" -> {
                    booster.x -= SEP_DRIFT * sep;
                    booster.zRot -= SEP_TILT * sep;
                }
                case "n" -> {
                    booster.z -= SEP_DRIFT * sep;
                    booster.xRot += SEP_TILT * sep;
                }
                default -> {
                    booster.z += SEP_DRIFT * sep;
                    booster.xRot -= SEP_TILT * sep;
                }
            }
        }

        // Engines gimbal (thrust vectoring wobble); plumes bloom with the ignition ramp and flicker.
        int k = 0;
        for (Map.Entry<String, ModelPart> e : this.parts.entrySet()) {
            String name = e.getKey();
            ModelPart p = e.getValue();
            if (name.startsWith("engine_")) {
                float phase = k * 1.7F;
                p.xRot += (Mth.sin(age * 0.9F + phase) * 0.05F + Mth.sin(age * 2.7F + phase) * 0.02F) * ign;
                p.zRot += (Mth.cos(age * 1.1F + phase) * 0.05F + Mth.cos(age * 3.1F + phase) * 0.02F) * ign;
                k++;
            } else if (name.startsWith("plume_")) {
                boolean boosterPlume = name.startsWith("plume_booster_");
                p.visible = lit && !(boosterPlume && sep > 0.02F);
                float phase = k * 2.3F;
                float flick = Mth.sin(age * 2.1F + phase) * 0.5F + Mth.sin(age * 3.7F + 1.3F * phase) * 0.5F;
                p.yScale = ign * (0.9F + 0.12F * flick) * (1.0F + 0.25F * t);
                float spread = 0.8F + 0.2F * ign + 0.1F * flick;
                p.xScale = spread;
                p.zScale = spread;
                k++;
            }
        }

        // Hatch: swings open (hinge on its +x edge) while nobody is aboard; the ladder goes with it.
        ModelPart hatch = part("hatch");
        if (hatch != null) {
            hatch.yRot += 1.75F * s.hatch;
        }
        ModelPart ladder = part("ladder");
        if (ladder != null) {
            ladder.visible = !s.launching && !s.cargo;
        }

        // Dishes: a mast dish spins; a boom dish scans side to side and its boom folds up for launch.
        float early = smooth(t / 0.14F);
        ModelPart dish = part("dish");
        ModelPart mast = part("dish_mast");
        if (dish != null) {
            if (mast != null) {
                dish.yRot += Mth.sin(age * 0.04F) * 0.6F * (1.0F - early);
                mast.xRot -= (Mth.PI / 2.0F) * early;
            } else {
                dish.yRot += age * 0.05F;
            }
        }

        // Solar wings stand deployed on the pad and fold flat during ignition.
        for (String tag : SIDES) {
            ModelPart wing = part("wing_" + tag + "_arm");
            if (wing != null) {
                wing.zRot = Mth.lerp(early, wing.getInitialPose().zRot(), 0.0F);
            }
            ModelPart grid = part("grid_" + tag + "_arm");
            if (grid != null) {
                grid.zRot -= 1.4F * smooth((t - 0.3F) / 0.2F);
            }
        }

        // Navigation lights: red/green alternate, the strobe flashes.
        int tick = (int) age;
        ModelPart navA = part("nav_a");
        ModelPart navB = part("nav_b");
        ModelPart strobe = part("strobe");
        if (navA != null) {
            navA.visible = tick % 40 < 20;
        }
        if (navB != null) {
            navB.visible = tick % 40 >= 20;
        }
        if (strobe != null) {
            strobe.visible = tick % 24 < 3;
        }
    }

    private static float smooth(float x) {
        if (x <= 0.0F) {
            return 0.0F;
        }
        if (x >= 1.0F) {
            return 1.0F;
        }
        return x * x * (3.0F - 2.0F * x);
    }
}
