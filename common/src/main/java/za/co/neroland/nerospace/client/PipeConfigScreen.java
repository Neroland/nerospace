package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.menu.PipeConfigMenu;

/**
 * The Universal Pipe configuration GUI (advanced-pipes slice B). Slot-less: shows the selected resource
 * layer and each of the six faces' I/O mode for that layer, with a {@link SpaceButton} to cycle the
 * layer and one per face to cycle its mode. Buttons route to the server menu via
 * {@code handleInventoryButtonClick} (no custom packet). Drawn on a plain hull panel (no texture
 * asset) using the shared {@link SpaceButton}; the live mode/layer text is drawn each frame from the
 * synced menu data.
 */
public class PipeConfigScreen extends AbstractContainerScreen<PipeConfigMenu> {

    private static final int ACCENT = 0xFF5AC8E0;   // pipe cyan
    private static final int PANEL = 0xF00B1119;     // dark hull
    private static final int TITLE = 0xFFD6ECFF;
    private static final int SUBTLE = 0xFF8DA0B4;

    private static final String[] FACE_NAMES = {"Down", "Up", "North", "South", "West", "East"};
    private static final int FIRST_ROW_Y = 40;
    private static final int ROW_STEP = 18;

    public PipeConfigScreen(PipeConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, NerospaceCommon.requireNonNull(title), 176, 152);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        // Layer cycler.
        this.addRenderableWidget(new SpaceButton(this.leftPos + 108, this.topPos + 18, 60, 14,
                Component.literal("Layer ▸"), ACCENT, b -> sendButton(PipeConfigMenu.BUTTON_CYCLE_TYPE)));
        // One cycler per face.
        for (int i = 0; i < 6; i++) {
            final int face = i;
            this.addRenderableWidget(new SpaceButton(this.leftPos + 128, this.topPos + FIRST_ROW_Y + i * ROW_STEP, 40, 14,
                    Component.literal("▸"), ACCENT, b -> sendButton(PipeConfigMenu.FACE_BASE + face)));
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        // Plain hull panel + top accent line (no texture asset).
        extractor.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, PANEL);
        extractor.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 1, ACCENT);
        super.extractContents(extractor, mouseX, mouseY, partialTick);

        // Selected layer.
        Component selectedType = NerospaceCommon.requireNonNull(this.menu.getSelectedType().label());
        Component selectedLayer = NerospaceCommon.requireNonNull(
                Component.literal("Layer: ").append(selectedType));
        extractor.text(this.font, selectedLayer,
                this.leftPos + 8, this.topPos + 22, TITLE, false);
        // Per-face mode for the selected layer.
        for (int i = 0; i < 6; i++) {
            Component mode = Component.translatable("pipe.nerospace.mode." + this.menu.getFaceMode(i).getSerializedName());
            Component line = Component.literal(FACE_NAMES[i] + ": ").append(mode);
            extractor.text(this.font, line, this.leftPos + 8, this.topPos + FIRST_ROW_Y + 3 + i * ROW_STEP, SUBTLE, false);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        extractor.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TITLE, false);
    }

    private void sendButton(int id) {
        MultiPlayerGameMode gameMode = this.minecraft.gameMode;
        if (gameMode != null) {
            gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }
}
