package za.co.neroland.nerospace.fabric.gametest;

import java.nio.file.Path;
import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.rocket.RocketEntity;

/**
 * DEV ONLY: captures the Nerospace gallery set, one 1920x1080 PNG per {@link Shot}, in a throwaway singleplayer world
 * (fixed seed, creative, cheats on). Run it with {@code ./gradlew :fabric:26.3:captureGallery}; it lives in the Fabric
 * 26.3 {@code gametest} source set, so none of it is in the release jar.
 *
 * <p>Each shot reuses the mod's own showcase tooling instead of searching terrain at runtime:
 * {@code /nerospace gallery sky <world> <state> here} sets the world's clock phase and weather to a named sky state and
 * moves the player to the mod's deterministic viewing spot on that world (its centre column; every shot arrives from
 * another world, so it is always the centre). Optional build commands then stage a small scene around
 * that spot, and the camera is pinned to the spot plus a fixed offset with a fixed yaw and pitch. With the fixed seed
 * the same world generates every run, so each run yields the same frames.
 *
 * <p>The player is the gametest's placeholder dev account; the HUD, hand and chat are hidden, no player names or
 * nametags are in frame, and nothing about the player is logged.
 */
public final class GalleryCaptureClientGameTest implements FabricClientGameTest {

    /** Ticks to let entities, block entities and the sky settle after the camera is pinned. */
    private static final int SETTLE_TICKS = 60;

    /**
     * Render distance for the frame itself. The gametest waits (which need every chunk in a full square) run at the
     * gametest default of 5; the distance is then raised for the vista and given {@link #VISTA_TICKS} to fill in.
     */
    private static final int RENDER_DISTANCE = 12;
    private static final int BASE_RENDER_DISTANCE = 5;
    private static final int VISTA_TICKS = 160;

    /**
     * One gallery frame.
     *
     * @param slug   file name part ({@code nerospace_gallery_<nn>_<slug>.png})
     * @param world  the world's lang id for {@code /nerospace gallery sky}, and its dimension is {@code nerospace:<world>}
     * @param state  the sky state (see the mod's {@code GallerySkyStates} table)
     * @param build  server commands run after arrival, executed at the player's feet ({@code ~} = the viewing spot)
     * @param offset camera offset from the viewing spot (feet position)
     * @param yaw    camera yaw (90 west, -90 east, 180 north, 0 south)
     * @param pitch  camera pitch (positive looks down)
     */
    record Shot(String slug, String world, String state, List<String> build, Vec3 offset,
            float yaw, float pitch) {
    }

    /**
     * The gallery, in CurseForge order. Retune poses here. Poses were picked from a sweep of candidate frames
     * (four headings, low and high) at each viewing spot with the fixed seed.
     */
    static final List<Shot> SHOTS = List.of(
            // 1. Orbit hero: a Tier 2 rocket on its 3x3 pad on the orbital station, looking east into the sunrise.
            new Shot("orbit_rocket", "station", "sunrise", List.of(
                    "fill ~-3 ~-1 ~-4 ~12 ~-1 ~4 nerospace:station_floor",
                    "fill ~5 ~ ~-1 ~7 ~ ~1 nerospace:rocket_launch_pad",
                    "setblock ~4 ~ ~2 nerospace:launch_gantry",
                    "setblock ~9 ~ ~-2 nerospace:fuel_tank",
                    "kill @e[type=nerospace:rocket,distance=..32]",
                    "summon nerospace:rocket ~6 ~0.1875 ~ {Tier:1}"),
                    new Vec3(-4, 1.0, -3.5), -70.0F, -8.0F),
            // 2. Luna: Earthrise in the east over the grey regolith.
            new Shot("luna_earthrise", "luna", "earthrise", List.of(), Vec3.ZERO, -90.0F, -4.0F),
            // 3. Mars: the orange plains at noon.
            new Shot("mars_plains", "mars", "noon", List.of(), new Vec3(0, 2.0, 0), 90.0F, 6.0F),
            // 4. Greenxertz: green hazy terrain at noon, looking west (the centre column is on a coast).
            new Shot("greenxertz_noon", "greenxertz", "noon", List.of(), new Vec3(0, 2.0, 0), 90.0F, 4.0F),
            // 5. Cindara at the eclipse phase: dark basalt and ore against the glowing eastern horizon.
            new Shot("cindara_eclipse", "cindara", "eclipse", List.of(), new Vec3(0, 2.0, 0), -90.0F, 4.0F),
            // 6. Glacira at the aurora phase (night): the ice sheet, looking south.
            new Shot("glacira_aurora", "glacira", "aurora", List.of(), new Vec3(0, 2.0, 0), 0.0F, 4.0F),
            // 7. Nyxara: the crystal terminator band, looking west to the low sun.
            new Shot("nyxara_band", "nyxara", "band", List.of(), new Vec3(0, 2.0, 0), 90.0F, 4.0F),
            // 8. A compact machine line on a Mars outpost deck (replaces the flat showcase floor). Built at the same
            // viewing spot as shot 3, after it, south of the spot (shot 3 looks west).
            new Shot("mars_outpost", "mars", "noon", List.of(
                    "fill ~-4 ~-1 ~2 ~6 ~-1 ~7 nerospace:station_floor",
                    "fill ~-4 ~ ~2 ~6 ~5 ~7 minecraft:air",
                    "setblock ~-3 ~ ~5 nerospace:combustion_generator",
                    "setblock ~-2 ~ ~5 nerospace:universal_pipe",
                    "setblock ~-1 ~ ~5 nerospace:nerosium_grinder",
                    "setblock ~0 ~ ~5 nerospace:universal_pipe",
                    "setblock ~1 ~ ~5 nerospace:fuel_refinery",
                    "setblock ~2 ~ ~5 nerospace:universal_pipe",
                    "setblock ~3 ~ ~5 nerospace:fuel_tank",
                    "setblock ~5 ~ ~5 nerospace:oxygen_generator",
                    "setblock ~-4 ~ ~7 nerospace:alien_lamp",
                    "setblock ~6 ~ ~7 nerospace:alien_lamp",
                    "fill ~-3 ~ ~7 ~4 ~ ~7 nerospace:solar_panel"),
                    new Vec3(0.5, 0.8, 1.5), 0.0F, 12.0F),
            // 9. Europa (2.4.0) at the eclipse phase: the ice plains under Jupiter, looking east. Pose not yet swept.
            new Shot("europa_eclipse", "europa", "eclipse", List.of(), new Vec3(0, 2.0, 0), -90.0F, 4.0F));

