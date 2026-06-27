package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import za.co.neroland.nerospace.menu.TrashCanMenu;

/**
 * Functional screen for the Trash Can: a bevelled hull panel with a recessed cell drawn behind every
 * slot (the single trash drop slot up top + the full player inventory + hotbar below), so the menu reads
 * like a normal container instead of a bare backdrop. The trash slot shows the last item dropped in (the
 * next one to be voided). Uses the 26.x container-screen API ({@code extractContents(GuiGraphicsExtractor,
 * ...)}); no texture asset, matching the mod's plain-hull screen convention.
 */
public class TrashCanScreen extends AbstractContainerScreen<TrashCanMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int HILITE = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;
    private static final int CELL_BORDER = 0xFF373737;
    private static final int CELL_FILL = 0xFF8B8B8B;

    public TrashCanScreen(TrashCanMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        // Hull panel with a simple bevel.
        extractor.fill(x, y, x + w, y + h, PANEL);
        extractor.fill(x, y, x + w, y + 1, HILITE);
        extractor.fill(x, y, x + 1, y + h, HILITE);
        extractor.fill(x, y + h - 1, x + w, y + h, SHADOW);
        extractor.fill(x + w - 1, y, x + w, y + h, SHADOW);

        // Recessed cell behind every slot (trash drop slot + player inventory + hotbar).
        for (Slot slot : this.menu.slots) {
            int sx = x + slot.x;
            int sy = y + slot.y;
            extractor.fill(sx - 1, sy - 1, sx + 17, sy + 17, CELL_BORDER);
            extractor.fill(sx, sy, sx + 16, sy + 16, CELL_FILL);
        }

        super.extractContents(extractor, mouseX, mouseY, partialTick);
    }
}
