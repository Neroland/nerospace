package za.co.neroland.nerospace.menu;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.item.AdvancedPipeFilterItem;
import za.co.neroland.nerospace.pipe.FaceFilter;
import za.co.neroland.nerospace.pipe.FaceFilter.FilterEntry;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * Configuration menu for the {@link AdvancedPipeFilterItem} (issue #25). Nine <b>ghost</b> slots
 * (a 3×3 grid) plus the player inventory; every mutation is persisted straight into the held
 * stack's {@code ADVANCED_FILTER} component, so closing the menu never loses state.
 *
 * <p>Interactions (handled in {@link #clicked} — ghost slots never move real items):</p>
 * <ul>
 *   <li><b>Left-click a ghost slot</b> with a carried stack: set that entry (count 1). With an
 *       empty cursor: clear the entry.</li>
 *   <li><b>Right-click a ghost slot</b> with an empty cursor: cycle the entry's match mode —
 *       exact item → the ghost item's tags (in {@link FaceFilter#sortedTags} order) → item.</li>
 *   <li><b>Shift-click a player-inventory stack</b>: copy it into the first free ghost slot.</li>
 * </ul>
 *
 * <p>Two synced toggles ride {@link #clickMenuButton} (whitelist/blacklist, exact/item-only);
 * per-entry tag indices sync through {@link ContainerData} — data layout:
 * {@code [0]}=blacklist, {@code [1]}=matchComponents, {@code [2..10]}=each entry's tagIndex.
 * Cross-loader note: plain (non-extended) menu on the vanilla {@code openMenu} path, same as
 * {@link PipeConfigMenu} — no loader-specific menu API, no custom packets.</p>
 */
public class AdvancedFilterMenu extends AbstractContainerMenu {

    public static final int FILTER_SLOTS = FaceFilter.MAX_ENTRIES; // 9 (3×3)
    public static final int DATA_COUNT = 2 + FILTER_SLOTS;         // 11
    public static final int BUTTON_TOGGLE_LIST_MODE = 0;
    public static final int BUTTON_TOGGLE_COMPONENTS = 1;

    /** Ghost-grid slot coordinates (3×3 at (26,17), 18px pitch — matches the GUI texture). */
    public static final int GRID_X = 26;
    public static final int GRID_Y = 17;

    @Nullable
    private final Player player;
    @Nullable
    private final InteractionHand hand;
    /** The exact stack being edited (object identity guarded by {@link #stillValid}). */
    private final ItemStack filterStack;

    private final SimpleContainer ghosts = new SimpleContainer(FILTER_SLOTS);
    private final int[] tagIndices = new int[FILTER_SLOTS];
    private boolean blacklist;
    private boolean matchComponents = true;

    private final ContainerData data;

    /** Client constructor (referenced by the {@code MenuType}). */
    public AdvancedFilterMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, new SimpleContainerData(DATA_COUNT));
    }

    /** Server constructor: edits the Advanced Pipe Filter currently held in {@code hand}. */
    public AdvancedFilterMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        this(containerId, playerInventory, hand, null);
    }

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    private AdvancedFilterMenu(int containerId, Inventory playerInventory,
            @Nullable InteractionHand hand, @Nullable ContainerData clientData) {
        super(ModMenuTypes.ADVANCED_FILTER.get(), containerId);
        this.hand = hand;
        if (hand != null) {
            this.player = playerInventory.player;
            this.filterStack = this.player.getItemInHand(hand);
            FaceFilter initial = AdvancedPipeFilterItem.configured(this.filterStack);
            this.blacklist = initial.blacklist();
            this.matchComponents = initial.matchComponents();
            List<FilterEntry> entries = initial.entries();
            for (int i = 0; i < FILTER_SLOTS; i++) {
                FilterEntry entry = i < entries.size() ? entries.get(i) : FilterEntry.EMPTY;
                this.ghosts.setItem(i, entry.ghost().copy());
                this.tagIndices[i] = entry.tagIndex();
            }
            this.data = new ContainerData() {
                @Override
                public int get(int index) {
                    if (index == 0) {
                        return AdvancedFilterMenu.this.blacklist ? 1 : 0;
                    }
                    if (index == 1) {
                        return AdvancedFilterMenu.this.matchComponents ? 1 : 0;
                    }
                    return AdvancedFilterMenu.this.tagIndices[index - 2];
                }

                @Override
                public void set(int index, int value) {
                    // read-only from the client
                }

                @Override
                public int getCount() {
                    return DATA_COUNT;
                }
            };
        } else {
            this.player = null;
            this.filterStack = ItemStack.EMPTY;
            this.data = clientData != null ? clientData : new SimpleContainerData(DATA_COUNT);
        }
        checkContainerDataCount(this.data, DATA_COUNT);
        for (int i = 0; i < FILTER_SLOTS; i++) {
            int row = i / 3;
            int col = i % 3;
            this.addSlot(new GhostSlot(this.ghosts, i, GRID_X + col * 18, GRID_Y + row * 18));
        }
        this.addStandardInventorySlots(playerInventory, 8, 84);
        this.addDataSlots(this.data);
    }

    /** Ghost slots never move real items; all interaction goes through {@link #clicked}. */
    private static final class GhostSlot extends Slot {
        GhostSlot(SimpleContainer container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player clickingPlayer) {
        if (slotId >= 0 && slotId < FILTER_SLOTS) {
            if (clickType == ClickType.PICKUP || clickType == ClickType.PICKUP_ALL
                    || clickType == ClickType.QUICK_MOVE) {
                ItemStack carried = getCarried();
                if (!carried.isEmpty()) {
                    setEntry(slotId, carried);
                } else if (button == 1) {
                    cycleTag(slotId);
                } else {
                    clearEntry(slotId);
                }
            }
            return; // ghost grid never routes to vanilla slot handling
        }
        // Never let the open filter (or any advanced filter) be moved while its menu edits it;
        // SWAP (hotbar number keys) is blocked wholesale for the same reason.
        if (clickType == ClickType.SWAP) {
            return;
        }
        if (slotId >= 0 && slotId < this.slots.size()
                && this.slots.get(slotId).getItem().getItem() instanceof AdvancedPipeFilterItem) {
            return;
        }
        super.clicked(slotId, button, clickType, clickingPlayer);
    }

    @Override
    public ItemStack quickMoveStack(Player quickMovePlayer, int index) {
        if (index < FILTER_SLOTS) {
            clearEntry(index);
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.slots.get(index).getItem();
        if (!stack.isEmpty() && !(stack.getItem() instanceof AdvancedPipeFilterItem)) {
            for (int i = 0; i < FILTER_SLOTS; i++) {
                if (this.ghosts.getItem(i).isEmpty()) {
                    setEntry(i, stack);
                    break;
                }
            }
        }
        return ItemStack.EMPTY; // ghost copy only — the real stack never moves
    }

    @Override
    public boolean clickMenuButton(Player buttonPlayer, int id) {
        if (id == BUTTON_TOGGLE_LIST_MODE) {
            this.blacklist = !this.blacklist;
            saveToStack();
            return true;
        }
        if (id == BUTTON_TOGGLE_COMPONENTS) {
            this.matchComponents = !this.matchComponents;
            saveToStack();
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player validPlayer) {
        if (this.hand == null || this.player == null) {
            return true; // client mirror — the server decides
        }
        return this.player.getItemInHand(this.hand) == this.filterStack
                && this.filterStack.getItem() instanceof AdvancedPipeFilterItem;
    }

    // --- Entry mutation (server persists; the client mirrors for responsiveness) ---

    private void setEntry(int slot, ItemStack carried) {
        this.ghosts.setItem(slot, carried.copyWithCount(1));
        this.tagIndices[slot] = FilterEntry.MODE_ITEM;
        saveToStack();
    }

    private void clearEntry(int slot) {
        this.ghosts.setItem(slot, ItemStack.EMPTY);
        this.tagIndices[slot] = FilterEntry.MODE_ITEM;
        saveToStack();
    }

    /** Exact item → tag 0 → tag 1 → … → exact item (deterministic order both sides). */
    private void cycleTag(int slot) {
        ItemStack ghost = this.ghosts.getItem(slot);
        if (ghost.isEmpty()) {
            return;
        }
        List<TagKey<Item>> tags = FaceFilter.sortedTags(ghost);
        if (tags.isEmpty()) {
            return;
        }
        int next = this.tagIndices[slot] + 1;
        this.tagIndices[slot] = next >= tags.size() ? FilterEntry.MODE_ITEM : next;
        saveToStack();
    }

    /** Persist the working state into the held stack's component (server only). */
    private void saveToStack() {
        if (this.player == null || this.player.level().isClientSide()) {
            return;
        }
        List<FilterEntry> entries = new ArrayList<>(FILTER_SLOTS);
        for (int i = 0; i < FILTER_SLOTS; i++) {
            entries.add(new FilterEntry(this.ghosts.getItem(i).copy(), this.tagIndices[i]));
        }
        AdvancedPipeFilterItem.store(this.filterStack,
                new FaceFilter(List.copyOf(entries), this.blacklist, this.matchComponents));
    }

    // --- Screen helpers -----------------------------------------------------

    public boolean isBlacklist() {
        return this.data.get(0) != 0;
    }

    public boolean isMatchComponents() {
        return this.data.get(1) != 0;
    }

    /** The synced tag index of ghost entry {@code slot} ({@code -1} = exact item). */
    public int tagIndex(int slot) {
        return this.data.get(2 + slot);
    }

    /** The synced ghost stack of entry {@code slot}. */
    public ItemStack ghost(int slot) {
        return this.slots.get(slot).getItem();
    }
}
