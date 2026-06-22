package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.menu.OxygenGeneratorMenu;

/**
 * Minimal functional screen for the Oxygen Generator (bare backdrop + a power gauge and an oxygen-tank
 * gauge). No slots — power and oxygen both move through the Universal Pipe network. Uses the 26.x
 * container-screen API ({@code extractContents(GuiGraphicsExtractor, ...)}); proven on both loaders.
 */
public class OxygenGeneratorScreen extends AbstractContainerScreen<OxygenGeneratorMenu> {

    private static final int POWER = 0xFF3CC8E6;   // cyan
    private static final int OXYGEN = 0xFF54D46A;  // gas-layer green

    public OxygenGeneratorScreen(OxygenGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        extractor.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFFC6C6C6);
        super.extractContents(extractor, mouseX, mouseY, partialTick);

        int maxEnergy = this.menu.getMaxEnergy();
        int eFill = maxEnergy > 0 ? (int) (this.menu.getEnergy() / (double) maxEnergy * 50.0D) : 0;
        extractor.fill(x + 10, y + 16 + (50 - eFill), x + 18, y + 66, POWER);

        int maxOxygen = this.menu.getMaxOxygen();
        int oFill = maxOxygen > 0 ? (int) (this.menu.getOxygen() / (double) maxOxygen * 50.0D) : 0;
        extractor.fill(x + 22, y + 16 + (50 - oFill), x + 30, y + 66, OXYGEN);
    }
}
