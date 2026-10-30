package za.co.neroland.nerospace.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.client.ui.ConsoleLayout;
import za.co.neroland.nerospace.client.ui.Glyphs;
import za.co.neroland.nerospace.menu.StationConsoleMenu;
import za.co.neroland.nerospace.network.ModNetwork;
import za.co.neroland.nerospace.network.RenameStationPayload;
import za.co.neroland.nerospace.network.StationConsoleSyncPayload;
import za.co.neroland.nerospace.network.StationMemberPayload;
import za.co.neroland.nerospace.world.station.DockingPolicy;
import za.co.neroland.nerospace.world.station.ModuleBuildRules;
import za.co.neroland.nerospace.world.station.ModuleCosts;
import za.co.neroland.nerospace.world.station.ModuleKind;
import za.co.neroland.nerospace.world.station.StationAccess;
import za.co.neroland.nerospace.world.station.StationClass;

/**
 * The Station Console screen: a 240×200 panel ({@code station_console.png}) in station teal with four client-side
 * tabs — Overview, Members, Docking, Build. Rows sit on the 9 / 12 / 18 px grid of {@link ConsoleLayout}; every
 * label that a translation could overflow is cut with an ellipsis and carries its full text in a tooltip. Everything it shows comes from the menu's synced ints and the
 * {@link StationConsoleSyncPayload}; every button only asks the server, which re-checks permissions. Member names
 * are resolved from the client's own tab list and never stored; an unresolved member reads "Member #n".
 */
