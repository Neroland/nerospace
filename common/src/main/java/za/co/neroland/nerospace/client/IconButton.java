package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import za.co.neroland.nerospace.client.ui.Glyphs;

/**
 * A {@link SpaceButton} that shows an 8x8 glyph from the glyph sheet instead of text, so arrows and chevrons look
 * the same on every loader (the vanilla font would fall back to Unifont for the arrow characters). The message is
 * kept for narration only.
 */
public class IconButton extends SpaceButton {

    private final Glyphs.Glyph glyph;

    public IconButton(int x, int y, int width, int height, Glyphs.Glyph glyph, Component narration, int accent,
            OnPress onPress) {
        super(x, y, width, height, narration, accent, onPress);
        this.glyph = glyph;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int colour = drawFrame(extractor);
        int gx = getX() + (getWidth() - Glyphs.CELL) / 2;
        int gy = getY() + (getHeight() - Glyphs.CELL) / 2;
        Glyphs.draw(extractor, this.glyph, gx, gy, colour);
    }
}
