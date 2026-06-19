package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.menu.CombustionGeneratorMenu;

/**
 * Minimal functional screen for the combustion generator (bare backdrop + an energy bar). Uses the
 * 26.x container-screen API ({@code extractContents(GuiGraphicsExtractor, ...)}). Slots are usable;
 * visual polish (a proper GUI texture) is a follow-up — only compilation + registration on both
 * loaders is verifiable headlessly.
 */
public class CombustionGeneratorScreen extends AbstractContainerScreen<CombustionGeneratorMenu> {

    public CombustionGeneratorScreen(CombustionGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        extractor.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFFC6C6C6);
        super.extractContents(extractor, mouseX, mouseY, partialTick);
        int cap = this.menu.capacity();
        int filled = cap > 0 ? (int) (this.menu.energy() / (double) cap * 50.0D) : 0;
        extractor.fill(x + 10, y + 16 + (50 - filled), x + 18, y + 66, 0xFFFF5A3C);
    }
}
