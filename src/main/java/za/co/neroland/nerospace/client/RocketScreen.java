package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.rocket.RocketMenu;

/**
 * The in-rocket UI (Phase 4): the rocket title plus a Launch button. The button routes through the
 * vanilla inventory-button channel ({@code handleInventoryButtonClick}) to
 * {@link RocketMenu#clickMenuButton}, which starts the launch server-side. The server re-validates
 * launch readiness, so the button stays clickable and simply no-ops when the rocket cannot fly.
 *
 * <p>26.1 reworked screen rendering around a render-state submission model, so richer readouts (a
 * fuel gauge, tier/destination text) are deferred to a follow-up that can be iterated live in
 * {@code runClient}; the synced values are already exposed on the menu for that.</p>
 */
public class RocketScreen extends AbstractContainerScreen<RocketMenu> {

    public RocketScreen(RocketMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }

    @Override
    protected void init() {
        super.init();
        Button launch = Button.builder(
                        Component.translatable("gui.nerospace.rocket.launch"),
                        button -> onLaunch())
                .bounds(this.leftPos + 50, this.topPos + 36, 76, 20)
                .build();
        this.addRenderableWidget(launch);
    }

    private void onLaunch() {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, RocketMenu.BUTTON_LAUNCH);
        }
    }
}
