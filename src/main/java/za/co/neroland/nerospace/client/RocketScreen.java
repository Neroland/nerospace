package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.rocket.Destinations;
import za.co.neroland.nerospace.rocket.RocketMenu;

/**
 * The in-rocket UI: a destination selector (shows the target; cycles through the tier's unlocked
 * destinations) and a Launch button. Both route through the vanilla inventory-button channel
 * ({@code handleInventoryButtonClick}) to {@link RocketMenu#clickMenuButton}, handled server-side.
 *
 * <p>NOTE: the container background panel still needs a {@code runClient} pass (26.1's screen render
 * model); for now the controls are plain buttons, which render fine.</p>
 */
public class RocketScreen extends AbstractContainerScreen<RocketMenu> {

    private int localDestIndex;
    private Button destinationButton;

    public RocketScreen(RocketMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }

    @Override
    protected void init() {
        super.init();
        this.localDestIndex = this.menu.getDestinationIndex();

        this.destinationButton = Button.builder(destinationLabel(this.localDestIndex), button -> onCycle())
                .bounds(this.leftPos + 30, this.topPos + 18, 116, 20)
                .build();
        this.destinationButton.active = this.menu.hasMultipleDestinations();
        this.addRenderableWidget(this.destinationButton);

        Button launch = Button.builder(Component.translatable("gui.nerospace.rocket.launch"), button -> onLaunch())
                .bounds(this.leftPos + 50, this.topPos + 44, 76, 20)
                .build();
        this.addRenderableWidget(launch);
    }

    private Component destinationLabel(int index) {
        var destinations = this.menu.getTier().destinations();
        if (destinations.isEmpty()) {
            return Component.literal("Destination: —");
        }
        int clamped = Math.floorMod(index, destinations.size());
        return Component.literal("Destination: " + Destinations.name(destinations.get(clamped)));
    }

    private void onCycle() {
        sendButton(RocketMenu.BUTTON_CYCLE_DEST);
        var destinations = this.menu.getTier().destinations();
        if (destinations.size() > 1) {
            this.localDestIndex = Math.floorMod(this.localDestIndex + 1, destinations.size());
            this.destinationButton.setMessage(destinationLabel(this.localDestIndex));
        }
    }

    private void onLaunch() {
        sendButton(RocketMenu.BUTTON_LAUNCH);
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }
}
