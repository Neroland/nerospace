package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.machine.NerosiumGrinderMenu;

/**
 * Screen for the Nerosium Grinder.
 *
 * <p>26.1 rewrote GUI rendering around a render-state submission model
 * ({@code GuiGraphicsExtractor}, {@code extractBackground}/{@code extractLabels}) and made the
 * {@code imageWidth}/{@code imageHeight} fields final (set via the super constructor). For now this
 * is a minimal functional screen using the default background; energy/progress visuals (synced via
 * the menu's {@code ContainerData}) can be drawn by overriding {@code extractBackground} with the
 * new {@code GuiGraphicsExtractor} once the slice is verified.</p>
 */
public class NerosiumGrinderScreen extends AbstractContainerScreen<NerosiumGrinderMenu> {

    public NerosiumGrinderScreen(NerosiumGrinderMenu menu, Inventory playerInventory, Component title) {
        // Width/height of the background bounds (176x166 is the default and could be omitted).
        super(menu, playerInventory, title, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }
}
