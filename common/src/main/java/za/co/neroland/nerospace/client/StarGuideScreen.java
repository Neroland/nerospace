package za.co.neroland.nerospace.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.client.ui.Glyphs;
import za.co.neroland.nerospace.client.ui.PlanetAccents;
import za.co.neroland.nerospace.client.ui.StarGuideLayout;
import za.co.neroland.nerospace.progression.StarGuide;
import za.co.neroland.nerospace.progression.StarGuideMenu;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;

/**
 * The Star Guide screen: a scrolling chapter rail on the left (ten 14 px entries, Worlds chapters by planet name
 * with a planet-accent stripe), the selected chapter's step nodes joined by a dotted trajectory on the right with a
 * progress counter, and a scrolling guide-text well underneath. Completed steps light up; a hidden bonus step shows
 * once done, with a dotted border and a star. Geometry lives in {@link StarGuideLayout}.
 *
 * <p>The click id ({@code chapter * 16 + step}) reports a step as seen so its completion pulse stops.</p>
 */
public class StarGuideScreen extends TexturedContainerScreen<StarGuideMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/star_guide.png");
    /** Star Guide accent: nerosium purple (the mod's signpost block). */
    private static final int ACCENT = 0xFFB05AE0;
    private static final int DONE = 0xFF58D08A;
    private static final int VALUE = 0xFFB9C6D4;
    private static final int STEP_TITLE = 0xFFE6D2FF;
    private static final int BODY_TEXT = 0xFFB6C6D8;
    private static final int PATH_IDLE = 0xFF31506B;

    private final List<RailEntry> railButtons = new ArrayList<>();
    private final List<NodeButton> stepButtons = new ArrayList<>();
    private int selectedChapter;
    private int selectedStep;
    private int railScroll;
    private int textScroll;
    /** Real step indices of the nodes shown for the selected chapter (hidden incomplete steps left out). */
    private final List<Integer> shownSteps = new ArrayList<>();
    private int shownMask = -1;

    public StarGuideScreen(StarGuideMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, StarGuideLayout.PANEL_W, StarGuideLayout.PANEL_H);
        this.titleLabelX = 10;
        this.inventoryLabelY = 10_000; // no player inventory on this panel
    }

    @Override
    protected void init() {
        this.cachedLines = null;
        super.init();
        this.railScroll = StarGuideLayout.scrollToShow(this.selectedChapter, this.railScroll, StarGuide.CHAPTER_COUNT);
        rebuildWidgets(null);
    }

    /**
     * Rebuilds every widget in reading (Tab) order: rail entries, chevrons, step nodes. {@code focusChapter} gets
     * keyboard focus afterwards when it is on the rail.
     */
    private void rebuildWidgets(@Nullable Integer focusChapter) {
        clearWidgets();
        this.railButtons.clear();
        int last = Math.min(StarGuide.CHAPTER_COUNT, this.railScroll + StarGuideLayout.RAIL_VISIBLE);
        RailEntry focus = null;
        for (int chapter = this.railScroll; chapter < last; chapter++) {
            int[] r = StarGuideLayout.railEntry(chapter - this.railScroll);
            final int c = chapter;
            RailEntry entry = new RailEntry(this.leftPos + r[0], this.topPos + r[1], r[2], r[3], chapter,
                    b -> selectChapter(c, false));
            addRenderableWidget(entry);
            this.railButtons.add(entry);
            if (focusChapter != null && focusChapter == chapter) {
                focus = entry;
            }
        }
        if (StarGuide.CHAPTER_COUNT > StarGuideLayout.RAIL_VISIBLE) {
            int max = StarGuideLayout.maxRailScroll(StarGuide.CHAPTER_COUNT);
            Component narration = Component.translatable("gui.nerospace.star_guide.scroll_chapters");
            IconButton up = new IconButton(this.leftPos + StarGuideLayout.CHEV_UP[0],
                    this.topPos + StarGuideLayout.CHEV_UP[1], StarGuideLayout.CHEV_UP[2], StarGuideLayout.CHEV_UP[3],
                    Glyphs.Glyph.CHEV_UP, narration, ACCENT, b -> scrollRail(-1));
            up.active = this.railScroll > 0;
            addRenderableWidget(up);
            IconButton down = new IconButton(this.leftPos + StarGuideLayout.CHEV_DOWN[0],
                    this.topPos + StarGuideLayout.CHEV_DOWN[1], StarGuideLayout.CHEV_DOWN[2],
                    StarGuideLayout.CHEV_DOWN[3], Glyphs.Glyph.CHEV_DOWN, narration, ACCENT, b -> scrollRail(1));
            down.active = this.railScroll < max;
            addRenderableWidget(down);
        }
        rebuildStepButtons();
        if (focus != null) {
            setFocused(focus);
        }
    }

    private void scrollRail(int delta) {
        int next = Math.max(0, Math.min(StarGuideLayout.maxRailScroll(StarGuide.CHAPTER_COUNT), this.railScroll + delta));
        if (next != this.railScroll) {
            this.railScroll = next;
            rebuildWidgets(null);
        }
    }

    private void selectChapter(int chapter, boolean keyboard) {
        this.selectedChapter = Math.max(0, Math.min(StarGuide.CHAPTER_COUNT - 1, chapter));
        this.selectedStep = 0;
        this.textScroll = 0;
        this.shownMask = -1;
        this.railScroll = StarGuideLayout.scrollToShow(this.selectedChapter, this.railScroll, StarGuide.CHAPTER_COUNT);
        rebuildWidgets(keyboard ? this.selectedChapter : null);
    }

    /**
     * Builds one node per SHOWN step (a hidden step appears only once complete). {@link #shownSteps} maps a node
     * slot back to the real step index, which is what the click id and the completion bits use.
     */
    private void rebuildStepButtons() {
        this.stepButtons.forEach(this::removeWidget);
        this.stepButtons.clear();
        this.shownSteps.clear();
        StarGuide.Chapter chapter = StarGuide.CHAPTERS.get(this.selectedChapter);
        int mask = this.menu.completionMask(this.selectedChapter);
        this.shownMask = mask;
        for (int i = 0; i < chapter.steps().size(); i++) {
            if (chapter.isShown(i, mask)) {
                this.shownSteps.add(i);
            }
        }
        int nodes = this.shownSteps.size();
        int number = 0;
        for (int slot = 0; slot < nodes; slot++) {
            final int step = this.shownSteps.get(slot);
            boolean bonus = chapter.steps().get(step).hidden();
            int[] r = StarGuideLayout.node(slot, nodes);
            NodeButton node = new NodeButton(this.leftPos + r[0], this.topPos + r[1], r[2], r[3],
                    bonus ? Component.translatable("gui.nerospace.star_guide.bonus")
                            : Component.literal(String.valueOf(++number)),
                    bonus, b -> selectStep(step));
            if (bonus) {
                node.setTooltip(Tooltip.create(Component.translatable("gui.nerospace.star_guide.bonus.tooltip")));
            }
            addRenderableWidget(node);
            this.stepButtons.add(node);
        }
    }

    private void selectStep(int step) {
        this.selectedStep = step;
        this.textScroll = 0;
        // Report "seen" so the completed-pulse stops (server writes the STAR_GUIDE_SEEN attachment).
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(
                    this.menu.containerId, this.selectedChapter * 16 + step);
        }
    }

    /** The planet a Worlds chapter is about, or null for the other chapters. */
    @Nullable
    private static PlanetDefinition worldsPlanet(StarGuide.Chapter chapter) {
        if (!chapter.id().startsWith(StarGuide.WORLDS_PREFIX)) {
            return null;
        }
        String name = chapter.id().substring(StarGuide.WORLDS_PREFIX.length());
        for (PlanetDefinition p : Planets.ALL) {
            if (p.dimension().identifier().getPath().equals(name)) {
                return p;
            }
        }
        return null;
    }

    /** The rail label: the planet name for a Worlds chapter, the chapter title otherwise. */
    static String railKey(StarGuide.Chapter chapter) {
        return chapter.id().startsWith(StarGuide.WORLDS_PREFIX)
                ? "gui.nerospace.star_guide.rail." + chapter.id() : chapter.titleKey();
    }

    private static Component ellipsis(Font font, Component text, int maxWidth) {
        String s = text.getString();
        if (font.width(s) <= maxWidth) {
            return text;
        }
        return Component.literal(font.plainSubstrByWidth(s, Math.max(0, maxWidth - font.width("…"))) + "…");
    }

    private List<FormattedCharSequence> descriptionLines() {
        List<StarGuide.Step> steps = StarGuide.CHAPTERS.get(this.selectedChapter).steps();
        int index = Math.min(this.selectedStep, steps.size() - 1);
        // Wrapped once per selected step, not every frame and wheel notch (the screen is rebuilt on a language change).
        int key = this.selectedChapter * 16 + index;
        List<FormattedCharSequence> lines = this.cachedLines;
        if (lines == null || this.cachedLinesKey != key) {
            lines = this.font.split(Component.translatable(steps.get(index).textKey()), StarGuideLayout.TEXT_W);
            this.cachedLines = lines;
            this.cachedLinesKey = key;
        }
        return lines;
    }

    @Nullable
    private List<FormattedCharSequence> cachedLines;
    private int cachedLinesKey = -1;

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractContents(extractor, mouseX, mouseY, partialTick);
        double mx = mouseX - this.leftPos;
        double my = mouseY - this.topPos;
        StarGuide.Chapter chapter = StarGuide.CHAPTERS.get(this.selectedChapter);
        // Progress counter tooltip.
        int required = Integer.bitCount(chapter.requiredMask());
        int done = Integer.bitCount(this.menu.completionMask(this.selectedChapter) & chapter.requiredMask());
        Component progress = Component.translatable("gui.nerospace.star_guide.progress", done, required);
        int pw = this.font.width(progress);
        if (StarGuideLayout.inside(new int[] { StarGuideLayout.PROGRESS_RIGHT - pw, StarGuideLayout.PROGRESS_Y, pw, 9 },
                mx, my)) {
            extractor.setTooltipForNextFrame(
                    Component.translatable("gui.nerospace.star_guide.progress.tooltip", done, required), mouseX, mouseY);
        }
        // Full step title over the title row (it may be ellipsised).
        if (StarGuideLayout.inside(new int[] { StarGuideLayout.TITLE_X, StarGuideLayout.TITLE_Y, StarGuideLayout.TITLE_W, 9 },
                mx, my)) {
            StarGuide.Step step = chapter.steps().get(Math.min(this.selectedStep, chapter.steps().size() - 1));
            extractor.setTooltipForNextFrame(Component.translatable(step.titleKey()), mouseX, mouseY);
        }
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        StarGuide.Chapter chapter = StarGuide.CHAPTERS.get(this.selectedChapter);
        List<StarGuide.Step> steps = chapter.steps();
        if (this.menu.completionMask(this.selectedChapter) != this.shownMask) {
            rebuildStepButtons(); // completion synced (or a hidden step just completed): re-layout the nodes
        }

        // Rail: selected or fully-completed chapters lit.
        for (RailEntry entry : this.railButtons) {
            boolean allDone = StarGuide.CHAPTERS.get(entry.chapter).isComplete(this.menu.completionMask(entry.chapter));
            entry.complete = allDone;
            entry.setSelected(entry.chapter == this.selectedChapter || allDone);
        }
        int[] thumb = StarGuideLayout.railThumb(this.railScroll, StarGuide.CHAPTER_COUNT);
        if (thumb != null) {
            int[] bar = StarGuideLayout.RAIL_BAR;
            g.fill(this.leftPos + bar[0], this.topPos + bar[1], this.leftPos + bar[0] + bar[2],
                    this.topPos + bar[1] + bar[3], TROUGH);
            g.fill(this.leftPos + bar[0], this.topPos + thumb[0], this.leftPos + bar[0] + bar[2],
                    this.topPos + thumb[0] + thumb[1], SUBTLE);
        }

        // Dotted trajectory line linking the chapter's nodes in order (the progression path).
        int nodes = this.shownSteps.size();
        for (int i = 0; i < nodes - 1; i++) {
            boolean done = this.menu.isStepComplete(this.selectedChapter, this.shownSteps.get(i));
            int color = done ? DONE : PATH_IDLE;
            int[] a = StarGuideLayout.node(i, nodes);
            int[] b = StarGuideLayout.node(i + 1, nodes);
            if (i / 3 == (i + 1) / 3) {
                int xa = Math.min(a[0], b[0]) + 42;
                int xb = Math.max(a[0], b[0]) - 2;
                dottedLine(g, this.leftPos + xa, this.topPos + a[1] + 6, this.leftPos + xb, this.topPos + a[1] + 6,
                        color);
            } else {
                int cx = a[0] + 19; // same column as the next node (serpentine turn)
                dottedLine(g, this.leftPos + cx, this.topPos + a[1] + 15, this.leftPos + cx, this.topPos + b[1] - 2,
                        color);
            }
        }

        // Step nodes: completed steps lit (steady once seen, pulsing until clicked once).
        long now = System.currentTimeMillis();
        boolean pulseOn = (now / 400L) % 2L == 0L;
        for (int i = 0; i < this.stepButtons.size(); i++) {
            int real = this.shownSteps.get(i);
            boolean done = this.menu.isStepComplete(this.selectedChapter, real);
            boolean seen = this.menu.isStepSeen(this.selectedChapter, real);
            NodeButton node = this.stepButtons.get(i);
            node.done = done;
            node.setSelected(done && (seen || pulseOn));
        }

        // Progress: required steps done / required steps (a bonus step never counts).
        int required = Integer.bitCount(chapter.requiredMask());
        int doneCount = Integer.bitCount(this.menu.completionMask(this.selectedChapter) & chapter.requiredMask());
        Component progress = Component.translatable("gui.nerospace.star_guide.progress", doneCount, required);
        label(g, progress, StarGuideLayout.PROGRESS_RIGHT - this.font.width(progress), StarGuideLayout.PROGRESS_Y,
                chapter.isComplete(this.menu.completionMask(this.selectedChapter)) ? DONE : VALUE);

        // Guide text for the selected step.
        StarGuide.Step step = steps.get(Math.min(this.selectedStep, steps.size() - 1));
        boolean stepDone = this.menu.isStepComplete(this.selectedChapter, this.selectedStep);
        Component title = ellipsis(this.font, Component.translatable(step.titleKey()),
                stepDone ? StarGuideLayout.TITLE_W_DONE : StarGuideLayout.TITLE_W);
        label(g, title, StarGuideLayout.TITLE_X, StarGuideLayout.TITLE_Y, stepDone ? DONE : STEP_TITLE);
        if (stepDone) {
            Glyphs.draw(g, Glyphs.Glyph.CHECK, this.leftPos + StarGuideLayout.DONE_MARK_X,
                    this.topPos + StarGuideLayout.TITLE_Y, DONE);
        }
        List<FormattedCharSequence> lines = descriptionLines();
        this.textScroll = Math.max(0, Math.min(StarGuideLayout.maxTextScroll(lines.size()), this.textScroll));
        for (int k = 0; k < StarGuideLayout.TEXT_LINES && this.textScroll + k < lines.size(); k++) {
            g.text(this.font, lines.get(this.textScroll + k), this.leftPos + StarGuideLayout.TEXT_X,
                    this.topPos + StarGuideLayout.TEXT_Y + k * StarGuideLayout.LINE_H, BODY_TEXT, false);
        }
        int[] textThumb = StarGuideLayout.textThumb(this.textScroll, lines.size());
        if (textThumb != null) {
            int[] bar = StarGuideLayout.TEXT_BAR;
            g.fill(this.leftPos + bar[0], this.topPos + bar[1], this.leftPos + bar[0] + bar[2],
                    this.topPos + bar[1] + bar[3], TROUGH);
            g.fill(this.leftPos + bar[0], this.topPos + textThumb[0], this.leftPos + bar[0] + bar[2],
                    this.topPos + textThumb[0] + textThumb[1], SUBTLE);
        }
    }

    /** The wheel scrolls the chapter rail over the rail, the guide text over the text well. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double mx = mouseX - this.leftPos;
        double my = mouseY - this.topPos;
        int dir = scrollY > 0.0D ? -1 : (scrollY < 0.0D ? 1 : 0);
        if (dir != 0 && StarGuideLayout.inside(StarGuideLayout.RAIL_WELL, mx, my)) {
            scrollRail(dir);
            return true;
        }
        if (dir != 0 && StarGuideLayout.inside(StarGuideLayout.TEXT_WELL, mx, my)) {
            int max = StarGuideLayout.maxTextScroll(descriptionLines().size());
            this.textScroll = Math.max(0, Math.min(max, this.textScroll + dir));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** Up / Down with keyboard focus on the rail move the chapter selection. */
    //? if >=1.21.9 {
    @Override
    public boolean keyPressed(KeyEvent event) {
    //?} else {
    /*@Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        KeyEvent event = new KeyEvent(keyCode, scanCode, modifiers);
    *///?}
        GuiEventListener focused = getFocused();
        if (focused instanceof RailEntry && (event.isUp() || event.isDown())) {
            int next = this.selectedChapter + (event.isUp() ? -1 : 1);
            if (next >= 0 && next < StarGuide.CHAPTER_COUNT) {
                selectChapter(next, true);
            }
            return true;
        }
        //? if >=1.21.9 {
        return super.keyPressed(event);
        //?} else {
        /*return super.keyPressed(event.key(), event.scancode(), event.modifiers());
        *///?}
    }

    /** A dotted 2px line between two points (axis-aligned or otherwise), ~5px pitch. */
    private static void dottedLine(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(1, (int) (Math.hypot(x1 - x0, y1 - y0) / 5.0D));
        for (int s = 0; s <= steps; s++) {
            float t = s / (float) steps;
            int ax = Math.round(Mth.lerp(t, x0, x1));
            int ay = Math.round(Mth.lerp(t, y0, y1));
            g.fill(ax, ay, ax + 2, ay + 2, color);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        extractor.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TITLE, false);
        // No inventory label: the panel has no player slots.
    }

    /** A rail entry: left-aligned, ellipsised title (full title in the tooltip), planet stripe and a done tick. */
    private static final class RailEntry extends SpaceButton {

        private final int chapter;
        private final int stripe;
        private boolean complete;

        RailEntry(int x, int y, int w, int h, int chapter, OnPress onPress) {
            super(x, y, w, h, Component.translatable(railKey(StarGuide.CHAPTERS.get(chapter))), ACCENT, onPress);
            this.chapter = chapter;
            PlanetDefinition planet = worldsPlanet(StarGuide.CHAPTERS.get(chapter));
            this.stripe = planet == null ? 0 : PlanetAccents.of(planet.dimension());
            setTooltip(Tooltip.create(Component.translatable(StarGuide.CHAPTERS.get(chapter).titleKey())));
        }

        //? if >=26.1 {
        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        //?} else if >=1.21.11 {
        /*@Override
        protected void renderContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        *///?} else {
        /*@Override
        protected void renderWidget(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        *///?}
            int colour = drawFrame(g);
            int x = getX();
            int y = getY();
            int textX = x + 2;
            if (this.stripe != 0) {
                g.fill(x + 1, y + 1, x + 4, y + 13, this.stripe);
                textX = x + 7;
            }
            int right = this.complete ? x + 68 : x + getWidth() - 2;
            if (this.complete) {
                Glyphs.draw(g, Glyphs.Glyph.CHECK, x + 70, y + 3, DONE);
            }
            Font font = Minecraft.getInstance().font;
            g.text(font, ellipsis(font, getMessage(), right - textX), textX, y + (getHeight() - 8) / 2, colour, false);
        }
    }

    /** A step node: number (or a star on a bonus step, with a dotted border) and a check once done. */
    private static final class NodeButton extends SpaceButton {

        private final boolean bonus;
        private boolean done;

        NodeButton(int x, int y, int w, int h, Component message, boolean bonus, OnPress onPress) {
            super(x, y, w, h, message, ACCENT, onPress);
            this.bonus = bonus;
            setDotted(bonus);
        }

        //? if >=26.1 {
        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        //?} else if >=1.21.11 {
        /*@Override
        protected void renderContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        *///?} else {
        /*@Override
        protected void renderWidget(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        *///?}
            int colour = drawFrame(g);
            int x = getX();
            int y = getY();
            if (this.bonus) {
                Glyphs.draw(g, Glyphs.Glyph.STAR, x + 16, y + 3, DONE);
            } else {
                Font font = Minecraft.getInstance().font;
                g.centeredText(font, getMessage(), x + getWidth() / 2, y + (getHeight() - 8) / 2, colour);
            }
            if (this.done) {
                Glyphs.draw(g, Glyphs.Glyph.CHECK, x + 30, y + 3, DONE);
            }
        }
    }
}
