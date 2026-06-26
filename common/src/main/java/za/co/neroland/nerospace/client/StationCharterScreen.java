package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.menu.StationCharterMenu;
import za.co.neroland.nerospace.network.FoundStationPayload;
import za.co.neroland.nerospace.network.ModNetwork;

/**
 * The Station Charter naming console: type a name and press Found to create a new station (the name
 * persists with its Station Core). It does <b>not</b> teleport you — fly a rocket to visit. Built on the
 * shared procedural hull; the name is sent to the server as a {@link FoundStationPayload}.
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

        SpaceButton found = new SpaceButton(this.leftPos + 12, this.topPos + 52, W - 24, 16,
                Component.translatable("gui.nerospace.station_charter.found"), ACCENT, b -> confirm());
        this.addRenderableWidget(found);
    }

    private void confirm() {
        ModNetwork.sendToServer(new FoundStationPayload(this.nameBox.getValue()));
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
        label(g, Component.translatable("gui.nerospace.station_charter.title"), 12, 5, TITLE);
    }
}
