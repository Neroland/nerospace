package za.co.neroland.nerospace.fabric.gametest;

import java.nio.file.Path;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerolandcore.demo.DemoClient;
import za.co.neroland.nerolandcore.demo.DemoPickerScreen;

/**
 * DEV ONLY: drives the Neroland Core "Nero Mods Demo" flow end to end with Nerospace installed and screenshots it:
 * the button on the singleplayer world list, the picker, the built hub, both exhibits, and every Nerospace outpost
 * reached through its real walk-on travel pad (and back through the outpost's return pad). Run it with
 * {@code ./gradlew :fabric:26.3:captureDemo}; it only acts when that task sets {@code nerospace.gametest=demo}, and it
 * lives in the Fabric 26.3 {@code gametest} source set, so none of it is in the release jar.
 *
 * <p>The player is the gametest's placeholder dev account; the HUD, hand and chat are hidden and nothing about the
 * player is logged.
 */
public final class DemoWorldClientGameTest implements FabricClientGameTest {

    private static final int KEY_ENTER = 257;

    /** Plot geometry, mirrored from Core's DemoLayout with the Core (13 wide) and Nerospace (36 x 31) plots. */
    private static final int NEROSPACE_MIN_X = 34;
    private static final int NEROSPACE_MIN_Z = -15;
    private static final int FLOOR_Y = 64;

