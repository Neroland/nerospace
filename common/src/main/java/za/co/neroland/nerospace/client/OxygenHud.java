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
 * Bespoke oxygen / hazard HUD gauge (terraform design §1.7): an O₂ bubble icon + bar drawn above the
 * hotbar while the player is in an airless Nerospace dimension — the proper readout that replaces the
 * vanilla air-bubble stopgap (which each loader suppresses on these dimensions). It also surfaces the
 * worn suit tier and any active / uncountered hazard, which the vanilla bubbles cannot.
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

    private static final int ICON_SIZE = 16;
    private static final int BAR_W = 80;
    private static final int BAR_H = 6;

    private OxygenHud() {
    }

    /** Draw the gauge for the local player; a no-op off airless dimensions / in creative / with the GUI hidden. */
    public static void render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        // No explicit F1/hide-GUI check: both loaders' HUD pipelines already skip all custom layers when
        // the GUI is hidden (the whole HUD render is gated on it), and 26.x dropped Options#hideGui.
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

        int x = g.guiWidth() / 2 - 91;     // align with the left edge of the hotbar
        int y = g.guiHeight() - 49;        // sit just above the hotbar / status rows
        int barX = x + ICON_SIZE + 3;      // bar to the right of the icon

        // Icon (16x16 GUI texture), vertically centred on the bar.
        g.blit(RenderPipelines.GUI_TEXTURED, ICON, x, y + BAR_H / 2 - ICON_SIZE / 2,
                0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

        // Gauge: inked border, dark trough, cyan fill (red when low), top sheen.
        g.fill(barX - 1, y - 1, barX + BAR_W + 1, y + BAR_H + 1, INK);
        g.fill(barX, y, barX + BAR_W, y + BAR_H, TROUGH);
        int fw = Math.round(BAR_W * frac);
        if (fw > 0) {
            g.fill(barX, y, barX + fw, y + BAR_H, frac > 0.3F ? O2 : LOW);
            g.fill(barX, y, barX + fw, y + 1, 0x55FFFFFF); // top sheen
        }

        g.text(mc.font, Component.literal("O2  " + Math.round(frac * 100) + "%"), barX, y - 9, LABEL, true);

        // Suit badge (armour slots are already client-synced): an active hazard SHIELD outranks the
        // capacity tier on the badge — both axes still apply.
        OxygenManager.SuitTier suit = OxygenManager.suitTier(player);
        OxygenManager.HazardShield shield = OxygenManager.hazardShield(player);
        int badgeRight = barX + BAR_W;
        if (shield != OxygenManager.HazardShield.NONE) {
            boolean heat = shield == OxygenManager.HazardShield.HEAT;
            Component badge = Component.literal(heat ? "SUIT HEAT" : "SUIT COLD");
            int w = mc.font.width(badge);
            g.text(mc.font, badge, badgeRight - w, y - 9, heat ? HEAT : COLD, true);
            badgeRight -= w + 6;
        } else if (suit != OxygenManager.SuitTier.NONE) {
            boolean t2 = suit == OxygenManager.SuitTier.TIER_2;
            Component badge = Component.literal(t2 ? "SUIT T2" : "SUIT T1");
            int w = mc.font.width(badge);
            g.text(mc.font, badge, badgeRight - w, y - 9, t2 ? TIER2 : TIER1, true);
            badgeRight -= w + 6;
        }

        // Uncountered dimension hazard: a red warning so the x4 drain is never mysterious.
        OxygenManager.HazardShield hazard = OxygenManager.hazardFor(player.level().dimension());
        if (hazard != OxygenManager.HazardShield.NONE && shield != hazard) {
            Component warn = Component.literal(hazard == OxygenManager.HazardShield.HEAT ? "HEAT!" : "COLD!");
            g.text(mc.font, warn, badgeRight - mc.font.width(warn), y - 9, LOW, true);
        }
    }
}
