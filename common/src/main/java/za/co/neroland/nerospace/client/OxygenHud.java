package za.co.neroland.nerospace.client;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.client.ui.Glyphs;
import za.co.neroland.nerospace.client.ui.HudLayout;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.world.planet.Planets;
import za.co.neroland.nerospace.world.planet.HazardOrderRules;
import za.co.neroland.nerospace.world.planet.PlanetHazards;
import za.co.neroland.nerospace.world.planet.RadiationRules;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.OxygenFieldEvents;
import za.co.neroland.nerospace.world.OxygenManager;

/**
 * Bespoke oxygen / hazard HUD gauge (terraform design §1.7): an O₂ bubble icon + bar drawn just to the
 * right of the hotbar while the player is in an airless Nerospace dimension — the proper readout that
 * replaces the vanilla air-bubble stopgap (which each loader suppresses on these dimensions). The O₂ %,
 * the worn suit tier, and any active / uncountered hazard each stack on their own line above the bar.
 *
 * <p><b>Cross-loader seam.</b> The drawing is pure vanilla ({@link GuiGraphicsExtractor}), so it lives
 * once here in {@code common} and both loaders call {@link #render(GuiGraphicsExtractor)} from their own
 * HUD-overlay hook: NeoForge via {@code RegisterGuiLayersEvent.registerAboveAll(...)} (a
 * {@code GuiLayer}), Fabric via {@code HudElementRegistry.addLast(...)} (a {@code HudElement}). Both
 * interfaces are functional and take {@code (GuiGraphicsExtractor, DeltaTracker)}, so each registers a
 * thin lambda that delegates here.</p>
 *
 * <p><b>Sync note:</b> no bespoke payload is needed. {@link OxygenManager} mirrors the oxygen attachment
 * onto the vanilla air supply every server tick (that mirror is already client-synced), so
 * {@code air / maxAir} IS the oxygen fraction the gauge shows. Suit tier + hazard shield come from the
 * player's (client-synced) armour slots.</p>
 */
public final class OxygenHud {

    private static final Identifier ICON =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/oxygen_hud_icon.png");

    private static final int INK = 0xFF05080D;
    private static final int TROUGH = 0xFF0B1119;
    private static final int O2 = 0xFF3CC8E6;       // cyan
    private static final int LOW = 0xFFE0506A;      // red when low
    private static final int LABEL = 0xFFCFE7FF;
    private static final int TIER1 = 0xFF96AAB4;    // steel — Tier 1 suit badge
    private static final int TIER2 = 0xFFF0C850;    // gold — Tier 2 suit badge
    private static final int HEAT = 0xFFF07830;     // ember — Thermal Suit shield badge
    private static final int COLD = 0xFF78D2F0;     // ice — Cryo Suit shield badge
    private static final int RAD = 0xFFF5C542;      // amber — Rad Lining badge (the planet card's radiation colour)

    private static final int ICON_SIZE = 16;
    private static final int BAR_W = 80;
    private static final int BAR_H = 6;
    /** Intensity bar right of the weather name: 4 cells, pitch 5, plus the 4 px gap. */
    private static final int WEATHER_BAR_W = 23;

    private OxygenHud() {
    }

