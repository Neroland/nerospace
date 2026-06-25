package za.co.neroland.nerospace.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;
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
            NerospaceCommon.id("textures/gui/oxygen_hud_icon.png");

    private static final int INK = 0xFF05080D;
    private static final int TROUGH = 0xFF0B1119;
    private static final int O2 = 0xFF3CC8E6;       // cyan
    private static final int LOW = 0xFFE0506A;      // red when low
    private static final int LABEL = 0xFFCFE7FF;
    private static final int TIER1 = 0xFF96AAB4;    // steel — Tier 1 suit badge
    private static final int TIER2 = 0xFFF0C850;    // gold — Tier 2 suit badge
    private static final int HEAT = 0xFFF07830;     // ember — Thermal Suit shield badge
    private static final int COLD = 0xFF78D2F0;     // ice — Cryo Suit shield badge

    private static final int ICON_SIZE = 16;
    private static final int BAR_W = 80;
    private static final int BAR_H = 6;

    private OxygenHud() {
    }

    /** Draw the gauge for the local player; a no-op off airless dimensions / in creative / with the GUI hidden. */
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
        if (!OxygenFieldEvents.FIELD_DIMENSIONS.contains(player.level().dimension())) {
            return;
        }
        int max = player.getMaxAirSupply();
        if (max <= 0) {
            return;
        }
        int air = Math.max(0, Math.min(max, player.getAirSupply()));
        float frac = (float) air / max;

        // Sit just to the RIGHT of the hotbar, vertically level with it; the labels stack upward.
        int totalW = ICON_SIZE + 3 + BAR_W;
        int x = Math.min(g.guiWidth() / 2 + 91 + 4, g.guiWidth() - totalW - 2); // right of the hotbar, clamped on-screen
        int y = g.guiHeight() - 19;                                              // icon top (~level with the hotbar)
        int barX = x + ICON_SIZE + 3;                                            // bar to the right of the icon
        int barY = y + ICON_SIZE / 2 - BAR_H / 2;                                // bar vertically centred on the icon

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

        // Labels stack upward, each on its own line above the bar.
        int line = barY - 10;
        g.text(mc.font, Component.literal("O2  " + Math.round(frac * 100) + "%"), barX, line, LABEL, true);

        // Suit badge (armour slots are already client-synced): an active hazard SHIELD outranks the
        // capacity tier on the badge — both axes still apply.
        OxygenManager.SuitTier suit = OxygenManager.suitTier(player);
        OxygenManager.HazardShield shield = OxygenManager.hazardShield(player);
        if (shield != OxygenManager.HazardShield.NONE) {
            boolean heat = shield == OxygenManager.HazardShield.HEAT;
            line -= 10;
            g.text(mc.font, Component.literal(heat ? "SUIT HEAT" : "SUIT COLD"), barX, line, heat ? HEAT : COLD, true);
        } else if (suit != OxygenManager.SuitTier.NONE) {
            boolean t2 = suit == OxygenManager.SuitTier.TIER_2;
            line -= 10;
            g.text(mc.font, Component.literal(t2 ? "SUIT T2" : "SUIT T1"), barX, line, t2 ? TIER2 : TIER1, true);
        }

        // Uncountered dimension hazard: a red warning on its own line so the x4 drain is never mysterious.
        OxygenManager.HazardShield hazard = OxygenManager.hazardFor(player.level().dimension());
        if (hazard != OxygenManager.HazardShield.NONE && shield != hazard) {
            line -= 10;
            g.text(mc.font, Component.literal(hazard == OxygenManager.HazardShield.HEAT ? "HEAT!" : "COLD!"),
                    barX, line, LOW, true);
        }
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
