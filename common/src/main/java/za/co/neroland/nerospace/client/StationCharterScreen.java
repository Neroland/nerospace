package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.menu.StationCharterMenu;
import za.co.neroland.nerospace.network.FoundStationPayload;
import za.co.neroland.nerospace.network.ModNetwork;
import za.co.neroland.nerospace.network.RenameStationPayload;

/**
 * The station naming console: type a name and confirm. In <b>found</b> mode it creates a new station (the
 * name persists with its Station Core) without teleporting — fly a rocket to visit. In <b>rename</b> mode
 * (opened by right-clicking a Station Core) it renames that station. Built on the shared procedural hull;
 * the name is sent to the server as a {@link FoundStationPayload} or {@link RenameStationPayload}.
 */
public class StationCharterScreen extends TexturedContainerScreen<StationCharterMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/rocket.png");
    private static final int ACCENT = 0xFF54D46A;
    private static final int W = 176;
    private static final int H = 78;

    private EditBox nameBox;

    public StationCharterScreen(StationCharterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, W, H);
        this.titleLabelY = -100;       // drawn manually
        this.inventoryLabelY = 10_000; // hidden
    }

    @Override
    protected void init() {
        super.init();
        this.nameBox = new EditBox(this.font, this.leftPos + 12, this.topPos + 28, W - 24, 18,
                Component.translatable("gui.nerospace.station_charter.name"));
        this.nameBox.setMaxLength(48);
        this.nameBox.setHint(Component.translatable("gui.nerospace.station_charter.hint"));
        this.addRenderableWidget(this.nameBox);
        this.setInitialFocus(this.nameBox);
        this.nameBox.setFocused(true);

        SpaceButton confirm = new SpaceButton(this.leftPos + 12, this.topPos + 52, W - 24, 16,
                Component.translatable("gui.nerospace.station_charter.confirm"), ACCENT, b -> confirm());
        this.addRenderableWidget(confirm);
    }

    /** Let the focused name box swallow all typing (incl. the inventory key) so it never closes the screen. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) { // Escape
            this.onClose();
            return true;
        }
        if (this.nameBox != null && (this.nameBox.keyPressed(event) || this.nameBox.canConsumeInput())) {
            return true;
        }
        return super.keyPressed(event);
    }

    private void confirm() {
        String name = this.nameBox.getValue();
        if (this.menu.mode() == StationCharterMenu.MODE_RENAME) {
            ModNetwork.sendToServer(new RenameStationPayload(this.menu.slot(), name));
        } else {
            ModNetwork.sendToServer(new FoundStationPayload(name));
        }
        this.onClose();
    }

    @Override
    protected void drawPanel(GuiGraphicsExtractor g) {
        int l = this.leftPos;
        int t = this.topPos;
        g.fill(l - 2, t - 2, l + W + 2, t + H + 2, INK);
        g.fill(l, t, l + W, t + H, 0xFF0E1724);
        g.fill(l, t, l + W, t + 16, 0xFF14283C);
        g.fill(l, t + 16, l + W, t + 17, ACCENT);
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        String key = this.menu.mode() == StationCharterMenu.MODE_RENAME
                ? "gui.nerospace.station_charter.rename"
                : "gui.nerospace.station_charter.title";
        label(g, Component.translatable(key), 12, 5, TITLE);
    }
}