    /**
     * Draw the gauge for the local player (and the planet clock dial above it); a no-op in creative / spectator /
     * with the GUI hidden. The oxygen gauge only shows on airless dimensions; the dial on any planet with a day.
     */
    public static void render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        // Respect F1 / hide-GUI (see isGuiHidden — read reflectively as the 26.x API has no public path).
        if (isGuiHidden(mc)) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator() || player.getAbilities().instabuild) {
            return;
        }
        // Sit just to the RIGHT of the hotbar, vertically level with it; the labels stack upward.
        int x = HudLayout.anchorX(g.guiWidth());   // right of the hotbar, clamped on-screen
        int y = HudLayout.anchorY(g.guiHeight());  // icon top (~level with the hotbar)
        ClockHud.render(g, mc, player, x, y);

        if (!OxygenFieldEvents.isFieldDimension(player.level().dimension())) {
            return;
        }
        int max = player.getMaxAirSupply();
        if (max <= 0) {
            return;
        }
        int air = Math.max(0, Math.min(max, player.getAirSupply()));
        float frac = (float) air / max;

        int barX = HudLayout.barX(x);   // bar to the right of the icon
        int barY = HudLayout.barY(y);   // bar vertically centred on the icon

        // Icon (16x16 GUI texture).
        g.blit(RenderPipelines.GUI_TEXTURED, ICON, x, y, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

        // Gauge: inked border, dark trough, cyan fill (red when low), top sheen.
        g.fill(barX - 1, barY - 1, barX + BAR_W + 1, barY + BAR_H + 1, INK);
        g.fill(barX, barY, barX + BAR_W, barY + BAR_H, TROUGH);
        int fw = Math.round(BAR_W * frac);
        if (fw > 0) {
            g.fill(barX, barY, barX + fw, barY + BAR_H, frac > 0.3F ? O2 : LOW);
            g.fill(barX, barY, barX + fw, barY + 1, 0x55FFFFFF); // top sheen
        }

        // The hazard that would hurt this player next (first unshielded of radiation, heat, cold at their
        // position) — read once, used by the Geiger glyph and the hazard line.
        Hazard hazardNow = clientHazard(player);

        // Labels stack upward, each on its own line above the bar (at most HudLayout.MAX_LINES).
        int k = 0;
        Component o2 = Component.translatable("hud.nerospace.oxygen", Math.round(frac * 100));
        int line = HudLayout.lineY(y, k++);
        g.text(mc.font, o2, barX, line, LABEL, true);
        // Geiger: radiation on this planet (dim); blinking while it is hurting this player (no Rad Lining).
        int radiation = Planets.byDimension(player.level().dimension()).map(p -> p.traits().radiation()).orElse(0);
        boolean exposed = hazardNow == Hazard.RADIATION;
        if (radiation >= 1 || exposed) {
            boolean on = !exposed || (System.currentTimeMillis() / 500L) % 2L == 0L;
            if (on) {
                Glyphs.draw(g, Glyphs.Glyph.RADIATION, barX + mc.font.width(o2) + 3, line,
                        exposed ? RAD : (0x99 << 24) | (RAD & 0xFFFFFF));
            }
        }

        // Suit badge (armour slots are already client-synced): an active hazard SHIELD outranks the
        // capacity tier on the badge — both axes still apply.
        OxygenManager.SuitTier suit = OxygenManager.suitTier(player);
        OxygenManager.HazardShield shield = OxygenManager.hazardShield(player);
        if (shield != OxygenManager.HazardShield.NONE) {
            boolean heat = shield == OxygenManager.HazardShield.HEAT;
            line = HudLayout.lineY(y, k++);
            int c = heat ? HEAT : COLD;
            Glyphs.draw(g, heat ? Glyphs.Glyph.HEAT : Glyphs.Glyph.COLD, barX - HudLayout.GLYPH_LEFT, line, c);
            g.text(mc.font, Component.translatable(heat ? "hud.nerospace.suit_heat" : "hud.nerospace.suit_cold"), barX, line, c, true);
        } else if (suit != OxygenManager.SuitTier.NONE) {
            boolean t2 = suit == OxygenManager.SuitTier.TIER_2;
            line = HudLayout.lineY(y, k++);
            int c = t2 ? TIER2 : TIER1;
            Glyphs.draw(g, Glyphs.Glyph.SUIT, barX - HudLayout.GLYPH_LEFT, line, c);
            g.text(mc.font, Component.translatable(t2 ? "hud.nerospace.suit_t2" : "hud.nerospace.suit_t1"), barX, line, c, true);
        }
        if (OxygenManager.radLined(player)) {
            line = HudLayout.lineY(y, k++);
            Glyphs.draw(g, Glyphs.Glyph.RADIATION, barX - HudLayout.GLYPH_LEFT, line, RAD);
            g.text(mc.font, Component.translatable("hud.nerospace.suit_rad"), barX, line, RAD, true);
        }

        // Weather: glyph, name and a 4-cell intensity bar (lit cells = intensity, so colour is never alone).
        WeatherEvent event = ClientPlanetWeather.event(player.level());
        if (NerospaceConfig.hudWeatherLine() && event != WeatherEvent.NONE) {
            line = HudLayout.lineY(y, k++);
            int tint = weatherTint(event);
            Glyphs.draw(g, Glyphs.weather(event), barX - HudLayout.GLYPH_LEFT, line, tint);
            Component name = Component.translatable("hud.nerospace.weather." + event.name().toLowerCase(Locale.ROOT));
            int room = g.guiWidth() - 2 - WEATHER_BAR_W - barX;
            name = ellipsis(mc, name, room);
            g.text(mc.font, name, barX, line, tint, true);
            int lit = Math.round(Math.max(0.0F, Math.min(1.0F, ClientPlanetWeather.intensity(player.level()))) * 4.0F);
            int cx0 = barX + mc.font.width(name) + 4;
            for (int i = 0; i < 4; i++) {
                int cx = cx0 + i * 5;
                g.fill(cx, line + 2, cx + 4, line + 6, i < lit ? tint : TROUGH);
            }
        }

        // The first unshielded hazard: a red warning on its own line so the x4 drain is never mysterious.
        OxygenManager.HazardShield hazard = OxygenManager.HazardShield.of(hazardNow);
        if (hazard != OxygenManager.HazardShield.NONE) {
            line = HudLayout.lineY(y, k++);
            String hazardKey = switch (hazard) {
                case HEAT -> "hud.nerospace.hazard_heat";
                case RADIATION -> "hud.nerospace.hazard_radiation";
                default -> "hud.nerospace.hazard_cold";
            };
            Glyphs.draw(g, Glyphs.hazard(hazardNow), barX - HudLayout.GLYPH_LEFT, line, LOW);
            g.text(mc.font, Component.translatable(hazardKey), barX, line, LOW, true);
        }
    }

    /** Weather tints (the planet card's): spore green, dust tan, ash grey, diamond-dust ice, flare orange. */
    private static int weatherTint(WeatherEvent e) {
        return switch (e) {
            case SPORE_BLOOM -> 0xFF3FBF6F;
            case DUST_STORM -> 0xFFD9A066;
            case ASH_FALL -> 0xFFB0A8A0;
            case DIAMOND_DUST -> 0xFF9FE8FF;
            case FLARE -> 0xFFFF6B35;
            default -> LABEL;
        };
    }

    private static Component ellipsis(Minecraft mc, Component text, int maxWidth) {
        String s = text.getString();
        if (mc.font.width(s) <= maxWidth) {
            return text;
        }
        return Component.literal(mc.font.plainSubstrByWidth(s, Math.max(0, maxWidth - mc.font.width("…"))) + "…");
    }

    /**
     * Client-side mirror of {@code PlanetHazards.at(level, pos, player)}: biome and trait hazards from the synced
     * biome tags, weather radiation from the synced planet weather when the player stands under open sky. A
     * Dawnband's positional COLD shield and oxygen fields are server-only, so the server's damage tick stays the
     * authority; this only picks the warning line.
     */
    private static Hazard clientHazard(LocalPlayer player) {
        // Worked out once per client tick (the HUD draws every frame; the answer only changes between ticks).
        long tick = player.level().getGameTime();
        int levelId = System.identityHashCode(player.level());
        Hazard cached = cachedHazard;
        if (cached != null && cachedHazardTick == tick && cachedHazardLevel == levelId) {
            return cached;
        }
        Hazard now = computeClientHazard(player);
        cachedHazard = now;
        cachedHazardTick = tick;
        cachedHazardLevel = levelId;
        return now;
    }

    private static Hazard cachedHazard;
    private static long cachedHazardTick = Long.MIN_VALUE;
    private static int cachedHazardLevel;

    private static Hazard computeClientHazard(LocalPlayer player) {
        BlockPos pos = player.blockPosition();
        WeatherEvent event = ClientPlanetWeather.event(player.level());
        boolean open = player.level().hasChunkAt(pos)
                && pos.getY() >= player.level().getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) - 1;
        String biome = player.level().hasChunkAt(pos)
                ? player.level().getBiome(pos).unwrapKey().map(k -> k.identifier().toString()).orElse("") : "";
        boolean weatherRad = open && (event == WeatherEvent.DUST_STORM
                || RadiationRules.flareApplies(event, biome, true));
        return HazardOrderRules.firstUnshielded(PlanetHazards.present(player.level(), pos, weatherRad),
                PlanetHazards.suitShields(player.level(), pos, player));
    }

    // --- F1 / hide-GUI detection --------------------------------------------
    // 26.x removed Options#hideGui; the flag is Hud#isHidden(), reached via the Gui#hud field. That field
    // is package-private on the de-obf compile classpath (only NeoForge's runtime AT widens it) and its
    // type diverges across 26.1.2/26.2, so a per-version access-widener is brittle. Read it reflectively
    // (handles cached after the first lookup; falls back to "not hidden" if the shape ever changes).

    private static boolean hideGuiResolved;
    private static java.lang.reflect.Field guiHudField;
    private static java.lang.reflect.Method hudIsHidden;

    private static boolean isGuiHidden(Minecraft mc) {
        if (!hideGuiResolved) {
            hideGuiResolved = true;
            try {
                guiHudField = net.minecraft.client.gui.Gui.class.getDeclaredField("hud");
                guiHudField.setAccessible(true);
                hudIsHidden = guiHudField.getType().getMethod("isHidden");
            } catch (ReflectiveOperationException | RuntimeException e) {
                guiHudField = null;
                hudIsHidden = null;
            }
        }
        if (guiHudField == null || hudIsHidden == null) {
            return false;
        }
        try {
            Object hud = guiHudField.get(mc.gui);
            return hud != null && Boolean.TRUE.equals(hudIsHidden.invoke(hud));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }
}
