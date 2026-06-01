package za.co.neroland.nerospace.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.rocket.Destinations;
import za.co.neroland.nerospace.rocket.RocketMenu;

/**
 * The interactive in-rocket UI (Phase 9b): a sci-fi panel with a fuel gauge, an interactive
 * trajectory row (the pad followed by a {@link SpaceButton} per reachable destination, the chosen
 * one lit), a Launch button, and a fuel-intake slot. Built from custom widgets + gauges drawn on the
 * hull panel (26.1 render model), so it no longer looks like vanilla.
 */
public class RocketScreen extends TexturedContainerScreen<RocketMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/gui/rocket.png");
    private static final int ACCENT = 0xFFE0506A;     // rocket red
    private static final int FUEL = 0xFFF0703C;       // fuel orange-red

    private SpaceButton launchButton;
    private final List<SpaceButton> destinationButtons = new ArrayList<>();

    public RocketScreen(RocketMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }

    @Override
    protected void init() {
        super.init();
        this.destinationButtons.clear();

        List<ResourceKey<Level>> destinations = this.menu.getTier().destinations();
        int x = this.leftPos + 30;
        for (int i = 0; i < destinations.size(); i++) {
            final int index = i;
            SpaceButton node = new SpaceButton(x, this.topPos + 36, 44, 14,
                    Component.literal(shortName(Destinations.name(destinations.get(i)))), ACCENT,
                    b -> onSelectDestination(index));
            node.active = destinations.size() > 1;
            this.addRenderableWidget(node);
            this.destinationButtons.add(node);
            x += 46;
        }

        this.launchButton = new SpaceButton(this.leftPos + 8, this.topPos + 52, 128, 14,
                Component.translatable("gui.nerospace.rocket.launch"), ACCENT, b -> onLaunch());
        this.addRenderableWidget(this.launchButton);
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        int pct = this.menu.getFuelPercent();
        label(g, Component.literal("Fuel: " + pct + "%   " + this.menu.getFuel() + " / " + this.menu.getCapacity() + " mB"),
                8, 17, 0xFFFFC9B0);
        hGauge(g, 8, 27, 160, 5, pct / 100f, FUEL);
        label(g, Component.literal("PAD >"), 8, 39, 0xFF9FB4C8);

        // Reflect live selection / launch-readiness onto the widgets.
        int selected = this.menu.getDestinationIndex();
        for (int i = 0; i < this.destinationButtons.size(); i++) {
            this.destinationButtons.get(i).setSelected(i == selected);
        }
        if (this.launchButton != null) {
            this.launchButton.active = this.menu.isLaunchable();
        }
    }

    private static String shortName(String full) {
        return switch (full) {
            case "Orbital Station" -> "Station";
            case "Greenxertz" -> "Xertz";
            default -> full;
        };
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
