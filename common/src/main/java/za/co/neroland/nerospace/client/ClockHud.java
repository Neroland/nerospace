package za.co.neroland.nerospace.client;

import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import za.co.neroland.nerospace.client.sky.SkyProfile;
import za.co.neroland.nerospace.client.sky.SkyProfiles;
import za.co.neroland.nerospace.client.ui.ClockDial;
import za.co.neroland.nerospace.client.ui.HudLayout;
import za.co.neroland.nerospace.client.ui.PlanetAccents;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;

/**
 * The planet clock dial on the HUD: a {@link ClockDial} above the oxygen gauge's status lines on any Nerospace
 * planet with a day cycle, lit by the planet's daylight curve, with the eclipse window as a dotted arc. When the
 * phase name changes (sunrise, day, sunset, night, eclipse) it shows for two seconds and fades out. Drawn from
 * {@link OxygenHud#render}, so it shares that layer's registration on every loader. Client-only state: nothing is
 * stored or sent.
 */
public final class ClockHud {

    private static final int SUBTLE = 0x8DA0B4;
    private static final long LABEL_MS = 2000L;
    private static final long FADE_MS = 500L;

    private static String lastKey = "";
    private static SkyProfile cachedProfile;
    private static List<ClockDial.Window> cachedWindows = List.of();
    private static int cachedSun = 0xFFFFFFFF;
    private static long changedAt;

    private ClockHud() {
    }

    /** Draws the dial for the HUD anchor {@code (x, y)} (the oxygen icon's top-left). */
    public static void render(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer player, int x, int y) {
        if (!NerospaceConfig.hudClockDial()) {
            return;
        }
        Optional<PlanetDefinition> found = Planets.byDimension(player.level().dimension());
        if (found.isEmpty() || found.get() == Planets.STATION || !ClientPlanetClock.known(player.level())
                || !ClientPlanetClock.hasDayLength()) {
            return;
        }
        PlanetDefinition planet = found.get();
        double phase = ClientPlanetClock.phase(player.level());
        SkyProfile profile = SkyProfiles.get(planet.traits().skyProfile());
        if (profile != cachedProfile) {
            // Rebuilt only when the profile changes (another planet or a resource reload), not every frame.
            cachedWindows = ClockDial.windowsFor(planet, profile);
            cachedSun = ClockDial.sunArgb(profile);
            cachedProfile = profile;
        }
        List<ClockDial.Window> windows = cachedWindows;
        int sun = cachedSun;

        int scale = NerospaceConfig.hudClockDialScale();
        int[] d = HudLayout.dial(x, y, scale);
        ClockDial.draw(g, d[0], d[1], HudLayout.dialRadius(scale), phase, PlanetAccents.of(planet.dimension()), sun,
                planet.traits().fixedClock(), windows, planet.daylight());

        String key = ClockDial.phaseKey(phase, windows);
        long now = System.currentTimeMillis();
        if (!key.equals(lastKey)) {
            boolean first = lastKey.isEmpty();
            lastKey = key;
            changedAt = first ? now - LABEL_MS : now; // no label pop on login / dimension change
        }
        long age = now - changedAt;
        if (age < LABEL_MS) {
            long left = LABEL_MS - age;
            int alpha = left >= FADE_MS ? 255 : Math.max(8, (int) (255 * left / FADE_MS));
            Component label = Component.translatable(key);
            int[] r = HudLayout.phaseLabel(x, y, scale, mc.font.width(label), g.guiWidth());
            g.text(mc.font, label, r[0], r[1], (alpha << 24) | SUBTLE, true);
        }
    }
}
