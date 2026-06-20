package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.menu.NerosiumGrinderMenu;

/** Minimal functional screen for the grinder (backdrop + progress arrow + energy bar). */
public class NerosiumGrinderScreen extends AbstractContainerScreen<NerosiumGrinderMenu> {

    public NerosiumGrinderScreen(NerosiumGrinderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        extractor.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFFC6C6C6);
        super.extractContents(extractor, mouseX, mouseY, partialTick);
        // progress arrow (between input @56 and output @116)
        int max = this.menu.maxProgress();
        int prog = max > 0 ? (int) (this.menu.progress() / (double) max * 22.0D) : 0;
        extractor.fill(x + 80, y + 38, x + 80 + prog, y + 42, 0xFF50C0FF);
        // energy bar
        int cap = this.menu.capacity();
        int filled = cap > 0 ? (int) (this.menu.energy() / (double) cap * 50.0D) : 0;
        extractor.fill(x + 10, y + 16 + (50 - filled), x + 18, y + 66, 0xFFFF5A3C);
    }
}
