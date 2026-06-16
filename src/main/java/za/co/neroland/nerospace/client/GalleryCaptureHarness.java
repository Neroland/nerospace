package za.co.neroland.nerospace.client;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.annotation.Nullable;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import za.co.neroland.nerospace.Nerospace;

/**
 * CLIENT, creative-debug: an automated screenshot pass over the {@code /nerospace gallery} scene
 * (the §9.4 shot list — see RELEASE_CHECKLIST.md). {@code /nerospace gallery} builds the scene
 * server-side; this harness then drives the camera, hides the HUD and writes a PNG per shot via
 * {@link Screenshot#grab}, so the gallery can be re-rendered repeatably after texture/model changes
 * instead of hand-framing every shot.
 *
 * <p>Commands (client-side, so they never reach the server):
 * <ul>
 *   <li>{@code /nsgallery capture [time]} — one shot: teleports into the flat {@code nerospace:capture}
 *       dimension at a fixed origin (so the backdrop is identical every run), builds the scene
 *       ({@code /nerospace gallery}), freezes the daylight/weather cycle at {@code time} (default
 *       {@code noon}; accepts {@code day}/{@code noon}/{@code night}/{@code midnight} or a tick
 *       number), disables clouds, waits for the scene to load, then screenshots each cluster to
 *       {@code .minecraft/screenshots/nerospace/<shot>.png}. Run from any creative world.</li>
 *   <li>{@code /nsgallery planets [time]} — fly through the planet dimensions
 *       ({@code greenxertz}/{@code cindara}/{@code glacira}/{@code station}) plus a terraform
 *       before/after pair, summoning frozen signature mobs and shooting a vista in each. Planet
 *       terrain is seed-dependent, so run this in a FIXED-SEED capture world and tune the coords in
 *       {@link #buildPlanetShots} to that seed.</li>
 *   <li>{@code /nsgallery shot <name>} — capture the CURRENT view once (HUD hidden) to
 *       {@code nerospace/<name>.png}. Use this to grab a hand-framed money shot.</li>
 * </ul>
 *
 * <p>PROTOTYPE NOTE: the per-shot camera vantages below are deliberate-but-rough starting points
 * (each cluster framed from a few blocks "south" looking north). Wide strips like the machine line
 * and rocket row won't fully fit in one frame — tune {@link #buildShots} or split them. Camera time
 * of day / weather are pinned at the start of a {@code capture} run for consistent lighting.
 */
@EventBusSubscriber(modid = Nerospace.MODID, value = Dist.CLIENT)
public final class GalleryCaptureHarness {

    /** Ticks to let chunks/entities settle and a fresh frame render before grabbing. */
    private static final int SETTLE_TICKS = 12;

    /** Ticks to wait after teleporting + triggering the build for the scene to load and sync. */
    private static final int BUILD_WARMUP_TICKS = 120;

    /** Ticks to wait after teleporting into a planet dimension (cross-dim load + chunk gen + summons). */
    private static final int PLANET_WARMUP_TICKS = 100;

    /** Deterministic flat backdrop (data/nerospace/dimension/capture.json) + the fixed build origin in it. */
    private static final String CAPTURE_DIMENSION = "nerospace:capture";
    private static final int ORIGIN_X = 0;
    private static final int ORIGIN_Y = 64;
    private static final int ORIGIN_Z = 0;

    /**
     * One framed capture. {@code setup} = server commands run before this shot (teleport, build,
     * summon…); {@code warmup} = ticks to wait after setup for the scene to load/sync; {@code camera}/
     * {@code target} = the pose ({@code null} poses → "keep current view").
     */
    private record Shot(String name, java.util.List<String> setup, java.util.List<String> build,
            int warmup, @Nullable Vec3 camera, @Nullable Vec3 target) {
    }

    private enum Phase { WARMUP, MOVE, SETTLE, SHOOT }

    private static final Deque<Shot> QUEUE = new ArrayDeque<>();
    private static boolean running;
    private static boolean hudWasHidden;
    @Nullable
    private static CloudStatus cloudsWere;
    private static Phase phase = Phase.MOVE;
    private static int warmup;
    private static int settle;
    @Nullable
    private static Shot current;

