package za.co.neroland.nerospace.client.ui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import org.jetbrains.annotations.Nullable;

/**
 * An invisible, focusable stand-in for one compact {@link PlanetCard} row: Tab reaches it, Enter/Space
 * activates it, and its narration reads the card's tooltip lines. The host screen still draws the card and handles
 * mouse clicks; this widget only adds the focus ring and the narration.
 */
public final class PlanetCardFocus extends Button {

    private static final int FOCUS_RING = 0xFFFFFFFF;

    public PlanetCardFocus(int x, int y, OnPress onPress) {
        super(x, y, PlanetCard.COMPACT_W, PlanetCard.COMPACT_H, Component.empty(), onPress, DEFAULT_NARRATION);
        this.visible = false;
        this.active = false;
    }

    /** Points this row at {@code data} (or hides it when the row is empty). */
    public void show(@Nullable PlanetCardData data) {
        this.visible = data != null;
        this.active = data != null;
        setMessage(data == null ? Component.empty() : narration(data));
    }

    /** The card's tooltip lines (name, select hint or reason, gravity + suit, weather, pad), as one sentence. */
    public static Component narration(PlanetCardData data) {
        List<Component> lines = PlanetCard.tooltip(data, true);
        MutableComponent out = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                out.append(Component.literal(". "));
            }
            out.append(lines.get(i).copy());
        }
        return out;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (!isFocused()) {
            return; // the screen draws the card itself; only keyboard focus needs a ring
        }
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        g.fill(x, y, x + w, y + 1, FOCUS_RING);
        g.fill(x, y + h - 1, x + w, y + h, FOCUS_RING);
        g.fill(x, y, x + 1, y + h, FOCUS_RING);
        g.fill(x + w - 1, y, x + w, y + h, FOCUS_RING);
    }
}
