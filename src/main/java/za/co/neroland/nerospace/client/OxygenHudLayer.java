package za.co.neroland.nerospace.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.GuiLayer;

import za.co.neroland.nerospace.world.OxygenFieldEvents;

/**
 * Bespoke oxygen HUD gauge (terraform design §1.7, layer 4): a small O₂ bar drawn above the hotbar
 * while the player is in an airless Nerospace dimension, replacing reliance on the vanilla air-bubble
 * mirror as the readout. Reads the mirrored oxygen value off the player's air supply (kept in sync
 * server-side by {@code GreenxertzAtmosphere}). Drawn via the 26.1 {@code GuiGraphicsExtractor} path.
 */
public class OxygenHudLayer implements GuiLayer {

    private static final int INK = 0xFF05080D;
    private static final int TROUGH = 0xFF0B1119;
    private static final int O2 = 0xFF3CC8E6;       // cyan
    private static final int LOW = 0xFFE0506A;      // red when low
    private static final int LABEL = 0xFFCFE7FF;

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

        int barW = 80;
        int barH = 6;
        int x = g.guiWidth() / 2 - 91;     // align with the left edge of the hotbar
        int y = g.guiHeight() - 49;        // sit just above the hotbar / status rows

        g.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, INK);
        g.fill(x, y, x + barW, y + barH, TROUGH);
        int fw = Math.round(barW * frac);
        if (fw > 0) {
            g.fill(x, y, x + fw, y + barH, frac > 0.3F ? O2 : LOW);
            g.fill(x, y, x + fw, y + 1, 0x55FFFFFF); // top sheen
        }
        g.text(mc.font, Component.literal("O2  " + Math.round(frac * 100) + "%"), x, y - 9, LABEL, true);
    }
}
