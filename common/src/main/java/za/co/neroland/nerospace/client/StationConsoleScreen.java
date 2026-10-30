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
 * The Station Console screen: a 240×200 procedural panel in station teal with four client-side
 * tabs — Overview, Members, Docking, Build. Everything it shows comes from the menu's synced ints and the
 * {@link StationConsoleSyncPayload}; every button only asks the server, which re-checks permissions. Member names
 * are resolved from the client's own tab list and never stored; an unresolved member reads "Member #n".
 */
public class StationConsoleScreen extends TexturedContainerScreen<StationConsoleMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/gui/rocket.png");
    /** Station teal. */
    private static final int ACCENT = 0xFF2EC4B6;
    private static final int HULL = 0xFF0D1119;
    private static final int HEADER = 0xFF14283C;
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
        String[] tabs = { "tab.overview", "tab.members", "tab.docking", "tab.build" };
        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            SpaceButton b = new SpaceButton(l + 6 + i * 58, t + 19, 56, 14, tr(tabs[i]), ACCENT, x -> {
                this.tab = idx;
                this.scroll = 0;
                this.addMode = false;
                rebuild();
            });
            b.setSelected(i == this.tab);
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
        button(220, 40, 12, 12, Component.literal("<"), manage(),
                () -> click(StationConsoleMenu.BUTTON_CYCLE_CLASS_BACK), manage() ? trait : null);
        if (this.renaming && manage()) {
            EditBox box = new EditBox(this.font, l + 114, t + 154, 116, 12, tr("rename"));
            box.setMaxLength(32);
            box.setValue(renameText != null ? renameText : s.name());
            this.renameBox = box;
            addRenderableWidget(box);
            setFocused(box);
            box.setFocused(true);
            button(10, 170, 72, 12, tr("rename_confirm"), true, this::confirmRename, null);
        } else {
            button(10, 170, 72, 12, tr("rename"), manage(), () -> {
                this.renaming = true;
                rebuild();
            }, null);
        }
        if (s.hasOwner()) {
            boolean armed = System.currentTimeMillis() - this.abandonArmedAt < 3000L;
            button(118, 170, 112, 12, armed ? tr("abandon_confirm") : tr("abandon"), manage(), () -> {
                if (System.currentTimeMillis() - this.abandonArmedAt < 3000L) {
                    click(StationConsoleMenu.BUTTON_ABANDON);
                    this.abandonArmedAt = 0L;
                } else {
                    this.abandonArmedAt = System.currentTimeMillis();
                }
                rebuild();
            }, null);
        } else if (s.viewerRole() == StationConsoleSyncPayload.ROLE_UNOWNED && isOperator()) {
            button(118, 170, 112, 12, tr("claim"), true, () -> click(StationConsoleMenu.BUTTON_CLAIM),
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
        boolean full = members.size() >= StationAccess.MAX_MEMBERS;
        button(10, 180, 96, 12, tr("add_member"), manage() && !full, () -> {
            this.addMode = true;
            this.scroll = 0;
            rebuild();
        }, full ? tr("member_limit") : null);
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
            int[] xy = tileXY(tile);
            boolean built = (builtMask & (1 << tile)) != 0;
            boolean full = kind == ModuleKind.SOLAR_WING ? s.solarWings() >= 2 : built;
            final int id = StationConsoleMenu.BUILD_BASE + tile;
            boolean affordable = ModuleCosts.satisfiable(kind, have) || creative();
            boolean classOk = kind != ModuleKind.OBSERVATORY || cls.traits().observatoryAllowed();
            boolean requiresOk = kind.requires() == null || (builtMask & (1 << kind.requires().tile())) != 0;
            boolean capOk = count < 8;
            boolean active = manage() && (full || (affordable && classOk && requiresOk && capOk));
            button(xy[0] + 62, xy[1] + 16, 40, 10, full ? tr("repair") : tr("build"), active,
                    () -> {
                        this.buildClickAt = System.currentTimeMillis();
                        click(id);
                    }, costTooltip(kind, have));
            if (built && (kind == ModuleKind.SOLAR_WING || kind == ModuleKind.GRAVITY_DECK)) {
                boolean on = (poweredMask & (1 << tile)) != 0;
                button(xy[0] + 28, xy[1] + 16, 32, 10, on ? tr("on") : tr("off"), use(),
                        () -> click(StationConsoleMenu.TOGGLE_MODULE_BASE + id - StationConsoleMenu.BUILD_BASE),
                        null);
            }
        }
    }

    private static int[] tileXY(int tile) {
        // Core is tile 0 (drawn at the top-left), the six buildable tiles follow: two columns of 104×28.
        int i = tile;
        return new int[] { 10 + (i % 2) * 112, 50 + (i / 2) * 30 };
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
        return out;
    }

    // --- input -----------------------------------------------------------------------------------

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            this.onClose();
            return true;
        }
        if (this.renameBox != null && this.renameBox.isFocused()) {
            if (event.key() == 257 || event.key() == 335) {
                confirmRename();
                return true;
            }
            if (this.renameBox.keyPressed(event) || this.renameBox.canConsumeInput()) {
                return true;
            }
        }
        if (event.key() == 262 || event.key() == 263) { // → / ←
            this.tab = (this.tab + (event.key() == 262 ? 1 : 3)) % 4;
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
        int l = this.leftPos;
        int t = this.topPos;
        g.fill(l - 2, t - 2, l + W + 2, t + H + 2, INK);
        g.fill(l, t, l + W, t + H, HULL);
        g.fill(l, t, l + W, t + 16, HEADER);
        g.fill(l, t + 16, l + W, t + 17, ACCENT);
        g.fill(l + 6, t + 36, l + 234, t + 194, WELL);
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        StationConsoleSyncPayload s = sync();
        StationClass cls = StationClass.byOrdinal(this.menu.get(StationConsoleMenu.D_CLASS));
        label(g, s == null ? tr("syncing") : Component.literal(s.name()), 8, 4, TITLE);
        Component badge = s != null && s.viewerRole() == StationConsoleSyncPayload.ROLE_VISITOR
                ? tr("visitor") : tr("class." + cls.id());
        int bw = this.font.width(badge);
        label(g, badge, W - 8 - bw, 4, ACCENT);
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
        if (s.revealCount() > 0) {
            label(g, tr("card.observatory", s.revealCount()), 114, 154, ACCENT);
        }

        int day = this.menu.get(StationConsoleMenu.D_FOUNDED_DAY);
        label(g, day < 0 ? tr("founded_unknown", s.slot() + 1) : tr("founded", day, s.slot() + 1), 114, 56, SUBTLE);
        label(g, tr("power"), 114, 64, POWER);
        segGauge(g, 114, 73, 116, 6, this.menu.get(StationConsoleMenu.D_POWER_PCT) / 100.0F, POWER);
        label(g, tr("power_value", s.powerFe()), 114, 82, POWER);
        label(g, tr("oxygen"), 114, 94, O2);
        hGauge(g, 114, 103, 116, 6, this.menu.get(StationConsoleMenu.D_OXYGEN_PCT) / 100.0F, O2);
        label(g, tr("pressurised", s.sealed(), s.airtight()), 114, 112, O2);
        label(g, tr("members_count", this.menu.get(StationConsoleMenu.D_MEMBER_COUNT), StationAccess.MAX_MEMBERS),
                114, 124, VALUE);
        label(g, tr("routes", this.menu.get(StationConsoleMenu.D_ROUTE_USED),
                this.menu.get(StationConsoleMenu.D_ROUTE_CAP)), 114, 134, VALUE);
        if (s.classCooldownTicks() > 0) {
            int minutes = s.classCooldownTicks() / 1200;
            label(g, tr("class_cooldown_in", minutes / 60, minutes % 60), 114, 144, SUBTLE);
        }

        label(g, tr("modules", this.menu.get(StationConsoleMenu.D_MODULE_COUNT)), 10, 98, TITLE);
        int y = 108;
        for (int tile = 0; tile < ModuleKind.TILE_COUNT; tile++) {
            boolean has = (built & (1 << tile)) != 0;
            boolean on = (powered & (1 << tile)) != 0;
            ModuleKind kind = tile == 0 ? ModuleKind.CORE : ModuleKind.byTile(tile);
            if (kind == null) {
                continue;
            }
            int dot = has ? ACCENT : SUBTLE;
            g.fill(l + 10, t + y + 2, l + 14, t + y + 6, dot);
            if (!has) {
                g.fill(l + 11, t + y + 3, l + 13, t + y + 5, WELL);
            }
            Component name = tr("module." + kind.id());
            label(g, has && !on ? name.copy().append(" ").append(tr("off")) : name, 17, y, has ? TITLE : SUBTLE);
            y += 8;
        }
        if (s.viewerRole() == StationConsoleSyncPayload.ROLE_UNOWNED) {
            label(g, tr("unclaimed"), 10, 162, WARN);
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
            label(g, tr(s.hasOwner() ? "owner_yes" : "owner_none"), 10, 40, VALUE);
            label(g, tr("visitor_members", this.menu.get(StationConsoleMenu.D_MEMBER_COUNT)), 10, 52, SUBTLE);
            return;
        }
        Component owner = s.owner().map(id -> displayName(id, -1)).orElse(tr("owner_none"));
        label(g, tr("owner").copy().append(" ").append(owner), 10, 40, TITLE);
        label(g, tr("members_count", s.members().size(), StationAccess.MAX_MEMBERS), 10, 52, VALUE);
        if (this.addMode) {
            label(g, tr("add_pick"), 120, 52, SUBTLE);
            if (this.candidates.isEmpty()) {
                label(g, tr("add_none"), 10, 64, SUBTLE);
            }
            for (int i = 0; i < ROWS && i + this.scroll < this.candidates.size(); i++) {
                label(g, displayName(this.candidates.get(i + this.scroll), -1), 12, 63 + i * 12, TITLE);
            }
            return;
        }
        if (s.members().isEmpty()) {
            label(g, tr("no_members"), 10, 64, SUBTLE);
        }
        for (int i = 0; i < ROWS && i + this.scroll < s.members().size(); i++) {
            int index = i + this.scroll;
            label(g, displayName(s.members().get(index), index), 12, 63 + i * 12, TITLE);
        }
    }

    private void drawDocking(GuiGraphicsExtractor g, StationConsoleSyncPayload s) {
        label(g, tr("arrival").copy().append(" ").append(
                tr(this.menu.get(StationConsoleMenu.D_ARRIVAL_KIND) == 1 ? "arrival.ring" : "arrival.pad")), 10, 40,
                TITLE);
        label(g, tr("paired_count", this.menu.get(StationConsoleMenu.D_PAIRED_PADS)), 10, 68, VALUE);
        if (s.pairedPads().isEmpty()) {
            label(g, tr("paired_none"), 10, 80, SUBTLE);
        }
        int y = 80;
        for (StationConsoleSyncPayload.PairedPad pad : s.pairedPads()) {
            String dim = pad.dimension();
            String shortDim = dim.contains(":") ? dim.substring(dim.indexOf(':') + 1) : dim;
            label(g, Component.literal(shortDim + " · " + pad.padName()), 12, y, pad.standing() ? VALUE : BAD);
            y += 10;
        }
        label(g, tr("routes", this.menu.get(StationConsoleMenu.D_ROUTE_USED),
                this.menu.get(StationConsoleMenu.D_ROUTE_CAP)), 10, 150, SUBTLE);
        label(g, tr("inbound", s.inbound()), 10, 162, SUBTLE);
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
            int[] xy = tileXY(tile);
            boolean built = (builtMask & (1 << tile)) != 0;
            int x0 = l + xy[0];
            int y0 = t + xy[1];
            g.fill(x0, y0, x0 + 104, y0 + 28, built ? ACCENT : 0xFF2E4A5A);
            g.fill(x0 + 1, y0 + 1, x0 + 103, y0 + 27, 0xFF0C1E2B);
            label(g, tr("module." + kind.id()), xy[0] + 4, xy[1] + 3, TITLE);
            Component state;
            int colour = SUBTLE;
            if (tile == 0) {
                state = tr("tile.core");
            } else if (kind == ModuleKind.SOLAR_WING) {
                state = tr("tile.wings", s.solarWings());
                colour = s.solarWings() > 0 ? ACCENT : SUBTLE;
            } else if (built) {
                state = tr("tile.built");
                colour = ACCENT;
            } else if (kind == ModuleKind.OBSERVATORY && !cls.traits().observatoryAllowed()) {
                state = tr("tile.research_only");
                colour = WARN;
            } else if (kind.requires() != null && (builtMask & (1 << kind.requires().tile())) == 0) {
                state = tr("tile.requires_habitat");
                colour = WARN;
            } else if (count >= 8) {
                state = tr("tile.max");
                colour = WARN;
            } else if (!ModuleCosts.satisfiable(kind, have) && !creative()) {
                state = tr("tile.missing");
                colour = BAD;
            } else {
                state = tr("tile.buildable");
                colour = ACCENT;
            }
            label(g, state, xy[0] + 4, xy[1] + 16, colour);
        }
        int result = this.menu.get(StationConsoleMenu.D_LAST_BUILD);
        if (System.currentTimeMillis() - this.buildClickAt < 3000L && result != ModuleBuildRules.RESULT_NONE) {
            Component msg = tr("result." + result);
            if (result == ModuleBuildRules.RESULT_OBSTRUCTED && s.blockedAt().isPresent()) {
                var p = s.blockedAt().get();
                msg = tr("result.blocked_at", p.getX(), p.getY(), p.getZ());
            }
            label(g, msg, 10, 184, result == ModuleBuildRules.RESULT_OK ? ACCENT : BAD);
        } else {
            label(g, tr("build_hint"), 10, 184, SUBTLE);
        }
    }
}