    @Override
    public void runTest(ClientGameTestContext context) {
        if ("demo".equals(System.getProperty("nerospace.gametest"))) {
            return; // a captureDemo run: only DemoWorldClientGameTest acts
        }
        // Creative + cheats: /nerospace gallery is player-only, so its commands are sent by the client player.
        try (TestSingleplayerContext world = context.worldBuilder()
                .adjustSettings(settings -> {
                    settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    settings.setAllowCommands(true);
                })
                .create()) {
            world.getConnection().waitForChunksRender();
            context.runOnClient(mc -> {
                mc.options.bobView().set(false);
                if (!mc.gui.hud.isHidden()) {
                    mc.gui.hud.toggle(); // hides the HUD, hand and chat
                }
            });
            world.getServer().runCommand("weather clear");

            Path out = outputDir();
            int index = 1;
            for (Shot shot : SHOTS) {
                capture(context, world, shot, index++, out);
            }
        }
    }

    private static void capture(ClientGameTestContext context, TestSingleplayerContext world, Shot shot, int index,
            Path out) {
        String dimension = "nerospace:" + shot.world();
        String sky = "nerospace gallery sky " + shot.world() + " " + shot.state() + " here";
        context.runOnClient(mc -> mc.player.connection.sendCommand(sky));
        world.getConnection().waitForServerboundPackets();
        waitForDimension(context, dimension);
        context.waitTicks(10);
        for (String cmd : shot.build()) {
            world.getServer().runCommand("execute as @a at @s run " + cmd);
        }
        context.waitTicks(5);

        Vec3 feet = context.computeOnClient(mc -> mc.player.position());
        Vec3 cam = feet.add(shot.offset());
        context.runOnClient(mc -> pin(mc, cam, shot.yaw(), shot.pitch()));
        waitForChunks(world);
        for (int i = 0; i < SETTLE_TICKS; i++) {
            context.waitTick();
            context.runOnClient(mc -> pin(mc, cam, shot.yaw(), shot.pitch()));
        }
        waitForChunks(world);
        context.runOnClient(mc -> mc.options.renderDistance().set(RENDER_DISTANCE));
        for (int i = 0; i < VISTA_TICKS; i++) {
            context.waitTick();
            context.runOnClient(mc -> pin(mc, cam, shot.yaw(), shot.pitch()));
        }
        if (shot.slug().contains("rocket")) {
            context.runOnClient(GalleryCaptureClientGameTest::reportRocketOutline);
        }

        String name = String.format("nerospace_gallery_%02d_%s", index, shot.slug());
        Path written = context.takeScreenshot(TestScreenshotOptions.of(name)
                .disableCounterPrefix()
                .withSize(1920, 1080)
                .withDestinationDir(out));
        System.out.println("[nerospace-gallery] wrote " + written.getFileName());
        context.runOnClient(mc -> mc.options.renderDistance().set(BASE_RENDER_DISTANCE));
    }

    /** Waits for the chunks around the camera to render; a timeout is logged and the frame is taken anyway. */
    private static void waitForChunks(TestSingleplayerContext world) {
        try {
            world.getConnection().waitForChunksRender(400); // 20 s
        } catch (AssertionError e) {
            System.out.println("[nerospace-gallery] chunks did not finish rendering in time; capturing anyway");
        }
    }

    /** Waits (max 30 s) until the client is in {@code dimension} with no loading screen open. */
    private static void waitForDimension(ClientGameTestContext context, String dimension) {
        context.waitFor(mc -> mc.level != null && mc.player != null
                && dimension.equals(mc.level.dimension().identifier().toString()), 600);
        context.waitForScreen(null);
    }

    /** Holds the camera (the player) still at {@code cam}, flying so gravity cannot move it. */
    private static void pin(Minecraft mc, Vec3 cam, float yaw, float pitch) {
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        player.getAbilities().flying = true;
        player.snapTo(cam.x, cam.y, cam.z, yaw, pitch);
        player.setDeltaMovement(Vec3.ZERO);
    }

    /** Diagnostic only (no player data): is the rocket drawn with the vanilla glowing outline? */
    private static void reportRocketOutline(Minecraft mc) {
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof RocketEntity) {
                System.out.println("[nerospace-gallery] rocket outline check: glowingTag=" + e.hasGlowingTag()
                        + " currentlyGlowing=" + e.isCurrentlyGlowing()
                        + " appearsGlowing=" + mc.shouldEntityAppearGlowing(e));
            }
        }
    }

    private static Path outputDir() {
        String prop = System.getProperty("nerospace.gallery.outDir");
        if (prop != null && !prop.isBlank()) {
            return Path.of(prop);
        }
        return FabricLoader.getInstance().getGameDir().resolve("screenshots").resolve("gallery");
    }
}
