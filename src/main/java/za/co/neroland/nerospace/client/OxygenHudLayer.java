package za.co.neroland.nerospace.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.gui.GuiLayer;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.world.GreenxertzAtmosphere;
import za.co.neroland.nerospace.world.OxygenFieldEvents;

/**
 * Bespoke oxygen HUD gauge (terraform design §1.7, layer 4): an O₂ bubble icon + bar drawn above the
 * hotbar while the player is in an airless Nerospace dimension — the proper readout that replaces the
 * vanilla air-bubble stopgap (which {@code NerospaceClient} now suppresses on these dimensions).
 *
 * <p><b>Sync note:</b> no bespoke attachment payload is needed. {@code GreenxertzAtmosphere} mirrors
 * the oxygen attachment onto the vanilla air supply every tick as {@code air = oxygen * airMax / max}
 * where {@code max} is the worn suit tier's capacity, and the air supply is already vanilla-synced to
 * the client — so {@code air / maxAir} IS the oxygen fraction, which is all the gauge shows. The suit
 * tier badge comes from the player's armour slots ({@link GreenxertzAtmosphere#suitTier}), which are
 * also already synced. Only an absolute "mB remaining" readout would need a custom payload.</p>
 *
 * <p>Drawn via the 26.1 {@code GuiGraphicsExtractor} path (same fill/text/blit approach as the
 * Phase 9 {@code TexturedContainerScreen} machine GUIs).</p>
 */
public class OxygenHudLayer implements GuiLayer {

    private static final Identifier ICON =
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/gui/oxygen_hud_icon.png");

    private static final int INK = 0xFF05080D;
    private static final int TROUGH = 0xFF0B1119;
    private static final int O2 = 0xFF3CC8E6;       // cyan
    private static final int LOW = 0xFFE0506A;      // red when low
    private static final int LABEL = 0xFFCFE7FF;
    private static final int TIER1 = 0xFF96AAB4;    // steel — Tier 1 suit badge
    private static final int TIER2 = 0xFFF0C850;    // gold — Tier 2 suit badge

    private static final int ICON_SIZE = 16;
    private static final int BAR_W = 80;
    private static final int BAR_H = 6;

    @Override
    public void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator() || player.getAbilities().instabuild) {
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

        // Suit-tier badge (armour slots are already client-synced; this is a pure item check).
        GreenxertzAtmosphere.SuitTier suit = GreenxertzAtmosphere.suitTier(player);
        if (suit != GreenxertzAtmosphere.SuitTier.NONE) {
            boolean t2 = suit == GreenxertzAtmosphere.SuitTier.TIER_2;
            Component badge = Component.literal(t2 ? "SUIT T2" : "SUIT T1");
            g.text(mc.font, badge, barX + BAR_W - mc.font.width(badge), y - 9, t2 ? TIER2 : TIER1, true);
        }
    }
}
