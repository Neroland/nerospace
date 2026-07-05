package za.co.neroland.nerospace.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.menu.AdvancedFilterMenu;
import za.co.neroland.nerospace.pipe.FaceFilter;

/**
 * Screen for the Advanced Pipe Filter (issue #25): a 3×3 ghost grid on the left, the
 * whitelist/blacklist and exact/item-only toggles on the right, and a status line under the grid
 * that explains the hovered entry (or the click hints when nothing is hovered). Entries in tag
 * mode carry a gold {@code #} badge. All state is read from the synced menu; the toggles route
 * through {@code handleInventoryButtonClick} like every other Nerospace menu (no custom packets).
 */
public class AdvancedFilterScreen extends TexturedContainerScreen<AdvancedFilterMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/advanced_filter.png");
    private static final int ACCENT = 0xFF5AC8E0;   // pipe cyan
    private static final int GOLD = 0xFFF0C860;     // tag-mode badge
    private static final int HINT_Y = 72;

    private SpaceButton listModeButton;
    private SpaceButton componentsButton;

    public AdvancedFilterScreen(AdvancedFilterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, 176, 166);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
    }

    @Override
    protected void init() {
        super.init();
        this.listModeButton = this.addRenderableWidget(new SpaceButton(
                this.leftPos + 96, this.topPos + 17, 72, 14,
                listModeLabel(), ACCENT,
                b -> sendButton(AdvancedFilterMenu.BUTTON_TOGGLE_LIST_MODE)));
        this.componentsButton = this.addRenderableWidget(new SpaceButton(
                this.leftPos + 96, this.topPos + 37, 72, 14,
                componentsLabel(), ACCENT,
                b -> sendButton(AdvancedFilterMenu.BUTTON_TOGGLE_COMPONENTS)));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // Labels track the synced state (the toggle round-trips through the server).
        this.listModeButton.setMessage(listModeLabel());
        this.componentsButton.setMessage(componentsLabel());
    }

    private Component listModeLabel() {
        return Component.translatable(this.menu.isBlacklist()
                ? "gui.nerospace.advanced_filter.blacklist"
                : "gui.nerospace.advanced_filter.whitelist");
    }

    private Component componentsLabel() {
        return Component.translatable(this.menu.isMatchComponents()
                ? "gui.nerospace.advanced_filter.exact"
                : "gui.nerospace.advanced_filter.item_only");
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractContents(extractor, mouseX, mouseY, partialTick);

        // Gold '#' badge on entries in tag mode.
        for (int i = 0; i < AdvancedFilterMenu.FILTER_SLOTS; i++) {
            if (this.menu.tagIndex(i) >= 0 && !this.menu.ghost(i).isEmpty()) {
                int x = this.leftPos + AdvancedFilterMenu.GRID_X + (i % 3) * 18 + 11;
                int y = this.topPos + AdvancedFilterMenu.GRID_Y + (i / 3) * 18 - 1;
                extractor.text(this.font, Component.literal("#"), x, y, GOLD, true);
            }
        }

        // Status line: the hovered entry's match rule, else the click hints.
        int hovered = hoveredGhostSlot(mouseX, mouseY);
        Component status;
        if (hovered >= 0 && !this.menu.ghost(hovered).isEmpty()) {
            ItemStack ghost = this.menu.ghost(hovered);
            TagKey<Item> tag = resolveTag(ghost, this.menu.tagIndex(hovered));
            status = tag != null
                    ? Component.translatable("gui.nerospace.advanced_filter.matches_tag",
                            "#" + tag.location())
                    : Component.translatable("gui.nerospace.advanced_filter.matches_item",
                            ghost.getHoverName());
        } else {
            status = Component.translatable("gui.nerospace.advanced_filter.hint");
        }
        extractor.text(this.font, status, this.leftPos + 8, this.topPos + HINT_Y, SUBTLE, false);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        // Title only — the vanilla "Inventory" label would overlap the status line at HINT_Y.
        extractor.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TITLE, false);
    }

    /** The ghost-grid index under the mouse, or -1 (geometry-based; no reliance on hoveredSlot). */
    private int hoveredGhostSlot(int mouseX, int mouseY) {
        int dx = mouseX - (this.leftPos + AdvancedFilterMenu.GRID_X);
        int dy = mouseY - (this.topPos + AdvancedFilterMenu.GRID_Y);
        if (dx < 0 || dy < 0 || dx >= 3 * 18 || dy >= 3 * 18) {
            return -1;
        }
        return (dy / 18) * 3 + (dx / 18);
    }

    private static TagKey<Item> resolveTag(ItemStack ghost, int tagIndex) {
        if (tagIndex < 0) {
            return null;
        }
        List<TagKey<Item>> tags = FaceFilter.sortedTags(ghost);
        return tagIndex < tags.size() ? tags.get(tagIndex) : null;
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }
}
