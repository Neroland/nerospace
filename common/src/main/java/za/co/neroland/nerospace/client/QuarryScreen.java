package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.machine.quarry.MinerTier;
import za.co.neroland.nerospace.machine.quarry.QuarryControllerBlockEntity;
import za.co.neroland.nerospace.machine.quarry.QuarryMenu;

/**
 * Screen for the quarry controller: sci-fi panel with a power gauge, the dig state, current depth and a
 * fluid-buffer gauge, around the frame/output slots.
 */
public class QuarryScreen extends TexturedContainerScreen<QuarryMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/quarry.png");
    private static final int ACCENT = MinerTier.TIER_1.accentColor();
    private static final int FLUID = 0xFF4FA8FF;

    public QuarryScreen(QuarryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, 176, 210);
        this.titleLabelX = 8;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 116;
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        int energy = this.menu.getEnergy();
        int maxEnergy = this.menu.getMaxEnergy();
        float energyFrac = maxEnergy == 0 ? 0f : (float) energy / maxEnergy;
        int pct = maxEnergy == 0 ? 0 : energy * 100 / maxEnergy;

        label(g, Component.literal("Power: " + pct + "%"), 8, 80, TITLE);
        segGauge(g, 8, 90, 160, 3, energyFrac, ACCENT);

        QuarryControllerBlockEntity.State state = this.menu.getState();
        label(g, Component.translatable("gui.nerospace.quarry.state." + state.name().toLowerCase(java.util.Locale.ROOT)),
                8, 97, SUBTLE);
        int depth = Math.max(0, this.menu.getRefY() - this.menu.getCurrentY());
        if (state != QuarryControllerBlockEntity.State.IDLE) {
            label(g, Component.literal("Depth: " + depth), 110, 97, SUBTLE);
        }

        int fluid = this.menu.getFluid();
        int maxFluid = this.menu.getMaxFluid();
        float fluidFrac = maxFluid == 0 ? 0f : (float) fluid / maxFluid;
        label(g, Component.literal("Fluid: " + fluid + " mB"), 8, 106, 0xFFB9D7FF);
        fluidGauge(g, 96, 107, 72, 3, fluidFrac, FLUID);
    }
}