    private GalleryCaptureHarness() {
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("nsgallery")
                        .then(Commands.literal("capture")
                                .executes(ctx -> startCapture("noon"))
                                .then(Commands.argument("time", StringArgumentType.word())
                                        .executes(ctx -> startCapture(StringArgumentType.getString(ctx, "time")))))
                        .then(Commands.literal("planets")
                                .executes(ctx -> startPlanets("noon"))
                                .then(Commands.argument("time", StringArgumentType.word())
                                        .executes(ctx -> startPlanets(StringArgumentType.getString(ctx, "time")))))
                        .then(Commands.literal("shot")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> shotHere(StringArgumentType.getString(ctx, "name"))))));
    }

    private static int startCapture(String time) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return 0;
        }
        if (running) {
            mc.player.sendSystemMessage(Component.literal("Gallery capture already running."));
            return 0;
        }
        java.util.List<Shot> shots = new java.util.ArrayList<>(buildShots(ORIGIN_X, ORIGIN_Y, ORIGIN_Z));
        // The first shot carries the one-time setup: teleport into the flat capture dimension, (re)build
        // the gallery there, and freeze the environment so every rerun is framed + lit identically.
        // These are server commands (they need cheats — the creative gallery world has them).
        java.util.List<String> setup = java.util.List.of(
                "execute in " + CAPTURE_DIMENSION + " run tp @s "
                        + (ORIGIN_X + 0.5) + " " + ORIGIN_Y + " " + (ORIGIN_Z + 0.5),
                "nerospace gallery clear", // no-op first run; stops reruns stacking
                "nerospace gallery",
                "gamerule advance_time false",   // 26.1 renamed doDaylightCycle → advance_time
                "gamerule advance_weather false", // …and doWeatherCycle → advance_weather
                "time set " + time,               // capture dim is overworld-type → has a clock
                "weather clear");
        Shot first = shots.get(0);
        shots.set(0, new Shot(first.name(), setup, java.util.List.of(), BUILD_WARMUP_TICKS,
                first.camera(), first.target()));

        QUEUE.clear();
        QUEUE.addAll(shots);
        begin(mc);
        mc.player.sendSystemMessage(Component.literal("Gallery capture: building scene, time=" + time + ", "
                + shots.size() + " shots → screenshots/nerospace/ (HUD hidden)."));
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
                + " shots across dimensions, time=" + time + " → screenshots/nerospace/ (HUD hidden)."));
        return Command.SINGLE_SUCCESS;
    }

    private static int shotHere(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || running) {
            return 0;
        }
        QUEUE.clear();
        QUEUE.add(new Shot(name, java.util.List.of(), java.util.List.of(), 0, null, null)); // grab the current view as-is
        begin(mc);
        return Command.SINGLE_SUCCESS;
    }

    private static void begin(Minecraft mc) {
        // screenshots/ is created by Screenshot.grab; the nerospace/ subfolder is not, so make it.
        new File(mc.gameDirectory, "screenshots/nerospace").mkdirs();
        hudWasHidden = mc.options.hideGui;
        mc.options.hideGui = true;
        cloudsWere = mc.options.cloudStatus().get();
        mc.options.cloudStatus().set(CloudStatus.OFF); // clouds scroll with game time → freeze them out of frame
        running = true;
        phase = Phase.MOVE; // the first shot's setup carries any teleport/build; warmup is per-shot now
        current = null;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
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
                for (String cmd : shot.setup()) { // pre-warmup: teleport / gamerules / time / summon
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
    private static void applyPose(LocalPlayer player, @Nullable Shot shot) {
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
        RenderTarget target = mc.getMainRenderTarget();
        Screenshot.grab(mc.gameDirectory, "nerospace/" + name + ".png", target, 1,
                msg -> {
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(msg);
                    }
                });
    }

    private static void finish(Minecraft mc) {
        mc.options.hideGui = hudWasHidden;
        if (cloudsWere != null) {
            mc.options.cloudStatus().set(cloudsWere);
        }
        running = false;
        current = null;
        QUEUE.clear();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("Gallery capture done — see screenshots/nerospace/."));
        }
    }

    /**
     * Explicit per-cluster vantages. Clusters sit on a ring ~48 blocks out (mirror of the rotunda in
     * NerospaceCommands): rockets N, machines S, blocks E, pipes W, creatures SE, suits NW. Each camera
     * is hand-shaped for its subject (tall vs thin-strip vs row), so they're not uniform. Coordinates
     * mirror the cluster bases in NerospaceCommands — tune the two together.
     */
    private static java.util.List<Shot> buildShots(int ox, int oy, int oz) {
        java.util.List<String> none = java.util.List.of();
        java.util.List<Shot> shots = new java.util.ArrayList<>();
        // Rockets (N, tall): low camera looking slightly up.
        shots.add(new Shot("rockets", none, none, 0,
                new Vec3(ox - 0.5, oy + 3, oz - 26), new Vec3(ox - 0.5, oy + 5, oz - 48)));
        // Machines (S, thin strip): 45° top-down, pulled ~10 closer (camera 12 out + 12 up over the strip).
        shots.add(new Shot("machines", none, none, 0,
                new Vec3(ox, oy + 13, oz + 36), new Vec3(ox, oy + 1, oz + 48)));
        // Blocks (E, grid): raised, looking down.
        shots.add(new Shot("blocks", none, none, 0,
                new Vec3(ox + 30, oy + 9, oz), new Vec3(ox + 48, oy + 2, oz)));
        // Pipes (W, rows): raised, looking down so the 4 resource rows read.
        shots.add(new Shot("pipes", none, none, 0,
                new Vec3(ox - 34, oy + 10, oz), new Vec3(ox - 48, oy + 1, oz)));
        // Creatures (SE, row along +X): over-the-shoulder — low, near the west end, looking E down the line.
        shots.add(new Shot("creatures", none, none, 0,
                new Vec3(ox + 14, oy + 4, oz + 33), new Vec3(ox + 44, oy + 2, oz + 34)));
        // Suits (NW, row along +X, stands face ~south): close, head-on from the south.
        shots.add(new Shot("suits", none, none, 0,
                new Vec3(ox - 33, oy + 3, oz - 26), new Vec3(ox - 34, oy + 2.5, oz - 34)));
        // Quarry landmark-only display (NE): the L of three landmarks + their projected lasers.
        shots.add(new Shot("quarry_landmarks", none, none, 0,
                new Vec3(ox + 31, oy + 4, oz - 30), new Vec3(ox + 31, oy + 2, oz - 38)));
        // Operating quarry (NE, further out): raised + angled to look INTO the pit and read the
        // glowing frame, the moving drill head and the power hookup.
        shots.add(new Shot("quarry_operating", none, none, 0,
                new Vec3(ox + 44, oy + 9, oz - 28), new Vec3(ox + 42, oy - 2, oz - 39)));
        // Solar arrays (SW): raised, looking south down the cluster so the front-row single units AND
        // the seam-joined multi-unit fields behind them (plus the cabled connector) read together.
        shots.add(new Shot("solar", none, none, 0,
                new Vec3(ox - 42, oy + 9, oz + 28), new Vec3(ox - 42, oy + 1, oz + 42)));
        return shots;
    }

    /**
     * Planet/dimension vistas (§9.4). Each shot teleports into a planet dimension, pins time/weather,
     * summons frozen signature mobs, then frames a vista; the terraform pair shows one patch barren
     * then greened. Reproducible ONLY in a fixed-seed capture world — planet terrain is seed-dependent.
     *
     * <p>WARNING: the coordinates here are PLACEHOLDERS. Terrain height/features depend on the world
     * seed, so tune cam/target/summon coords to your chosen capture seed (same run→look→nudge loop as
     * the gallery). Bad coords just mean a poorly-framed shot, not a crash.
     */
    private static java.util.List<Shot> buildPlanetShots(String time) {
        java.util.List<Shot> shots = new java.util.ArrayList<>();
        // Greenxertz has a clock (overworld-type) → time set works. Cindara/Glacira/Station use the
        // "space" dimension type (END starfield, NO clock), so time set must be skipped there — they're
        // dark by design. The signature mobs are summoned pre-warmup so they fall to the surface.
        // Each mob shot kills its prior summons first (NoAI mobs are persistent → they'd stack on rerun).
        shots.add(planetShot("greenxertz_vista", "nerospace:greenxertz", time, true,
                new Vec3(8.5, 96, 8.5), new Vec3(28, 88, 28), java.util.List.of(
                        "kill @e[type=nerospace:xertz_stalker]",
                        "kill @e[type=nerospace:quartz_crawler]",
                        "summon nerospace:xertz_stalker 26 90 26 {NoAI:1b,PersistenceRequired:1b}",
                        "summon nerospace:quartz_crawler 30 90 30 {NoAI:1b,PersistenceRequired:1b}"),
                java.util.List.of()));
        shots.add(planetShot("cindara_basin", "nerospace:cindara", time, false,
                new Vec3(8.5, 96, 8.5), new Vec3(28, 88, 28), java.util.List.of(
                        "kill @e[type=nerospace:cinder_stalker]",
                        "kill @e[type=nerospace:ember_strutter]",
                        "summon nerospace:cinder_stalker 26 90 26 {NoAI:1b,PersistenceRequired:1b}",
                        "summon nerospace:ember_strutter 30 90 30 {NoAI:1b,PersistenceRequired:1b}"),
                java.util.List.of()));
        shots.add(planetShot("glacira_frost", "nerospace:glacira", time, false,
                new Vec3(8.5, 96, 8.5), new Vec3(28, 88, 28), java.util.List.of(
                        "kill @e[type=nerospace:frost_strider]",
                        "kill @e[type=nerospace:woolly_drift]",
                        "summon nerospace:frost_strider 26 90 26 {NoAI:1b,PersistenceRequired:1b}",
                        "summon nerospace:woolly_drift 30 90 30 {NoAI:1b,PersistenceRequired:1b}"),
                java.util.List.of()));
        // Orbital station is an empty space dimension until something is built — place a small platform
        // (floor + two walls + a Station Core) at a fixed spot and shoot it against the starfield. The
        // placement runs POST-warmup (build list) so the chunks are loaded ("position not loaded" else).
        shots.add(planetShot("orbital_station", "nerospace:station", time, false,
                new Vec3(-6.5, 71, -6.5), new Vec3(3, 65, 3), java.util.List.of(),
                java.util.List.of(
                        "fill -1 64 -1 7 67 7 minecraft:air", // clear any prior platform before rebuilding
                        "fill 0 64 0 6 64 6 nerospace:station_floor",
                        "fill 0 65 0 6 65 0 nerospace:station_wall",
                        "fill 0 65 0 0 65 6 nerospace:station_wall",
                        "setblock 3 65 3 nerospace:station_core")));
        // Terraform before/after on CINDARA (the barren volcanic-ash world). AFTER converts a patch to
        // the nerospace:terraformed_meadow biome (the neon-emerald "living" palette) + grass surface,
        // two trees and a Meadow Loper. Block ops run post-warmup. NOTE: Cindara is a no-sun space dim,
        // so the patch will be dim; if you want a bright "living" shot say so and I'll stage it on
        // sunlit Greenxertz instead. by is a guess at the surface — tune to the seed.
        int bx = 60;
        int by = 70;
        int bz = 60;
        Vec3 tcam = new Vec3(bx - 6, by + 7, bz - 6);
        Vec3 ttgt = new Vec3(bx + 6, by, bz + 6);
        // A controlled flat patch built fresh each run (so reruns don't leave a dug pit / stale biome /
        // floating mismatch from guessing Cindara's surface): clear the column to air, set the BIOME,
        // then lay a 2-deep base topped with the surface block. BEFORE = barren coarse-dirt on the native
        // (cindara) biome; AFTER = grass on the neon terraformed_meadow biome, with trees + a Meadow Loper.
        int x2 = bx + 12;
        int z2 = bz + 12;
        String clearPatch = "fill " + (bx - 2) + " " + by + " " + (bz - 2) + " " + (bx + 14) + " "
                + (by + 12) + " " + (bz + 14) + " minecraft:air";
        String base = "fill " + bx + " " + (by - 1) + " " + bz + " " + x2 + " " + (by - 1) + " " + z2
                + " minecraft:dirt";
        shots.add(planetShot("terraform_before", "nerospace:cindara", time, false, tcam, ttgt,
                java.util.List.of(), java.util.List.of(
                        clearPatch,
                        "kill @e[type=nerospace:meadow_loper]",
                        "fillbiome " + bx + " " + (by - 1) + " " + bz + " " + x2 + " " + (by + 10) + " " + z2
                                + " nerospace:cindara", // reset biome → barren (else the prior green lingers)
                        base,
                        "fill " + bx + " " + by + " " + bz + " " + x2 + " " + by + " " + z2 + " minecraft:coarse_dirt")));
        shots.add(planetShot("terraform_after", "nerospace:cindara", time, false, tcam, ttgt,
                java.util.List.of(), java.util.List.of(
                        clearPatch,
                        "kill @e[type=nerospace:meadow_loper]",
                        "fillbiome " + bx + " " + (by - 1) + " " + bz + " " + x2 + " " + (by + 10) + " " + z2
                                + " nerospace:terraformed_meadow",
                        base,
                        "fill " + bx + " " + by + " " + bz + " " + x2 + " " + by + " " + z2 + " minecraft:grass_block",
                        "place feature minecraft:oak " + (bx + 3) + " " + (by + 1) + " " + (bz + 3),
                        "place feature minecraft:oak " + (bx + 9) + " " + (by + 1) + " " + (bz + 8),
                        "summon nerospace:meadow_loper " + (bx + 6) + " " + (by + 1) + " " + (bz + 6)
                                + " {NoAI:1b,PersistenceRequired:1b}")));
        return shots;
    }

    /**
     * Build a planet shot. {@code summons} run pre-warmup (so mobs fall to the surface during the wait);
     * {@code builds} run post-warmup (block placement needs the chunks loaded). {@code hasClock} gates
     * {@code time set} — space dimensions have no clock and reject it.
     */
    private static Shot planetShot(String name, String dim, String time, boolean hasClock, Vec3 cam, Vec3 tgt,
            java.util.List<String> summons, java.util.List<String> builds) {
        java.util.List<String> setup = new java.util.ArrayList<>();
        setup.add("execute in " + dim + " run tp @s " + cam.x + " " + cam.y + " " + cam.z);
        setup.add("gamerule advance_time false");    // 26.1: replaces doDaylightCycle
        setup.add("gamerule advance_weather false");  // 26.1: replaces doWeatherCycle
        setup.add("weather clear");
        if (hasClock) {
            setup.add("time set " + time); // only dims with a clock accept this
        }
        setup.addAll(summons);
        return new Shot(name, setup, builds, PLANET_WARMUP_TICKS, cam, tgt);
    }
}
