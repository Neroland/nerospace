package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.menu.PipeConfigMenu;

/**
 * The Universal Pipe configuration GUI. One row per face: a colour chip matching the in-world
 * face shading (see {@link PipeFaceColors} / {@link UniversalPipeRenderer}), the face's I/O mode
 * for the selected layer with a cycle button, and the face's <b>filter slot</b> — the physical
 * Pipe Filter / Advanced Pipe Filter installed on that face (hover it for the filter's contents;
 * take it out to clear the face). Drawn on a plain hull panel (no texture asset); buttons route
 * to the server menu via {@code handleInventoryButtonClick} (no custom packets).
 */
public class PipeConfigScreen extends AbstractContainerScreen<PipeConfigMenu> {

    private static final int ACCENT = 0xFF5AC8E0;   // pipe cyan
    private static final int PANEL = 0xF00B1119;     // dark hull
    private static final int TITLE = 0xFFD6ECFF;
    private static final int SUBTLE = 0xFF8DA0B4;
    private static final int INK = 0xFF05080D;
    private static final int SOCKET = 0xFF141A24;

    private static final String[] FACE_NAMES = {"Down", "Up", "North", "South", "West", "East"};

    public PipeConfigScreen(PipeConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 232);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
    }

    @Override
    protected void init() {
        super.init();
        // Layer cycler.
        this.addRenderableWidget(new SpaceButton(this.leftPos + 100, this.topPos + 16, 68, 14,
                Component.literal("Layer ▸"), ACCENT, b -> sendButton(PipeConfigMenu.BUTTON_CYCLE_TYPE)));
        // One mode cycler per face, aligned with its row.
        for (int i = 0; i < 6; i++) {
            final int face = i;
            this.addRenderableWidget(new SpaceButton(
                    this.leftPos + 114, this.topPos + PipeConfigMenu.FIRST_ROW_Y + i * PipeConfigMenu.ROW_STEP + 1,
                    28, 14, Component.literal("▸"), ACCENT,
                    b -> sendButton(PipeConfigMenu.FACE_BASE + face)));
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        // Plain hull panel + top accent line (no texture asset).
        extractor.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, PANEL);
        extractor.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 1, ACCENT);

        // Slot sockets: the six face filter slots + the player inventory.
        for (int i = 0; i < 6; i++) {
            socket(extractor, PipeConfigMenu.SLOT_X, PipeConfigMenu.FIRST_ROW_Y + i * PipeConfigMenu.ROW_STEP);
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                socket(extractor, 8 + col * 18, PipeConfigMenu.INV_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            socket(extractor, 8 + col * 18, PipeConfigMenu.INV_Y + 58);
        }

        super.extractContents(extractor, mouseX, mouseY, partialTick);

        // Selected layer.
        extractor.text(this.font, Component.literal("Layer: ").append(this.menu.getSelectedType().label()),
                this.leftPos + 8, this.topPos + 19, TITLE, false);
        // Per-face rows: colour chip (matches the in-world Configurator shading) + face + mode.
        for (int i = 0; i < 6; i++) {
            int rowY = this.topPos + PipeConfigMenu.FIRST_ROW_Y + i * PipeConfigMenu.ROW_STEP;
            int chip = PipeFaceColors.ARGB[Direction.from3DDataValue(i).get3DDataValue()];
            extractor.fill(this.leftPos + 8, rowY + 4, this.leftPos + 16, rowY + 12, INK);
            extractor.fill(this.leftPos + 9, rowY + 5, this.leftPos + 15, rowY + 11, chip);
            Component mode = Component.translatable("pipe.nerospace.mode." + this.menu.getFaceMode(i).getSerializedName());
            Component line = Component.literal(FACE_NAMES[i] + ": ").append(mode);
            extractor.text(this.font, line, this.leftPos + 20, rowY + 4, SUBTLE, false);
        }
    }

    /** An 18x18 recessed slot socket around the 16x16 item position at {@code (dx, dy)}. */
    private void socket(GuiGraphicsExtractor extractor, int dx, int dy) {
        int x = this.leftPos + dx;
        int y = this.topPos + dy;
        extractor.fill(x - 1, y - 1, x + 17, y + 17, INK);
        extractor.fill(x, y, x + 16, y + 16, SOCKET);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        extractor.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TITLE, false);
        extractor.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, SUBTLE, false);
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }
}
