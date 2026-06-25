package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;


import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.menu.TrashCanMenu;

/**
 * Minimal functional screen for the Trash Can: a bare backdrop with the single preview slot outlined.
 * The slot shows the last item dropped in (the next one to be voided). Uses the 26.x container-screen
 * API ({@code extractContents(GuiGraphicsExtractor, ...)}); proven on both loaders.
 */
public class TrashCanScreen extends AbstractContainerScreen<TrashCanMenu> {

    public TrashCanScreen(TrashCanMenu menu, Inventory playerInventory, Component title) {
        super(NerospaceCommon.requireNonNull(menu), NerospaceCommon.requireNonNull(playerInventory),
                NerospaceCommon.requireNonNull(title));
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        extractor.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFFC6C6C6);
        // Outline the trash preview slot (slot is anchored at 80,35 in the menu).
        extractor.fill(x + 79, y + 34, x + 97, y + 52, 0xFF5A5A5A);
        extractor.fill(x + 80, y + 35, x + 96, y + 51, 0xFF8B8B8B);
        super.extractContents(extractor, mouseX, mouseY, partialTick);
    }
}