    /** Outposts in travel-pad order (Nerospace's NerospaceDemoModule.OUTPOSTS). */
    private static final String[] WORLDS = {"station", "luna", "mars", "cindara", "glacira", "greenxertz", "nyxara"};

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!"demo".equals(System.getProperty("nerospace.gametest"))) {
            return; // the gallery capture run shares this source set
        }
        Path out = outputDir();
        context.runOnClient(mc -> mc.options.bobView().set(false));

        // 1. The singleplayer world list with the Nero Mods Demo button.
        context.setScreen(() -> new SelectWorldScreen(new TitleScreen()));
        context.waitFor(mc -> demoButton(mc.gui.screen()) != null, 400);
        context.waitTicks(10);
        shot(context, out, "demo_01_world_list");
        report(context, "world list screen: ");

        // 2. The picker.
        context.runOnClient(mc -> demoButton(mc.gui.screen()).onPress(new KeyEvent(KEY_ENTER, 0, 0)));
        context.waitForScreen(DemoPickerScreen.class);
        context.waitTicks(10);
        shot(context, out, "demo_02_picker");
        Path native2 = context.takeScreenshot(TestScreenshotOptions.of("demo_02b_picker_window_size")
                .disableCounterPrefix().withDestinationDir(out));
        System.out.println("[nerospace-demo] wrote " + native2.getFileName());

        // 3. Create: the picker drives vanilla's Create World screen and the world loads.
        context.runOnClient(mc -> press(mc.gui.screen(), "demo.nerolandcore.picker.create"));
        context.waitFor(mc -> mc.level != null && mc.player != null && mc.gui.screen() == null, 20 * 180);
        context.runOnClient(mc -> {
            if (!mc.gui.hud.isHidden()) {
                mc.gui.hud.toggle();
            }
        });
        context.waitTicks(200);
        Vec3 spawn = context.computeOnClient(mc -> mc.player.position());
        System.out.println("[nerospace-demo] spawned at " + spawn + " in "
                + context.computeOnClient(mc -> mc.level.dimension().identifier().toString()));
        shot(context, out, "demo_03_hub_spawn_view");

        // 4. Overviews of the hub and both plots (camera pinned, flying).
        pinFor(context, new Vec3(-6.5, 74, -14.5), new Vec3(20, 65, 0), 60);
        shot(context, out, "demo_04_hub_and_core_plot");
        pinFor(context, new Vec3(30.5, 88, -30.5), new Vec3(52, 65, 0), 80);
        shot(context, out, "demo_05_nerospace_plot");
        pinFor(context, new Vec3(49.5, 69, 4.5), new Vec3(50, 68, -9), 60);
        shot(context, out, "demo_06_nerospace_launch_row");
        pinFor(context, new Vec3(51.5, 70, -1.5), new Vec3(52, 65, 9), 60);
        shot(context, out, "demo_06b_nerospace_machines_and_pads");

        // 5. Every outpost through its real travel pad, and back through the return pad.
        for (int i = 0; i < WORLDS.length; i++) {
            String world = WORLDS[i];
            int padX = NEROSPACE_MIN_X + 4 + i * 4;
            int padZ = NEROSPACE_MIN_Z + 25;
            context.runOnClient(mc -> {
                mc.player.getAbilities().flying = false;
                mc.player.onUpdateAbilities();
            });
            // Dropped onto the plate from just above it: a pressure plate reacts to an entity moving into it.
            context.runOnClient(mc -> mc.player.connection.sendCommand(
                    String.format(java.util.Locale.ROOT, "tp @s %d.5 %d.5 %d.5 -90 0", padX, FLOOR_Y + 2, padZ)));
            boolean arrived = waitDimension(context, "nerospace:" + world, 100);
            if (!arrived) {
                BlockPos platePos = new BlockPos(padX, FLOOR_Y + 1, padZ);
                String states = context.computeOnClient(mc -> mc.level.getBlockState(platePos) + " over "
                        + mc.level.getBlockState(platePos.below()) + "; player at " + mc.player.position()
                        + " onGround=" + mc.player.onGround() + " flying=" + mc.player.getAbilities().flying);
                System.out.println("[nerospace-demo] plate did not trigger for " + world + ": " + states);
                continue;
            }
            context.waitTicks(160);
            Vec3 at = context.computeOnClient(mc -> mc.player.position());
            System.out.println("[nerospace-demo] arrived on " + world + " at " + at);
            shot(context, out, String.format("demo_%02d_outpost_%s", 7 + i, world));
            // The return pad is two blocks behind the arrival facing.
            BlockPos back = context.computeOnClient(mc -> {
                LocalPlayer p = mc.player;
                return p.blockPosition().relative(Direction.fromYRot(p.getYRot()).getOpposite(), 2);
            });
            context.runOnClient(mc -> mc.player.connection.sendCommand(String.format(java.util.Locale.ROOT,
                    "tp @s %.1f %.1f %.1f", back.getX() + 0.5D, back.getY() + 1.5D, back.getZ() + 0.5D)));
            boolean home = waitDimension(context, "minecraft:overworld", 400);
            System.out.println("[nerospace-demo] return pad from " + world + (home ? " worked" : " did NOT work"));
            if (!home) {
                context.runOnClient(mc -> mc.player.connection.sendCommand("execute in minecraft:overworld run tp @s 0.5 65 0.5"));
                waitDimension(context, "minecraft:overworld", 400);
            }
            context.waitTicks(40);
        }

        context.runOnClient(mc -> mc.disconnectFromWorld(Component.literal("Demo capture finished")));
        context.waitForScreen(TitleScreen.class);
        context.waitFor(mc -> mc.getSingleplayerServer() == null && mc.level == null, 20 * 60);
        context.waitTicks(40);
        context.setScreen(TitleScreen::new);
        context.waitTicks(5);
    }

    private static boolean waitDimension(ClientGameTestContext context, String dimension, int ticks) {
        try {
            context.waitFor(mc -> mc.level != null && mc.player != null && mc.gui.screen() == null
                    && dimension.equals(mc.level.dimension().identifier().toString()), ticks);
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }

    /** Pins the camera at {@code cam} looking at {@code target} for {@code ticks} ticks. */
    private static void pinFor(ClientGameTestContext context, Vec3 cam, Vec3 target, int ticks) {
        Vec3 d = target.subtract(cam);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        for (int i = 0; i < ticks; i++) {
            context.runOnClient(mc -> pin(mc, cam, yaw, pitch));
            context.waitTick();
        }
    }

    private static void pin(Minecraft mc, Vec3 cam, float yaw, float pitch) {
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        player.getAbilities().flying = true;
        player.snapTo(cam.x, cam.y, cam.z, yaw, pitch);
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static Button demoButton(Screen screen) {
        return screen == null ? null : button(screen, DemoClient.BUTTON_KEY);
    }

    private static Button button(Screen screen, String key) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof Button b && b.getMessage().getContents() instanceof TranslatableContents t
                    && key.equals(t.getKey())) {
                return b;
            }
        }
        return null;
    }

    private static void press(Screen screen, String key) {
        Button b = screen == null ? null : button(screen, key);
        if (b == null) {
            throw new AssertionError("button " + key + " not found on " + screen);
        }
        b.onPress(new KeyEvent(KEY_ENTER, 0, 0));
    }

    private static void report(ClientGameTestContext context, String prefix) {
        String name = context.computeOnClient(mc -> mc.gui.screen() == null ? "none"
                : mc.gui.screen().getClass().getSimpleName());
        System.out.println("[nerospace-demo] " + prefix + name);
    }

    private static void shot(ClientGameTestContext context, Path out, String name) {
        Path written = context.takeScreenshot(TestScreenshotOptions.of(name)
                .disableCounterPrefix()
                .withSize(1920, 1080)
                .withDestinationDir(out));
        System.out.println("[nerospace-demo] wrote " + written.getFileName());
    }

    private static Path outputDir() {
        String prop = System.getProperty("nerospace.demo.outDir");
        if (prop != null && !prop.isBlank()) {
            return Path.of(prop);
        }
        return FabricLoader.getInstance().getGameDir().resolve("screenshots").resolve("demo");
    }
}
