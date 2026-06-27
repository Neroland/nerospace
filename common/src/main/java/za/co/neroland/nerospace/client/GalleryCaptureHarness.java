package za.co.neroland.nerospace.client;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * CLIENT, creative-debug: an automated, fully-reproducible screenshot pass over the
 * {@code /nerospace gallery} scene (the §9.4 shot list — see RELEASE_CHECKLIST.md).
 * {@code /nerospace gallery} builds the scene server-side; this harness then drives the camera, hides
 * every overlay and writes a PNG per shot via {@link Screenshot#grab}, so the gallery can be
 * re-rendered identically after texture/model changes instead of hand-framing every shot.
 *
 * <p><b>Cross-loader.</b> This is loader-agnostic: it only touches vanilla client classes. Each loader
 * wires it from its own client setup — {@link #registerClientCommands(CommandDispatcher)} from the
 * loader's client-command hook (NeoForge/Forge {@code RegisterClientCommandsEvent}, Fabric
 * {@code ClientCommandRegistrationCallback}) and {@link #tick()} from the loader's per-client-tick hook
 * (next to {@code MeteorTrackerHud.tick()}). The command bodies never read the command source, so the
 * registration is generic in the brigadier source type {@code <S>} and the same call serves all three
 * loaders.
 *
 * <p><b>Commands</b> (client-side — they never reach the server dispatcher, which owns
 * {@code /nerospace gallery|station}):
 * <ul>
 *   <li>{@code /nerospace capture [time]} — the gallery pass: teleports into the flat
 *       {@code nerospace:capture} dimension at a fixed origin (identical backdrop every run),
 *       rebuilds the gallery there from scratch ({@code gallery clear} then {@code gallery} — so a
 *       rerun never leaves lingering blocks/entities or stale machine progress), freezes
 *       time/weather/clouds, then shoots the reframed multi-angle shot list.</li>
 *   <li>{@code /nerospace capture planets [time]} — the dimension vistas, each staged as a SELF-CONTAINED
 *       controlled scene (a themed floating platform + frozen signature mobs built fresh at a fixed
 *       origin in the real dimension), so they're 100% reproducible in ANY world — no fixed seed, no
 *       terrain tuning. Each scene clears its own footprint before rebuilding.</li>
 *   <li>{@code /nerospace capture all [time]} — gallery pass then planet pass in one run.</li>
 *   <li>{@code /nerospace capture shot <name>} — capture the CURRENT view once (overlays hidden) to
 *       {@code nerospace/<name>.png}. Use this to grab a hand-framed money shot.</li>
 * </ul>
 *
 * <p>{@code time} accepts {@code day}/{@code noon}/{@code night}/{@code midnight} or a tick number; it
 * only applies in dimensions with a clock (the capture dim + Greenxertz). The space dimensions
 * (Cindara/Glacira/Station) render against their starfield by design.
 *
 * <p>Outputs land in {@code .minecraft/screenshots/nerospace/<shot>.png} at native resolution with the
 * HUD, clouds and view-bob all suppressed for the duration of the run.
 */
public final class GalleryCaptureHarness {

    /** Ticks to let chunks/entities settle and a fresh frame render before grabbing. */
    private static final int SETTLE_TICKS = 12;

    /** Ticks to wait after teleporting + triggering the build for the scene to load and sync. */
    private static final int BUILD_WARMUP_TICKS = 120;

    /** Ticks to wait after teleporting into a planet dimension (cross-dim load + chunk gen). */
    private static final int PLANET_WARMUP_TICKS = 100;

    /** Deterministic flat backdrop (data/nerospace/dimension/capture.json) + the fixed build origin. */
    private static final String CAPTURE_DIMENSION = "nerospace:capture";
    private static final int ORIGIN_X = 0;
    private static final int ORIGIN_Y = 64;
    private static final int ORIGIN_Z = 0;

    /**
     * One framed capture. {@code setup} = server commands run before this shot (teleport, build,
     * summon…); {@code build} = server commands run AFTER warmup (block placement needs loaded
     * chunks); {@code warmup} = ticks to wait after setup for the scene to load/sync; {@code camera}/
     * {@code target} = the pose ({@code null} poses → "keep current view").
     */
    private record Shot(String name, List<String> setup, List<String> build,
            int warmup, Vec3 camera, Vec3 target) {
    }

    private enum Phase { WARMUP, MOVE, SETTLE, SHOOT }

    private static final Deque<Shot> QUEUE = new ArrayDeque<>();
    private static boolean running;
    private static boolean hudWasHidden;
    private static boolean bobViewWas;
    private static CloudStatus cloudsWere;
    private static Phase phase = Phase.MOVE;
    private static int warmup;
    private static int settle;
    private static Shot current;

    // The HUD-hide flag and the main render target both DIVERGE across 26.1.2 ↔ 26.2, so they're
    // resolved reflectively (no compile-time reference to version-specific symbols), the same approach
    // OxygenHud uses. 26.1.x: Options#hideGui (boolean) + Minecraft#getMainRenderTarget(). 26.2:
    // Gui#hud → Hud#isHidden()/toggle() + GameRenderer#mainRenderTarget().
    private static boolean hudResolved;
    private static java.lang.reflect.Field optHideGuiField;     // 26.1.x
    private static java.lang.reflect.Field guiHudField;         // 26.2
    private static java.lang.reflect.Method hudIsHiddenMethod;  // 26.2
    private static java.lang.reflect.Method hudToggleMethod;    // 26.2
    private static boolean rtResolved;
    private static java.lang.reflect.Method mcGetRenderTarget;  // 26.1.x
    private static java.lang.reflect.Method grMainRenderTarget; // 26.2

    private GalleryCaptureHarness() {
    }

    // ------------------------------------------------------------------------------------------------
    // Command registration (generic in the brigadier source type — bodies never read the source).
    // ------------------------------------------------------------------------------------------------

    /**
     * Register the client-side {@code /nerospace capture …} tree on the given dispatcher. Generic in
     * the source type {@code S} so the SAME method serves NeoForge/Forge ({@code CommandSourceStack})
     * and Fabric ({@code FabricClientCommandSource}); the command bodies drive the local client only.
     */
    public static <S> void registerClientCommands(CommandDispatcher<S> dispatcher) {
        dispatcher.register(
                LiteralArgumentBuilder.<S>literal("nerospace")
                        .then(LiteralArgumentBuilder.<S>literal("capture")
                                .executes(ctx -> startCapture("noon"))
                                .then(LiteralArgumentBuilder.<S>literal("planets")
                                        .executes(ctx -> startPlanets("noon"))
                                        .then(RequiredArgumentBuilder.<S, String>argument("time", StringArgumentType.word())
                                                .executes(ctx -> startPlanets(StringArgumentType.getString(ctx, "time")))))
                                .then(LiteralArgumentBuilder.<S>literal("all")
                                        .executes(ctx -> startAll("noon"))
                                        .then(RequiredArgumentBuilder.<S, String>argument("time", StringArgumentType.word())
                                                .executes(ctx -> startAll(StringArgumentType.getString(ctx, "time")))))
                                .then(LiteralArgumentBuilder.<S>literal("shot")
                                        .then(RequiredArgumentBuilder.<S, String>argument("name", StringArgumentType.word())
                                                .executes(ctx -> shotHere(StringArgumentType.getString(ctx, "name")))))
                                .then(RequiredArgumentBuilder.<S, String>argument("time", StringArgumentType.word())
                                        .executes(ctx -> startCapture(StringArgumentType.getString(ctx, "time"))))));
    }

    // ------------------------------------------------------------------------------------------------
    // Run starters.
    // ------------------------------------------------------------------------------------------------

    private static int startCapture(String time) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return 0;
        }
        if (running) {
            mc.player.sendSystemMessage(Component.literal("Gallery capture already running."));
            return 0;
        }
        QUEUE.clear();
        QUEUE.addAll(galleryQueue(time));
        begin(mc);
        mc.player.sendSystemMessage(Component.literal("Gallery capture: rebuilding scene, time=" + time + ", "
                + QUEUE.size() + " shots → screenshots/nerospace/ (overlays hidden)."));
        return Command.SINGLE_SUCCESS;
    }

    private static int startPlanets(String time) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return 0;
        }
        if (running) {
            mc.player.sendSystemMessage(Component.literal("Capture already running."));
            return 0;
        }
        QUEUE.clear();
        QUEUE.addAll(buildPlanetShots(time));
        begin(mc);
        mc.player.sendSystemMessage(Component.literal("Planet capture: " + QUEUE.size()
                + " self-contained scenes across dimensions, time=" + time + " → screenshots/nerospace/."));
        return Command.SINGLE_SUCCESS;
    }

    private static int startAll(String time) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return 0;
        }
        if (running) {
            mc.player.sendSystemMessage(Component.literal("Capture already running."));
            return 0;
        }
        QUEUE.clear();
        QUEUE.addAll(galleryQueue(time));
        QUEUE.addAll(buildPlanetShots(time));
        begin(mc);
        mc.player.sendSystemMessage(Component.literal("Full capture: " + QUEUE.size()
                + " shots (gallery + planets), time=" + time + " → screenshots/nerospace/."));
        return Command.SINGLE_SUCCESS;
    }

    private static int shotHere(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || running) {
            return 0;
        }
        QUEUE.clear();
        QUEUE.add(new Shot(name, List.of(), List.of(), 0, null, null)); // grab the current view as-is
        begin(mc);
        return Command.SINGLE_SUCCESS;
    }

    /** The gallery shot list with the one-time rebuild + environment-pin setup attached to shot 0. */
    private static List<Shot> galleryQueue(String time) {
        List<Shot> shots = new ArrayList<>(buildShots(ORIGIN_X, ORIGIN_Y, ORIGIN_Z));
        // Shot 0 carries the one-time setup: teleport into the flat capture dimension, rebuild the
        // gallery FROM SCRATCH (clear → build, so reruns never stack blocks/entities or keep stale
        // machine progress), and freeze the environment so every rerun is framed + lit identically.
        // These are server commands (they need cheats — the creative gallery world has them).
        List<String> setup = List.of(
                "execute in " + CAPTURE_DIMENSION + " run tp @s "
                        + (ORIGIN_X + 0.5) + " " + ORIGIN_Y + " " + (ORIGIN_Z + 0.5),
                "nerospace gallery clear", // wipe any prior footprint so the rebuild can't stack duplicates
                "nerospace gallery",       // fresh build → fresh machine progress, no lingering artifacts
                "gamerule advance_time false",    // 26.1 renamed doDaylightCycle → advance_time
                "gamerule advance_weather false", // …and doWeatherCycle → advance_weather
                "time set " + time,               // capture dim is overworld-type → has a clock
                "weather clear");
        Shot first = shots.get(0);
        shots.set(0, new Shot(first.name(), setup, List.of(), BUILD_WARMUP_TICKS,
                first.camera(), first.target()));
        return shots;
    }

    private static void begin(Minecraft mc) {
        // screenshots/ is created by Screenshot.grab; the nerospace/ subfolder is not, so make it.
        new File(mc.gameDirectory, "screenshots/nerospace").mkdirs();
        hudWasHidden = isHudHidden(mc);
        setHudHidden(mc, true);
        cloudsWere = mc.options.cloudStatus().get();
        mc.options.cloudStatus().set(CloudStatus.OFF); // clouds scroll with game time → freeze them out of frame
        bobViewWas = mc.options.bobView().get();
        mc.options.bobView().set(false); // kill view-bob so a pinned pose renders the exact same frame
        running = true;
        phase = Phase.MOVE; // each shot's setup carries any teleport/build; warmup is per-shot
        current = null;
    }

    // ------------------------------------------------------------------------------------------------
    // Per-client-tick state machine (driven from each loader's client-tick hook).
    // ------------------------------------------------------------------------------------------------

    /** Called once per client tick from each loader's own client-tick hook. No-op unless a run is live. */
    public static void tick() {
        if (!running) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            finish(mc);
            return;
        }

        switch (phase) {
            case MOVE -> {
                final Shot shot = QUEUE.poll();
                current = shot;
                if (shot == null) {
                    finish(mc);
                    return;
                }
                for (String cmd : shot.setup()) { // pre-warmup: teleport / gamerules / time / kill
                    player.connection.sendCommand(cmd);
                }
                warmup = shot.warmup();
                phase = Phase.WARMUP;
            }
            case WARMUP -> {
                final Shot shot = current;
                if (shot == null) {
                    phase = Phase.MOVE;
                    return;
                }
                if (--warmup <= 0) { // teleport + chunks now loaded
                    for (String cmd : shot.build()) { // block placement needs loaded chunks (else "not loaded")
                        player.connection.sendCommand(cmd);
                    }
                    applyPose(player, shot);
                    settle = SETTLE_TICKS;
                    phase = Phase.SETTLE;
                }
            }
            case SETTLE -> {
                final Shot shot = current;
                if (shot == null) {
                    phase = Phase.MOVE;
                    return;
                }
                applyPose(player, shot); // re-pin every tick so gravity/AI can't drift the camera
                if (--settle <= 0) {
                    phase = Phase.SHOOT;
                }
            }
            case SHOOT -> {
                final Shot shot = current;
                if (shot == null) {
                    phase = Phase.MOVE;
                    return;
                }
                grab(mc, shot.name());
                phase = Phase.MOVE;
            }
        }
    }

    /** Snap the player (the render camera) to the shot's pose, holding it still. */
    private static void applyPose(LocalPlayer player, Shot shot) {
        if (shot == null) {
            return;
        }
        Vec3 cam = shot.camera();
        Vec3 tgt = shot.target();
        if (cam == null || tgt == null) {
            return; // "keep current view" shot
        }
        double dx = tgt.x - cam.x;
        double dy = tgt.y - cam.y;
        double dz = tgt.z - cam.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horiz)));
        player.snapTo(cam.x, cam.y, cam.z, yaw, pitch);
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static void grab(Minecraft mc, String name) {
        RenderTarget target = mainRenderTarget(mc);
        if (target == null) {
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("Capture: could not resolve the render target."));
            }
            return;
        }
        Screenshot.grab(mc.gameDirectory, "nerospace/" + name + ".png", target, 1,
                msg -> {
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(msg);
                    }
                });
    }

    private static void finish(Minecraft mc) {
        setHudHidden(mc, hudWasHidden);
        if (cloudsWere != null) {
            mc.options.cloudStatus().set(cloudsWere);
        }
        mc.options.bobView().set(bobViewWas);
        running = false;
        current = null;
        QUEUE.clear();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("Capture done — see screenshots/nerospace/."));
        }
    }

    // --- HUD hide + render target (version-divergent → reflective) ----------------------------------

    private static void resolveHud() {
        if (hudResolved) {
            return;
        }
        hudResolved = true;
        try { // 26.1.x: Options#hideGui boolean field
            optHideGuiField = net.minecraft.client.Options.class.getDeclaredField("hideGui");
            optHideGuiField.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException e) {
            optHideGuiField = null;
        }
        if (optHideGuiField == null) {
            try { // 26.2: Gui#hud → Hud#isHidden()/toggle()
                guiHudField = net.minecraft.client.gui.Gui.class.getDeclaredField("hud");
                guiHudField.setAccessible(true);
                Class<?> hudType = guiHudField.getType();
                hudIsHiddenMethod = hudType.getMethod("isHidden");
                hudToggleMethod = hudType.getMethod("toggle");
            } catch (ReflectiveOperationException | RuntimeException e) {
                guiHudField = null;
                hudIsHiddenMethod = null;
                hudToggleMethod = null;
            }
        }
    }

    private static boolean isHudHidden(Minecraft mc) {
        resolveHud();
        try {
            if (optHideGuiField != null) {
                return optHideGuiField.getBoolean(mc.options);
            }
            if (guiHudField != null && hudIsHiddenMethod != null) {
                Object hud = guiHudField.get(mc.gui);
                return hud != null && Boolean.TRUE.equals(hudIsHiddenMethod.invoke(hud));
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            // fall through to "not hidden"
        }
        return false;
    }

    private static void setHudHidden(Minecraft mc, boolean hidden) {
        resolveHud();
        try {
            if (optHideGuiField != null) {
                optHideGuiField.setBoolean(mc.options, hidden);
                return;
            }
            if (guiHudField != null && hudIsHiddenMethod != null && hudToggleMethod != null) {
                Object hud = guiHudField.get(mc.gui);
                if (hud != null && !Boolean.valueOf(hidden).equals(hudIsHiddenMethod.invoke(hud))) {
                    hudToggleMethod.invoke(hud); // toggle only when not already in the wanted state
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            // best-effort; a missing flag just means the HUD shows in the shot
        }
    }

    /** The main framebuffer: 26.1.x {@code Minecraft#getMainRenderTarget()}, 26.2 {@code GameRenderer#mainRenderTarget()}. */
    private static RenderTarget mainRenderTarget(Minecraft mc) {
        if (!rtResolved) {
            rtResolved = true;
            try {
                mcGetRenderTarget = Minecraft.class.getMethod("getMainRenderTarget");
            } catch (NoSuchMethodException | RuntimeException e) {
                mcGetRenderTarget = null;
            }
            if (mcGetRenderTarget == null) {
                try {
                    grMainRenderTarget = mc.gameRenderer.getClass().getMethod("mainRenderTarget");
                } catch (ReflectiveOperationException | RuntimeException e) {
                    grMainRenderTarget = null;
                }
            }
        }
        try {
            if (mcGetRenderTarget != null) {
                return (RenderTarget) mcGetRenderTarget.invoke(mc);
            }
            if (grMainRenderTarget != null) {
                return (RenderTarget) grMainRenderTarget.invoke(mc.gameRenderer);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            // fall through
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------
    // Gallery shot list — reframed + multi-angle. Coordinates mirror the cluster bases in the
    // server-side NerospaceCommands.buildGallery (origin = capture-dim origin). Tune the two together.
    // ------------------------------------------------------------------------------------------------

    private static List<Shot> buildShots(int ox, int oy, int oz) {
        List<Shot> shots = new ArrayList<>();

        // BLOCK GRID (EAST, base ox+38 / oz-9, displays float at oy+3). Grid centre ≈ (51, oy+3, 3).
        // High 3/4 read of the whole grid, then a low corner angle.
        shots.add(shot("blocks", 51.5, oy + 28, 30, 51.5, oy + 3, 3));
        shots.add(shot("blocks_angle", 33, oy + 6, -4, 51, oy + 3, 3));

        // MACHINE STRIP (SOUTH, z+48, x −14..14, four wired clusters). Wide read + two detail angles.
        shots.add(shot("machines", 0, oy + 18, 18, 0, oy + 1.5, 48));
        shots.add(shot("machines_power", -12.5, oy + 4, 44, -10.5, oy + 1.5, 48));   // combustion→grinder line
        shots.add(shot("machines_refinery", -5, oy + 4, 44, -5, oy + 1.5, 48));       // battery→refinery→tank

        // PIPES (WEST, x −50..−46, four resource rows stacked in z). View from the east along the rows.
        shots.add(shot("pipes", -40, oy + 7, 0.5, -49, oy + 1.5, 0.5));

        // SUITS + STAR GUIDE (NORTH-WEST, z−34). Suits face ~south → head-on from the south; guide close.
        shots.add(shot("suits", -35, oy + 3.5, -27, -35.5, oy + 2, -34));
        shots.add(shot("star_guide", -28, oy + 3, -29, -28, oy + 2.5, -34));

        // ROCKET ROW (NORTH, z−48, tiers at x −13/−5/3/12; tall). Hero wide + two tier details.
        shots.add(shot("rockets", -0.5, oy + 7, -25, -0.5, oy + 10, -48));
        shots.add(shot("rocket_t1", -13, oy + 3, -43, -13, oy + 6, -48));
        shots.add(shot("rocket_t4", 12, oy + 5, -41, 12, oy + 9, -49));         // Heavy Launch Complex + gantry
        shots.add(shot("rocket_o2", -9, oy + 4, -41, -9, oy + 2, -45));         // live O2 fuelling line
        shots.add(shot("travel_node", 16, oy + 3, -41, 16, oy + 1.5, -45));

        // LAUNCH CONTROLLER (centre-NW, ctrl at −27/oy+1/−18 facing south; hologram projects south).
        shots.add(shot("launch_controller", -27, oy + 4, -9, -27, oy + 2, -18));

        // CREATURES (SOUTH-EAST, z+34, nine frozen along x 18..50). Full line + over-the-shoulder closeup.
        shots.add(shot("creatures", 34, oy + 12, 11, 34, oy + 2, 35));
        shots.add(shot("creatures_closeup", 20, oy + 3, 30, 31, oy + 2, 34));

        // METEOR SITE (SOUTH-WEST, −28/+30; meteor hovers at oy+11). Low angle so crater + core + trail read.
        shots.add(shot("meteor_site", -20, oy + 5, 24, -28, oy + 6, 30));

        // QUARRIES (NORTH-EAST). Landmark L, an operating 9×9 looking into the pit, and the big 33×33 claim.
        shots.add(shot("quarry_landmarks", 31, oy + 6, -30, 31, oy + 1.5, -37));
        shots.add(shot("quarry_operating", 46, oy + 10, -27, 45, oy - 2, -37));
        shots.add(shot("quarry_big", 102, oy + 22, -34, 101, oy - 4, -56));

        // SOLAR ARRAYS (SOUTH-WEST, base −50/+36). Wide read of all tiers + a closer field angle.
        shots.add(shot("solar", -42, oy + 12, 23, -42, oy + 2, 40));
        shots.add(shot("solar_field", -48, oy + 6, 30, -42, oy + 2, 41));

        return shots;
    }

    // ------------------------------------------------------------------------------------------------
    // Planet vistas — SELF-CONTAINED controlled scenes (no terrain/seed dependence). Each builds a
    // fresh themed platform + frozen signature mobs at a fixed origin in the real dimension and clears
    // its own footprint first, so a rerun in ANY world produces the identical shot.
    // ------------------------------------------------------------------------------------------------

    private static List<Shot> buildPlanetShots(String time) {
        List<Shot> shots = new ArrayList<>();

        // Greenxertz (overworld-type → has a clock): green steel platform + xertz quartz / nerosteel
        // accents, with the Greenxertz fauna frozen on deck.
        shots.add(planetScene("greenxertz_vista", "nerospace:greenxertz", true, time,
                0, 110, 0, "nerospace:nerosteel_block", "nerospace:xertz_quartz_ore", "nerospace:alien_lamp",
                List.of("nerospace:xertz_stalker", "nerospace:quartz_crawler", "nerospace:greenling")));

        // Cindara (space-type → no clock, volcanic dark): cindrite platform + cindrite ore + meteor rock.
        shots.add(planetScene("cindara_basin", "nerospace:cindara", false, time,
                0, 110, 0, "nerospace:cindrite_block", "nerospace:cindrite_ore", "nerospace:meteor_rock",
                List.of("nerospace:cinder_stalker", "nerospace:ember_strutter")));

        // Glacira (space-type → no clock, frozen): glacite platform + glacite ore + blue ice.
        shots.add(planetScene("glacira_frost", "nerospace:glacira", false, time,
                0, 110, 0, "nerospace:glacite_block", "nerospace:glacite_ore", "minecraft:blue_ice",
                List.of("nerospace:frost_strider", "nerospace:woolly_drift")));

        // Orbital Station (space-type → starfield): a station floor pad with two walls + a Station Core.
        shots.add(stationScene("orbital_station", time));

        // Terraform before/after — staged on SUNLIT Greenxertz so the "living" palette reads bright.
        // Two patches at the same fixed origin, each fully rebuilt + cleared (so before never bleeds
        // into after). BEFORE = barren coarse-dirt; AFTER = grass on the neon terraformed_meadow biome
        // with two trees and a Meadow Loper.
        shots.add(terraformScene("terraform_before", time, false));
        shots.add(terraformScene("terraform_after", time, true));

        return shots;
    }

    /**
     * A self-contained planet scene: clear a box, lay a 13×13 themed platform (with a 3×3 ore inlay and
     * a short accent pillar), freeze the signature mobs on deck, and frame it 3/4-on against the real
     * dimension's sky. All block ops + summons run POST-warmup so the chunks are loaded.
     */
    private static Shot planetScene(String name, String dim, boolean hasClock, String time,
            int ox, int oy, int oz, String platform, String ore, String accent, List<String> mobs) {
        List<String> setup = teleportSetup(dim, hasClock, time, ox + 0.5, oy + 4, oz - 9);
        List<String> build = new ArrayList<>();
        // Clear the whole working box (incl. any prior platform) so reruns leave no artifacts.
        build.add(fill(ox - 8, oy - 1, oz - 8, ox + 8, oy + 20, oz + 8, "minecraft:air"));
        killMobs(build, mobs);
        // 13×13 platform, a 3×3 ore inlay at centre, and a 3-high accent pillar at the far corner.
        build.add(fill(ox - 6, oy, oz - 6, ox + 6, oy, oz + 6, platform));
        build.add(fill(ox - 1, oy, oz - 1, ox + 1, oy, oz + 1, ore));
        build.add(fill(ox + 5, oy + 1, oz + 5, ox + 5, oy + 3, oz + 5, accent));
        // Frozen, persistent signature mobs spaced across the deck.
        for (int i = 0; i < mobs.size(); i++) {
            int mxp = ox - 3 + i * 3;
            build.add(summon(mobs.get(i), mxp, oy + 1, oz));
        }
        Vec3 cam = new Vec3(ox - 9, oy + 4, oz - 9);
        Vec3 tgt = new Vec3(ox, oy + 2, oz);
        return new Shot(name, setup, build, PLANET_WARMUP_TICKS, cam, tgt);
    }

    /** The orbital station scene: a floor pad with two walls and a bound Station Core against the void. */
    private static Shot stationScene(String name, String time) {
        int ox = 0;
        int oy = 96;
        int oz = 0;
        List<String> setup = teleportSetup("nerospace:station", false, time, ox - 6.5, oy + 4, oz - 6.5);
        List<String> build = new ArrayList<>();
        build.add(fill(ox - 2, oy - 1, oz - 2, ox + 8, oy + 6, oz + 8, "minecraft:air")); // clear prior platform
        build.add(fill(ox, oy, oz, ox + 6, oy, oz + 6, "nerospace:station_floor"));
        build.add(fill(ox, oy + 1, oz, ox + 6, oy + 1, oz, "nerospace:station_wall"));
        build.add(fill(ox, oy + 1, oz, ox, oy + 1, oz + 6, "nerospace:station_wall"));
        build.add(setblock(ox + 3, oy + 1, oz + 3, "nerospace:station_core"));
        Vec3 cam = new Vec3(ox - 6, oy + 5, oz - 6);
        Vec3 tgt = new Vec3(ox + 3, oy + 1, oz + 3);
        return new Shot(name, setup, build, PLANET_WARMUP_TICKS, cam, tgt);
    }

    /**
     * Terraform before/after on sunlit Greenxertz, at a fixed origin, fully rebuilt + cleared each run.
     * {@code after=false} → barren coarse-dirt on the native biome; {@code after=true} → grass on the
     * neon {@code terraformed_meadow} biome with two oaks and a Meadow Loper.
     */
    private static Shot terraformScene(String name, String time, boolean after) {
        int bx = 40;
        int by = 110;
        int bz = 40;
        int x2 = bx + 12;
        int z2 = bz + 12;
        List<String> setup = teleportSetup("nerospace:greenxertz", true, time, bx - 5, by + 7, bz - 5);
        List<String> build = new ArrayList<>();
        build.add(fill(bx - 2, by, bz - 2, x2 + 2, by + 12, z2 + 2, "minecraft:air"));
        build.add("kill @e[type=nerospace:meadow_loper]");
        // Reset the biome each run so the prior pass never lingers.
        build.add("fillbiome " + bx + " " + (by - 1) + " " + bz + " " + x2 + " " + (by + 10) + " " + z2
                + " " + (after ? "nerospace:terraformed_meadow" : "nerospace:greenxertz"));
        build.add(fill(bx, by - 1, bz, x2, by - 1, z2, "minecraft:dirt")); // sub-base
        if (after) {
            build.add(fill(bx, by, bz, x2, by, z2, "minecraft:grass_block"));
            build.add("place feature minecraft:oak " + (bx + 3) + " " + (by + 1) + " " + (bz + 3));
            build.add("place feature minecraft:oak " + (bx + 9) + " " + (by + 1) + " " + (bz + 8));
            build.add(summon("nerospace:meadow_loper", bx + 6, by + 1, bz + 6));
        } else {
            build.add(fill(bx, by, bz, x2, by, z2, "minecraft:coarse_dirt"));
        }
        Vec3 cam = new Vec3(bx - 6, by + 7, bz - 6);
        Vec3 tgt = new Vec3(bx + 6, by, bz + 6);
        return new Shot(name, setup, build, PLANET_WARMUP_TICKS, cam, tgt);
    }

    // ------------------------------------------------------------------------------------------------
    // Small command/shot builders.
    // ------------------------------------------------------------------------------------------------

    /** Pre-warmup setup: cross-dim teleport to the camera vantage, pin time/weather (clock dims only). */
    private static List<String> teleportSetup(String dim, boolean hasClock, String time,
            double camX, double camY, double camZ) {
        List<String> setup = new ArrayList<>();
        setup.add("execute in " + dim + " run tp @s " + camX + " " + camY + " " + camZ);
        setup.add("gamerule advance_time false");   // 26.1: replaces doDaylightCycle
        setup.add("gamerule advance_weather false"); // 26.1: replaces doWeatherCycle
        setup.add("weather clear");
        if (hasClock) {
            setup.add("time set " + time); // only dimensions with a clock accept this
        }
        return setup;
    }

    private static void killMobs(List<String> out, List<String> mobs) {
        for (String mob : mobs) {
            out.add("kill @e[type=" + mob + "]");
        }
    }

    private static String fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        return "fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2 + " " + z2 + " " + block;
    }

    private static String setblock(int x, int y, int z, String block) {
        return "setblock " + x + " " + y + " " + z + " " + block;
    }

    private static String summon(String type, int x, int y, int z) {
        return "summon " + type + " " + (x + 0.5) + " " + y + " " + (z + 0.5)
                + " {NoAI:1b,PersistenceRequired:1b}";
    }

    /** A camera-only gallery shot (no setup/build; the rebuild rides on shot 0). */
    private static Shot shot(String name, double cx, double cy, double cz, double tx, double ty, double tz) {
        return new Shot(name, List.of(), List.of(), 0, new Vec3(cx, cy, cz), new Vec3(tx, ty, tz));
    }
}
