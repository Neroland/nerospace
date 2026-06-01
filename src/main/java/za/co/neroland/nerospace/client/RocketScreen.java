package za.co.neroland.nerospace.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.rocket.Destinations;
import za.co.neroland.nerospace.rocket.RocketMenu;

/**
 * The interactive in-rocket UI (Phase 8b). Built entirely from button widgets + the menu's fuel-intake
 * slot, because 26.1 replaced the immediate-mode {@code GuiGraphics} with a render-state submission
 * model ({@code GuiGraphicsExtractor}) that makes hand-drawn bars/lines impractical here.
 *
 * <p>Layout (top to bottom): a live <b>fuel readout</b> (percentage + millibuckets), an interactive
 * <b>trajectory</b> row — the launch pad followed by one selectable button per destination the tier
 * can reach, with the chosen target highlighted — and a <b>Launch</b> button that enables only when
 * the rocket is fuelled and crewed. Labels and button states refresh every {@link #containerTick()}.
 * The fuel-intake slot (top-right) accepts a fuel bucket/canister.</p>
 */
public class RocketScreen extends AbstractContainerScreen<RocketMenu> {

    private Button fuelReadout;
    private Button launchButton;
    private final List<Button> destinationButtons = new ArrayList<>();

    public RocketScreen(RocketMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }

    @Override
    protected void init() {
        super.init();
        this.destinationButtons.clear();

        // Live fuel readout (non-interactive; updated each tick).
        this.fuelReadout = Button.builder(fuelLabel(), b -> {})
                .bounds(this.leftPos + 8, this.topPos + 16, 160, 12)
                .build();
        this.fuelReadout.active = false;
        this.addRenderableWidget(this.fuelReadout);

        // Trajectory row: launch-pad origin node, then one button per reachable destination.
        Button origin = Button.builder(Component.translatable("gui.nerospace.rocket.pad"), b -> {})
                .bounds(this.leftPos + 8, this.topPos + 32, 24, 18)
                .build();
        origin.active = false;
        this.addRenderableWidget(origin);

        List<ResourceKey<Level>> destinations = this.menu.getTier().destinations();
        int x = this.leftPos + 36;
        for (int i = 0; i < destinations.size(); i++) {
            final int index = i;
            Button node = Button.builder(Component.literal(Destinations.name(destinations.get(i))),
                            b -> onSelectDestination(index))
                    .bounds(x, this.topPos + 32, 42, 18)
                    .build();
            // A single-destination tier can't re-route; leave that node non-interactive.
            node.active = destinations.size() > 1;
            this.addRenderableWidget(node);
            this.destinationButtons.add(node);
            x += 44;
        }

        // Launch.
        this.launchButton = Button.builder(Component.translatable("gui.nerospace.rocket.launch"), b -> onLaunch())
                .bounds(this.leftPos + 8, this.topPos + 54, 120, 18)
                .build();
        this.addRenderableWidget(this.launchButton);

        refresh();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refresh();
    }

    /** Pushes current synced rocket state into the widgets. */
    private void refresh() {
        if (this.fuelReadout != null) {
            this.fuelReadout.setMessage(fuelLabel());
        }
        if (this.launchButton != null) {
            this.launchButton.active = this.menu.isLaunchable();
        }
        int selected = this.menu.getDestinationIndex();
        List<ResourceKey<Level>> destinations = this.menu.getTier().destinations();
        for (int i = 0; i < this.destinationButtons.size() && i < destinations.size(); i++) {
            String name = Destinations.name(destinations.get(i));
            Component label = (i == selected) ? Component.literal("> " + name) : Component.literal(name);
            this.destinationButtons.get(i).setMessage(label);
        }
    }

    private Component fuelLabel() {
        return Component.translatable("gui.nerospace.rocket.fuel_pct",
                this.menu.getFuelPercent(), this.menu.getFuel(), this.menu.getCapacity());
    }

    private void onSelectDestination(int index) {
        sendButton(RocketMenu.SELECT_DEST_BASE + index);
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