public class StationConsoleScreen extends TexturedContainerScreen<StationConsoleMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/station_console.png");
    /** Station teal. */
    private static final int ACCENT = 0xFF2EC4B6;
    private static final int HULL = 0xFF0D1119;
    private static final int WELL = 0xFF0A1018;
    private static final int VALUE = 0xFFB9C6D4;
    private static final int WARN = 0xFFF5C542;
    private static final int BAD = 0xFFF06060;
    private static final int POWER = 0xFFF6DC8A;
    private static final int O2 = 0xFF3CC8E6;
    private static final int W = 240;
    private static final int H = 200;
    private static final int ROWS = 8;

    private static final int TAB_OVERVIEW = 0;
    private static final int TAB_MEMBERS = 1;
    private static final int TAB_DOCKING = 2;
    private static final int TAB_BUILD = 3;

    private int tab = TAB_OVERVIEW;
    private int scroll;
    private boolean addMode;
    private boolean renaming;
    private long abandonArmedAt;
    private long buildClickAt;
    @Nullable
    private EditBox renameBox;
    private String signature = "";
    private final List<UUID> candidates = new ArrayList<>();
    /** Hover tooltips for drawn (non-widget) rows this frame: {x, y, w, h} panel-relative → text. */
    private final List<Object[]> hoverTips = new ArrayList<>();

    public StationConsoleScreen(StationConsoleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, W, H);
        this.titleLabelY = -100;       // the header draws name + class instead
        this.inventoryLabelY = 10_000; // no player inventory on this screen
    }

    @Nullable
    private StationConsoleSyncPayload sync() {
        return ClientStationConsole.forContainer(this.menu.containerId);
    }

    private boolean manage() {
        return this.menu.get(StationConsoleMenu.D_CAN_MANAGE) != 0 && sync() != null;
    }

    private boolean use() {
        return this.menu.get(StationConsoleMenu.D_CAN_USE) != 0 && sync() != null;
    }

    private void click(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable("gui.nerospace.station_console." + key, args);
    }

    // --- widgets ---------------------------------------------------------------------------------

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void rebuild() {
        String renameText = this.renameBox != null ? this.renameBox.getValue() : null;
        clearWidgets();
        this.renameBox = null;
        int l = this.leftPos;
        int t = this.topPos;
        String[] tabs = { "tab.overview.short", "tab.members", "tab.docking", "tab.build" };
        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            SpaceButton b = new SpaceButton(l + 6 + i * 58, t + 19, 56, 14, tr(tabs[i]), ACCENT, x -> {
                this.tab = idx;
                this.scroll = 0;
                this.addMode = false;
                rebuild();
            });
            b.setSelected(i == this.tab);
            if (i == TAB_OVERVIEW) {
                b.setTooltip(Tooltip.create(tr("tab.overview")));
            }
            addRenderableWidget(b);
        }
        StationConsoleSyncPayload s = sync();
        if (s == null) {
            return; // "Syncing…" — controls appear once the payload lands
        }
        switch (this.tab) {
            case TAB_OVERVIEW -> overviewWidgets(s, l, t, renameText);
            case TAB_MEMBERS -> membersWidgets(s, l, t);
            case TAB_DOCKING -> dockingWidgets(s, l, t);
            case TAB_BUILD -> buildWidgets(s, l, t);
            default -> { }
        }
    }

    private SpaceButton button(int x, int y, int w, int h, Component label, boolean active, Runnable press,
            @Nullable Component tooltip) {
        SpaceButton b = new SpaceButton(this.leftPos + x, this.topPos + y, w, h, label, ACCENT, x0 -> press.run());
        b.active = active;
        if (!active && tooltip == null) {
            b.setTooltip(Tooltip.create(tr("members_only")));
        } else if (tooltip != null) {
            b.setTooltip(Tooltip.create(tooltip));
        }
        addRenderableWidget(b);
        return b;
    }

    private void overviewWidgets(StationConsoleSyncPayload s, int l, int t, @Nullable String renameText) {
        StationClass cls = StationClass.byOrdinal(this.menu.get(StationConsoleMenu.D_CLASS));
        Component trait = tr("class." + cls.id() + ".trait");
        button(114, 40, 104, 12, tr("class." + cls.id()), manage(), () -> click(StationConsoleMenu.BUTTON_CYCLE_CLASS),
                manage() ? trait : null);
        IconButton back = new IconButton(l + 220, t + 40, 12, 12, Glyphs.Glyph.ARROW_L,
                Component.literal("<"), ACCENT, x0 -> click(StationConsoleMenu.BUTTON_CYCLE_CLASS_BACK));
        back.active = manage();
        back.setTooltip(Tooltip.create(manage() ? trait : tr("members_only")));
        addRenderableWidget(back);
        if (this.renaming && manage()) {
            EditBox box = new EditBox(this.font, l + 10, t + ConsoleLayout.BUTTON_ROW_Y, 140, 12, tr("rename"));
            box.setMaxLength(32);
            box.setValue(renameText != null ? renameText : s.name());
            this.renameBox = box;
            addRenderableWidget(box);
            setFocused(box);
            box.setFocused(true);
            button(154, ConsoleLayout.BUTTON_ROW_Y, 76, 12, tr("rename_confirm"), true, this::confirmRename, null);
            return; // Abandon / Claim hidden while renaming (the box takes the row)
        }
        button(10, ConsoleLayout.BUTTON_ROW_Y, 72, 12, tr("rename"), manage(), () -> {
            this.renaming = true;
            rebuild();
        }, null);
        if (s.hasOwner()) {
            boolean armed = System.currentTimeMillis() - this.abandonArmedAt < 3000L;
            button(118, ConsoleLayout.BUTTON_ROW_Y, 112, 12, armed ? tr("abandon_confirm") : tr("abandon"), manage(), () -> {
                if (System.currentTimeMillis() - this.abandonArmedAt < 3000L) {
                    click(StationConsoleMenu.BUTTON_ABANDON);
                    this.abandonArmedAt = 0L;
                } else {
                    this.abandonArmedAt = System.currentTimeMillis();
                }
                rebuild();
            }, null);
        } else if (s.viewerRole() == StationConsoleSyncPayload.ROLE_UNOWNED && isOperator()) {
            button(118, ConsoleLayout.BUTTON_ROW_Y, 112, 12, tr("claim"), true, () -> click(StationConsoleMenu.BUTTON_CLAIM),
                    tr("claim.tooltip"));
        }
    }

    private boolean isOperator() {
        return this.minecraft != null && this.minecraft.player != null && this.minecraft.player.permissions()
                .hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
    }

    private void confirmRename() {
        if (this.renameBox != null && !this.renameBox.getValue().isBlank()) {
            ModNetwork.sendToServer(new RenameStationPayload(this.menu.slot(), this.renameBox.getValue().trim()));
        }
        this.renaming = false;
        rebuild();
    }

    private void membersWidgets(StationConsoleSyncPayload s, int l, int t) {
        if (!use()) {
            return; // visitors see the count only
        }
        String me = this.minecraft != null && this.minecraft.player != null
                ? this.minecraft.player.getUUID().toString() : "";
        if (this.addMode) {
            this.candidates.clear();
            this.candidates.addAll(onlineCandidates(s));
            for (int i = 0; i < ROWS && i + this.scroll < this.candidates.size(); i++) {
                UUID who = this.candidates.get(i + this.scroll);
                button(182, 62 + i * 12, 44, 10, tr("add"), manage(), () -> {
                    ModNetwork.sendToServer(new StationMemberPayload(this.menu.slot(), who));
                    this.addMode = false;
                    rebuild();
                }, null);
            }
            memberChevrons(this.candidates.size());
            button(10, 180, 96, 12, tr("add_cancel"), true, () -> {
                this.addMode = false;
                rebuild();
            }, null);
            return;
        }
        List<UUID> members = s.members();
        for (int i = 0; i < ROWS && i + this.scroll < members.size(); i++) {
            int index = i + this.scroll;
            boolean self = members.get(index).toString().equals(me);
            if (self) {
                button(182, 62 + i * 12, 44, 10, tr("leave"), true, () -> click(StationConsoleMenu.BUTTON_LEAVE),
                        null);
            } else if (manage()) {
                button(182, 62 + i * 12, 44, 10, tr("remove"), true,
                        () -> click(StationConsoleMenu.REMOVE_MEMBER_BASE + index), null);
            }
        }
        memberChevrons(members.size());
        boolean full = members.size() >= StationAccess.MAX_MEMBERS;
        button(10, 180, 96, 12, tr("add_member"), manage() && !full, () -> {
            this.addMode = true;
            this.scroll = 0;
            rebuild();
        }, full ? tr("member_limit") : null);
    }

    /** Scroll chevrons under the member list, only when it is longer than a page. */
    private void memberChevrons(int size) {
        if (size <= ROWS) {
            return;
        }
        int max = size - ROWS;
        IconButton up = new IconButton(this.leftPos + 10, this.topPos + 168, 28, 8,
                Glyphs.Glyph.CHEV_UP, tr("tab.members"), ACCENT, x -> scrollMembers(-1));
        up.active = this.scroll > 0;
        addRenderableWidget(up);
        IconButton down = new IconButton(this.leftPos + 42, this.topPos + 168, 28, 8,
                Glyphs.Glyph.CHEV_DOWN, tr("tab.members"), ACCENT, x -> scrollMembers(1));
        down.active = this.scroll < max;
        addRenderableWidget(down);
    }

    private void scrollMembers(int delta) {
        StationConsoleSyncPayload s = sync();
        int size = s == null ? 0 : (this.addMode ? this.candidates.size() : s.members().size());
        int next = Math.max(0, Math.min(Math.max(0, size - ROWS), this.scroll + delta));
        if (next != this.scroll) {
            this.scroll = next;
            rebuild();
        }
    }

    /** Online players from the client's tab list, minus the owner and current members. Names stay client-side. */
    private List<UUID> onlineCandidates(StationConsoleSyncPayload s) {
        List<UUID> out = new ArrayList<>();
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        if (conn == null) {
            return out;
        }
        for (PlayerInfo info : conn.getListedOnlinePlayers()) {
            UUID id = info.getProfile().id();
            if (s.members().contains(id) || s.owner().map(id::equals).orElse(Boolean.FALSE)) {
                continue;
            }
            out.add(id);
        }
        out.sort((a, b) -> displayName(a, -1).getString().compareToIgnoreCase(displayName(b, -1).getString()));
        return out;
    }

    private void dockingWidgets(StationConsoleSyncPayload s, int l, int t) {
        DockingPolicy policy = DockingPolicy.values()[Math.max(0, Math.min(1, s.docking()))];
        button(10, 52, 140, 12, tr("docking." + policy.name().toLowerCase(java.util.Locale.ROOT)), manage(),
                () -> click(StationConsoleMenu.BUTTON_TOGGLE_DOCKING), manage() ? tr("docking.tooltip") : null);
        button(10, 180, 160, 12, tr("send_visitors"), use(),
                () -> click(StationConsoleMenu.BUTTON_SEND_VISITORS_HOME), use() ? tr("send_visitors.tooltip") : null);
    }

    private void buildWidgets(StationConsoleSyncPayload s, int l, int t) {
        Map<String, Integer> have = inventoryCounts();
        int count = this.menu.get(StationConsoleMenu.D_MODULE_COUNT);
        int builtMask = this.menu.get(StationConsoleMenu.D_BUILT_MASK);
        int poweredMask = this.menu.get(StationConsoleMenu.D_POWERED_MASK);
        StationClass cls = StationClass.byOrdinal(this.menu.get(StationConsoleMenu.D_CLASS));
        for (int tile = 1; tile < ModuleKind.TILE_COUNT; tile++) {
            ModuleKind kind = ModuleKind.byTile(tile);
            if (kind == null) {
                continue;
            }
            int[] row = ConsoleLayout.buildRow(tile);
            boolean built = (builtMask & (1 << tile)) != 0;
            boolean full = kind == ModuleKind.SOLAR_WING ? s.solarWings() >= 2 : built;
            final int id = StationConsoleMenu.BUILD_BASE + tile;
            boolean affordable = ModuleCosts.satisfiable(kind, have) || creative();
            boolean classOk = kind != ModuleKind.OBSERVATORY || cls.traits().observatoryAllowed();
            boolean requiresOk = kind.requires() == null || (builtMask & (1 << kind.requires().tile())) != 0;
            boolean capOk = count < 8;
            boolean active = manage() && (full || (affordable && classOk && requiresOk && capOk));
            button(row[0] + ConsoleLayout.BUILD_BUTTON_DX, row[1] + 2, ConsoleLayout.BUILD_BUTTON_W, 12, full ? tr("repair") : tr("build"), active,
                    () -> {
                        this.buildClickAt = System.currentTimeMillis();
                        click(id);
                    }, costTooltip(kind, have));
            if (built && (kind == ModuleKind.SOLAR_WING || kind == ModuleKind.GRAVITY_DECK)) {
                boolean on = (poweredMask & (1 << tile)) != 0;
                SpaceButton toggle = button(row[0] + ConsoleLayout.TOGGLE_BUTTON_DX, row[1] + 2, ConsoleLayout.TOGGLE_BUTTON_W, 12,
                        on ? tr("on") : tr("off"), use(),
                        () -> click(StationConsoleMenu.TOGGLE_MODULE_BASE + id - StationConsoleMenu.BUILD_BASE),
                        null);
                toggle.setSelected(on);
            }
        }
    }

    private boolean creative() {
        return this.minecraft != null && this.minecraft.player != null
                && this.minecraft.player.getAbilities().instabuild;
    }

    private Map<String, Integer> inventoryCounts() {
        Map<String, Integer> counts = new HashMap<>();
        if (this.minecraft == null || this.minecraft.player == null) {
            return counts;
        }
        Inventory inv = this.minecraft.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                counts.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(),
                        Integer::sum);
            }
        }
        return counts;
    }

    private static Component costTooltip(ModuleKind kind, Map<String, Integer> have) {
        MutableComponent out = tr("module." + kind.id()).copy();
        out.append(Component.literal("\n")).append(tr("module." + kind.id() + ".desc"));
        for (Map.Entry<String, Integer> e : ModuleCosts.of(kind).entrySet()) {
            Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(e.getKey())).orElse(null);
            Component name = item == null ? Component.literal(e.getKey()) : item.getName(new ItemStack(item));
            int held = have.getOrDefault(e.getKey(), 0);
            out.append(Component.literal("\n" + e.getValue() + "x ")).append(name)
                    .append(Component.literal(held < e.getValue() ? "  (" + held + ")" : ""));
        }
        int missing = ModuleCosts.shortfall(kind, have).values().stream().mapToInt(Integer::intValue).sum();
        if (missing > 0) {
            out.append(Component.literal("\n")).append(tr("cost.short", missing).copy().withColor(BAD));
        }
        return out;
    }

    // --- input -----------------------------------------------------------------------------------

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            this.onClose();
            return true;
        }
        if (this.renameBox != null && this.renameBox.isFocused()) {
            if (event.isConfirmation()) {
                confirmRename();
                return true;
            }
            if (this.renameBox.keyPressed(event) || this.renameBox.canConsumeInput()) {
                return true;
            }
        }
        if (event.isRight() || event.isLeft()) {
            this.tab = (this.tab + (event.isRight() ? 1 : 3)) % 4;
            this.scroll = 0;
            rebuild();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (this.tab == TAB_MEMBERS) {
            StationConsoleSyncPayload s = sync();
            int size = s == null ? 0 : (this.addMode ? this.candidates.size() : s.members().size());
            int max = Math.max(0, size - ROWS);
            int next = Math.max(0, Math.min(max, this.scroll - (int) Math.signum(scrollY)));
            if (next != this.scroll) {
                this.scroll = next;
                rebuild();
            }
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        StationConsoleSyncPayload s = sync();
        StringBuilder sig = new StringBuilder();
        sig.append(s == null ? "none" : Integer.toString(s.hashCode()));
        for (int i = 0; i < StationConsoleMenu.DATA_COUNT; i++) {
            sig.append(',').append(this.menu.get(i));
        }
        sig.append(',').append(System.currentTimeMillis() - this.abandonArmedAt < 3000L);
        String now = sig.toString();
        if (!now.equals(this.signature)) {
            this.signature = now;
            rebuild();
        }
    }

    @Override
    public void removed() {
        super.removed();
        ClientStationConsole.clear();
    }

    // --- drawing ---------------------------------------------------------------------------------

    @Override
    protected void drawPanel(GuiGraphicsExtractor g) {
        super.drawPanel(g); // station_console.png: header band, rule, tab rail well and page well
        // Station sky strip in the header: black, a few fixed stars, the homeworld's teal limb, a teal rule.
        int x = this.leftPos + 6;
        int y = this.topPos + 2;
        g.fill(x, y, x + 48, y + 12, 0xFF02040A);
        for (int i = 0; i < 6; i++) {
            int sx = x + 3 + (i * 37 + 11) % 42;
            int sy = y + 1 + (i * 5 + 2) % 8;
            g.fill(sx, sy, sx + 1, sy + 1, (i & 1) == 0 ? 0xFFFFFFFF : 0xFF6E84A0);
        }
        for (int i = 0; i < 48; i++) {
            int dy = (int) Math.round(Math.sqrt(Math.max(0, 900 - (i - 24) * (i - 24))) / 6.0);
            g.fill(x + i, y + 11 - Math.min(4, dy), x + i + 1, y + 11, 0xFF17625B);
        }
        g.fill(x, y + 11, x + 48, y + 12, ACCENT);
    }

    /** A hover tooltip for a drawn (non-widget) rect, shown after the frame if the mouse is over it. */
    private void hoverTip(int x, int y, int w, int h, Component text) {
        this.hoverTips.add(new Object[] { new int[] { x, y, w, h }, text });
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        this.hoverTips.clear();
        super.extractContents(g, mouseX, mouseY, partialTick);
        int mx = mouseX - this.leftPos;
        int my = mouseY - this.topPos;
        for (Object[] tip : this.hoverTips) {
            int[] r = (int[]) tip[0];
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                g.setTooltipForNextFrame(this.font.split((Component) tip[1], 200), mouseX, mouseY);
                break;
            }
        }
    }

    /** Draws {@code text} at a panel position cut to {@code maxWidth}; the full text becomes a hover tooltip. */
    private void fitLabel(GuiGraphicsExtractor g, Component text, int dx, int dy, int maxWidth, int colour) {
        Component shown = SpaceButton.fit(this.font, text, maxWidth);
        label(g, shown, dx, dy, colour);
        if (shown != text) {
            hoverTip(dx, dy, maxWidth, 9, text);
        }
    }

    private void glyph(GuiGraphicsExtractor g, Glyphs.Glyph glyph, int dx, int dy, int colour) {
        Glyphs.draw(g, glyph, this.leftPos + dx, this.topPos + dy, colour);
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        StationConsoleSyncPayload s = sync();
        StationClass cls = StationClass.byOrdinal(this.menu.get(StationConsoleMenu.D_CLASS));
        // Header badge: Visitor (with a lock), Unclaimed (warning) or the station class.
        boolean visitor = s != null && s.viewerRole() == StationConsoleSyncPayload.ROLE_VISITOR;
        boolean unowned = s != null && s.viewerRole() == StationConsoleSyncPayload.ROLE_UNOWNED;
        Component badgeFull = visitor ? tr("visitor") : (unowned ? tr("unclaimed") : tr("class." + cls.id()));
        Component badge = SpaceButton.fit(this.font, badgeFull, ConsoleLayout.BADGE_MAX);
        int bw = this.font.width(badge);
        int bx = ConsoleLayout.BADGE_RIGHT - bw;
        label(g, badge, bx, 4, visitor ? SUBTLE : (unowned ? WARN : ACCENT));
        if (badge != badgeFull) {
            hoverTip(bx, 4, bw, 9, badgeFull);
        }
        if (visitor) {
            glyph(g, Glyphs.Glyph.LOCK, bx - 10, 4, SUBTLE);
            bx -= 10;
        }
        int nameMax = Math.min(ConsoleLayout.NAME_MAX, bx - 6 - ConsoleLayout.NAME_X);
        fitLabel(g, s == null ? tr("syncing") : Component.literal(s.name()), ConsoleLayout.NAME_X, 4, nameMax, TITLE);

        // The selected tab joins the page well (drawn after the buttons).
        int tx = this.leftPos + 6 + this.tab * 58;
        g.fill(tx + 1, this.topPos + 19 + 13, tx + 55, this.topPos + 19 + 16, WELL);
        if (s == null) {
            label(g, tr("syncing"), 12, 42, SUBTLE);
            return;
        }
        switch (this.tab) {
            case TAB_OVERVIEW -> drawOverview(g, s, cls);
            case TAB_MEMBERS -> drawMembers(g, s);
            case TAB_DOCKING -> drawDocking(g, s);
            case TAB_BUILD -> drawBuild(g, s);
            default -> { }
        }
    }

    private void drawOverview(GuiGraphicsExtractor g, StationConsoleSyncPayload s, StationClass cls) {
        int l = this.leftPos;
        int t = this.topPos;
        int built = this.menu.get(StationConsoleMenu.D_BUILT_MASK);
        int powered = this.menu.get(StationConsoleMenu.D_POWERED_MASK);
        int deckTile = ModuleKind.GRAVITY_DECK.tile();
        boolean deck = (built & (1 << deckTile)) != 0 && (powered & (1 << deckTile)) != 0;
        za.co.neroland.nerospace.client.ui.PlanetCardData data = new za.co.neroland.nerospace.client.ui.PlanetCardData(
                za.co.neroland.nerospace.registry.ModDimensions.STATION_LEVEL, Component.literal(s.name()),
                deck ? 1.0 : 0.1, za.co.neroland.nerospace.api.Hazard.NONE,
                za.co.neroland.nerospace.world.OxygenManager.SuitTier.TIER_1, false,
                za.co.neroland.nerospace.world.planet.WeatherEvent.NONE, 0F, 0.5F, true, false,
                java.util.Optional.of(Component.translatable("gui.nerospace.card.dock", s.name())),
                za.co.neroland.nerospace.client.ui.PlanetCardData.State.AVAILABLE, 1);
        za.co.neroland.nerospace.client.ui.PlanetCard.draw(g, this.font, l + 10, t + 40, data, false, false,
                System.currentTimeMillis());

        int day = this.menu.get(StationConsoleMenu.D_FOUNDED_DAY);
        fitLabel(g, day < 0 ? tr("founded_unknown", s.slot() + 1) : tr("founded", day, s.slot() + 1), 114, 56, 116,
                SUBTLE);
        label(g, tr("power"), 114, 64, POWER);
        segGauge(g, 114, 73, 116, 6, this.menu.get(StationConsoleMenu.D_POWER_PCT) / 100.0F, POWER);
        fitLabel(g, tr("power_value", s.powerFe()), 114, 82, 116, POWER);
        label(g, tr("oxygen"), 114, 94, O2);
        hGauge(g, 114, 103, 116, 6, this.menu.get(StationConsoleMenu.D_OXYGEN_PCT) / 100.0F, O2);
        fitLabel(g, tr("pressurised", s.sealed(), s.airtight()), 114, 112, 116, O2);
        glyph(g, Glyphs.Glyph.MEMBER, 114, 124, VALUE);
        fitLabel(g, tr("members_count", this.menu.get(StationConsoleMenu.D_MEMBER_COUNT), StationAccess.MAX_MEMBERS),
                124, 124, 106, VALUE);
        glyph(g, Glyphs.Glyph.ROCKET, 114, 134, VALUE);
        fitLabel(g, tr("routes", this.menu.get(StationConsoleMenu.D_ROUTE_USED),
                this.menu.get(StationConsoleMenu.D_ROUTE_CAP)), 124, 134, 106, VALUE);
        if (s.classCooldownTicks() > 0) {
            int minutes = s.classCooldownTicks() / 1200;
            fitLabel(g, tr("class_cooldown_in", minutes / 60, minutes % 60), 114, 144, 116, SUBTLE);
        }
        if (s.revealCount() > 0 && this.renameBox == null) {
            glyph(g, Glyphs.Glyph.MOD_OBSERVATORY, 114, 154, ACCENT);
            fitLabel(g, tr("card.observatory", s.revealCount()), 124, 154, 106, ACCENT);
        }

        label(g, tr("modules", this.menu.get(StationConsoleMenu.D_MODULE_COUNT)), 10, ConsoleLayout.MODULE_HEADER_Y,
                TITLE);
        for (int tile = 0; tile < ModuleKind.TILE_COUNT; tile++) {
            boolean has = (built & (1 << tile)) != 0;
            boolean on = (powered & (1 << tile)) != 0;
            ModuleKind kind = tile == 0 ? ModuleKind.CORE : ModuleKind.byTile(tile);
            if (kind == null) {
                continue;
            }
            int y = ConsoleLayout.moduleRowY(tile);
            glyph(g, Glyphs.module(tile), 10, y, has ? ACCENT : SUBTLE);
            glyph(g, has ? Glyphs.Glyph.DOT_FILLED : Glyphs.Glyph.DOT_HOLLOW, 19, y, has ? ACCENT : SUBTLE);
            Component name = tr("module." + kind.id());
            fitLabel(g, has && !on ? name.copy().append(" ").append(tr("off")) : name, 28, y, 78,
                    has ? TITLE : SUBTLE);
        }
    }

    private Component displayName(UUID id, int memberIndex) {
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        PlayerInfo info = conn == null ? null : conn.getPlayerInfo(id);
        if (info != null) {
            return Component.literal(info.getProfile().name());
        }
        return memberIndex >= 0 ? tr("member_n", memberIndex + 1) : tr("member_unknown");
    }

    private void drawMembers(GuiGraphicsExtractor g, StationConsoleSyncPayload s) {
        if (!use()) {
            fitLabel(g, tr(s.hasOwner() ? "owner_yes" : "owner_none"), 10, 40, 220, VALUE);
            fitLabel(g, tr("visitor_members", this.menu.get(StationConsoleMenu.D_MEMBER_COUNT)), 10, 52, 220, SUBTLE);
            return;
        }
        Component owner = s.owner().map(id -> displayName(id, -1)).orElse(tr("owner_none"));
        glyph(g, Glyphs.Glyph.MEMBER, 10, 40, TITLE);
        fitLabel(g, tr("owner").copy().append(" ").append(owner), 20, 40, 210, TITLE);
        label(g, tr("members_count", s.members().size(), StationAccess.MAX_MEMBERS), 10, 52, VALUE);
        if (this.addMode) {
            fitLabel(g, tr("add_pick"), 120, 52, 110, SUBTLE);
            if (this.candidates.isEmpty()) {
                fitLabel(g, tr("add_none"), 10, 64, 220, SUBTLE);
            }
            for (int i = 0; i < ROWS && i + this.scroll < this.candidates.size(); i++) {
                glyph(g, Glyphs.Glyph.MEMBER, 12, 63 + i * 12, SUBTLE);
                fitLabel(g, displayName(this.candidates.get(i + this.scroll), -1), 22, 63 + i * 12, 156, TITLE);
            }
            return;
        }
        if (s.members().isEmpty()) {
            fitLabel(g, tr("no_members"), 10, 64, 220, SUBTLE);
        }
        for (int i = 0; i < ROWS && i + this.scroll < s.members().size(); i++) {
            int index = i + this.scroll;
            glyph(g, Glyphs.Glyph.MEMBER, 12, 63 + i * 12, SUBTLE);
            fitLabel(g, displayName(s.members().get(index), index), 22, 63 + i * 12, 156, TITLE);
        }
    }

    private void drawDocking(GuiGraphicsExtractor g, StationConsoleSyncPayload s) {
        boolean ring = this.menu.get(StationConsoleMenu.D_ARRIVAL_KIND) == 1;
        glyph(g, ring ? Glyphs.Glyph.DOCK : Glyphs.Glyph.PAD, 10, 40, TITLE);
        fitLabel(g, tr("arrival").copy().append(" ").append(tr(ring ? "arrival.ring" : "arrival.pad")), 20, 40, 210,
                TITLE);
        fitLabel(g, tr("paired_count", this.menu.get(StationConsoleMenu.D_PAIRED_PADS)), 10, 66, 220, VALUE);
        if (s.pairedPads().isEmpty()) {
            fitLabel(g, tr("paired_none"), 10, 80, 220, SUBTLE);
        }
        // Compact pad cards, two rows of three; a pad that no longer stands has a dotted border and says so.
        List<StationConsoleSyncPayload.PairedPad> pads = s.pairedPads();
        for (int i = 0; i < pads.size() && i < ConsoleLayout.DOCK_CARDS; i++) {
            StationConsoleSyncPayload.PairedPad pad = pads.get(i);
            int[] r = ConsoleLayout.dockCard(i);
            drawPadCard(g, r, pad);
        }
        if (pads.size() > ConsoleLayout.DOCK_CARDS) {
            label(g, Component.literal("+" + (pads.size() - ConsoleLayout.DOCK_CARDS)), 216, 146, SUBTLE);
        }
        glyph(g, Glyphs.Glyph.ROCKET, 10, 154, SUBTLE);
        fitLabel(g, tr("routes", this.menu.get(StationConsoleMenu.D_ROUTE_USED),
                this.menu.get(StationConsoleMenu.D_ROUTE_CAP)), 20, 154, 210, SUBTLE);
        glyph(g, Glyphs.Glyph.ROCKET, 10, 166, WARN);
        fitLabel(g, tr("inbound", s.inbound()), 20, 166, 210, SUBTLE);
    }

    private void drawPadCard(GuiGraphicsExtractor g, int[] r, StationConsoleSyncPayload.PairedPad pad) {
        int x = this.leftPos + r[0];
        int y = this.topPos + r[1];
        int w = r[2];
        int h = r[3];
        String dim = pad.dimension();
        String shortDim = dim.contains(":") ? dim.substring(dim.indexOf(':') + 1) : dim;
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key = net.minecraft.resources.ResourceKey
                .create(net.minecraft.core.registries.Registries.DIMENSION, Identifier.parse(dim));
        int accent = za.co.neroland.nerospace.client.ui.PlanetAccents.of(key);
        // The planet's own (translated) name; the raw path only for a dimension Nerospace does not know.
        boolean known = key.equals(za.co.neroland.nerospace.registry.ModDimensions.OVERWORLD)
                || za.co.neroland.nerospace.world.planet.Planets.get(key) != null;
        int border = pad.standing() ? 0xFF2E4A5A : 0xFF262B33;
        if (pad.standing()) {
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, border);
        } else {
            for (int i = -1; i < w + 1; i += 2) {
                g.fill(x + i, y - 1, x + i + 1, y, border);
                g.fill(x + i, y + h, x + i + 1, y + h + 1, border);
            }
            for (int j = -1; j < h + 1; j += 2) {
                g.fill(x - 1, y + j, x, y + j + 1, border);
                g.fill(x + w, y + j, x + w + 1, y + j + 1, border);
            }
        }
        g.fill(x, y, x + w, y + h, HULL);
        g.fill(x, y, x + w, y + 2, accent);
        Glyphs.draw(g, pad.standing() ? Glyphs.Glyph.PAD : Glyphs.Glyph.LOCK, x + 2, y + 5,
                pad.standing() ? accent : SUBTLE);
        Component planet = known ? za.co.neroland.nerospace.rocket.Destinations.displayName(key)
                : Component.literal(shortDim);
        Component name = Component.literal(pad.padName());
        g.text(this.font, SpaceButton.fit(this.font, planet, w - 14), x + 12, y + 5, pad.standing() ? TITLE : SUBTLE,
                false);
        if (pad.standing()) {
            g.text(this.font, SpaceButton.fit(this.font, name, w - 4), x + 2, y + 18, VALUE, false);
        } else {
            g.text(this.font, SpaceButton.fit(this.font, tr("pad_missing"), w - 4), x + 2, y + 18, BAD, false);
        }
        hoverTip(r[0], r[1], w, h, planet.copy().append(" · " + pad.padName())
                .append(pad.standing() ? Component.empty() : Component.literal("\n").append(tr("pad_missing"))));
    }

    private void drawBuild(GuiGraphicsExtractor g, StationConsoleSyncPayload s) {
        int l = this.leftPos;
        int t = this.topPos;
        int count = this.menu.get(StationConsoleMenu.D_MODULE_COUNT);
        label(g, tr("modules", count), 10, 40, count >= 8 ? WARN : TITLE);
        int builtMask = this.menu.get(StationConsoleMenu.D_BUILT_MASK);
        StationClass cls = StationClass.byOrdinal(this.menu.get(StationConsoleMenu.D_CLASS));
        Map<String, Integer> have = inventoryCounts();
        for (int tile = 0; tile < ModuleKind.TILE_COUNT; tile++) {
            ModuleKind kind = tile == 0 ? ModuleKind.CORE : ModuleKind.byTile(tile);
            if (kind == null) {
                continue;
            }
            int[] row = ConsoleLayout.buildRow(tile);
            boolean built = (builtMask & (1 << tile)) != 0;
            boolean missing = !ModuleCosts.satisfiable(kind, have) && !creative();
            int x0 = l + row[0];
            int y0 = t + row[1];
            int border = built ? ACCENT : (missing ? 0xFF262B33 : 0xFF2E4A5A);
            g.fill(x0, y0, x0 + row[2], y0 + row[3], border);
            g.fill(x0 + 1, y0 + 1, x0 + row[2] - 1, y0 + row[3] - 1, 0xFF0C1E2B);
            glyph(g, Glyphs.module(tile), row[0] + 4, row[1] + 4, built ? ACCENT : SUBTLE);
            Component name = tr("module." + kind.id());
            Component shown = SpaceButton.fit(this.font, name, ConsoleLayout.BUILD_NAME_W);
            label(g, shown, row[0] + 16, row[1] + 4, TITLE);
            hoverTip(row[0] + 16, row[1] + 4, ConsoleLayout.BUILD_NAME_W, 9, costTooltip(kind, have));

            // State / cost column.
            int cx = row[0] + ConsoleLayout.BUILD_STATE_DX;
            boolean toggle = built && (kind == ModuleKind.SOLAR_WING || kind == ModuleKind.GRAVITY_DECK);
            int colW = toggle ? ConsoleLayout.BUILD_STATE_W_TOGGLE : ConsoleLayout.BUILD_STATE_W;
            boolean wingsLeft = kind == ModuleKind.SOLAR_WING && s.solarWings() < 2;
            if (tile == 0) {
                fitLabel(g, tr("tile.core"), cx, row[1] + 4, colW + 44, SUBTLE);
            } else if (built && !wingsLeft) {
                glyph(g, Glyphs.Glyph.DOT_FILLED, cx, row[1] + 4, ACCENT);
                Component state = kind == ModuleKind.SOLAR_WING ? tr("tile.wings", s.solarWings()) : tr("tile.built");
                if (toggle) {
                    hoverTip(cx, row[1] + 4, 8, 8, state); // the On/Off switch takes the column
                } else {
                    fitLabel(g, state, cx + 10, row[1] + 4, colW - 10, SUBTLE);
                }
            } else if (kind == ModuleKind.OBSERVATORY && !cls.traits().observatoryAllowed()) {
                fitLabel(g, tr("tile.research_only"), cx, row[1] + 4, colW, WARN);
            } else if (kind.requires() != null && (builtMask & (1 << kind.requires().tile())) == 0) {
                fitLabel(g, tr("tile.requires_habitat"), cx, row[1] + 4, colW, WARN);
            } else if (count >= 8) {
                fitLabel(g, tr("tile.max"), cx, row[1] + 4, colW, WARN);
            } else {
                drawCost(g, kind, have, wingsLeft ? s.solarWings() + " / 2 · " : "", cx, row[1] + 4, colW);
                hoverTip(cx, row[1] + 4, colW, 9, costTooltip(kind, have));
            }
        }
        int result = this.menu.get(StationConsoleMenu.D_LAST_BUILD);
        if (System.currentTimeMillis() - this.buildClickAt < 3000L && result != ModuleBuildRules.RESULT_NONE) {
            Component msg = tr("result." + result);
            if (result == ModuleBuildRules.RESULT_OBSTRUCTED && s.blockedAt().isPresent()) {
                var p = s.blockedAt().get();
                msg = tr("result.blocked_at", p.getX(), p.getY(), p.getZ());
            }
            fitLabel(g, msg, 10, 184, 220, result == ModuleBuildRules.RESULT_OK ? ACCENT : BAD);
        } else {
            fitLabel(g, tr("build_hint"), 10, 184, 220, SUBTLE);
        }
    }

    /** The cost list "8x hull · 2x titanium": items the player lacks in BAD, cut with an ellipsis at {@code maxW}. */
    private void drawCost(GuiGraphicsExtractor g, ModuleKind kind, Map<String, Integer> have, String prefix, int dx,
            int dy, int maxW) {
        List<Component> parts = new ArrayList<>();
        List<Integer> colours = new ArrayList<>();
        if (!prefix.isEmpty()) {
            parts.add(Component.literal(prefix));
            colours.add(VALUE);
        }
        boolean first = true;
        for (Map.Entry<String, Integer> e : ModuleCosts.of(kind).entrySet()) {
            Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(e.getKey())).orElse(null);
            Component name = item == null ? Component.literal(e.getKey()) : item.getName(new ItemStack(item));
            if (!first) {
                parts.add(Component.literal(" · "));
                colours.add(VALUE);
            }
            first = false;
            parts.add(tr("cost", e.getValue(), name));
            colours.add(creative() || have.getOrDefault(e.getKey(), 0) >= e.getValue() ? VALUE : BAD);
        }
        int total = 0;
        for (Component part : parts) {
            total += this.font.width(part);
        }
        int limit = total <= maxW ? maxW : maxW - this.font.width("…");
        int x = dx;
        for (int i = 0; i < parts.size(); i++) {
            Component part = parts.get(i);
            int w = this.font.width(part);
            if (x - dx + w > limit) {
                label(g, Component.literal(this.font.plainSubstrByWidth(part.getString(), Math.max(0, limit - (x - dx)))
                        + "…"), x, dy, colours.get(i));
                return;
            }
            label(g, part, x, dy, colours.get(i));
            x += w;
        }
    }
}
