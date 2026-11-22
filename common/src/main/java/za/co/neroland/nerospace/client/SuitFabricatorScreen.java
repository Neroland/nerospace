package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.suit.SuitFabricatorMenu;

/** Suit Fabricator. Uses the shared machine panel and draws the charge as a per-mille bar. */
public class SuitFabricatorScreen extends TexturedContainerScreen<SuitFabricatorMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/oxygen_generator.png");

    public SuitFabricatorScreen(SuitFabricatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE, 0xFF3CC8E6, 176, 166);
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor graphics) {
        int filled = this.menu.energyPerMille() * 52 / 1000;
        int x = this.leftPos + 80;
        int y = this.topPos + 20;
        graphics.fill(x, y, x + filled, y + 6, 0xFF3CC8E6);
        graphics.text(this.font, Component.translatable("gui.nerospace.suit_fabricator.hint"),
                this.leftPos + 8, this.topPos + 54, 0xFFD6ECFF, false);
    }
}
