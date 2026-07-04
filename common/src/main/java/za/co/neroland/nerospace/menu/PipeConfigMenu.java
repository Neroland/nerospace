package za.co.neroland.nerospace.menu;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.item.AdvancedPipeFilterItem;
import za.co.neroland.nerospace.item.PipeFilterItem;
import za.co.neroland.nerospace.pipe.PipeIoMode;
import za.co.neroland.nerospace.pipe.PipeResourceType;
import za.co.neroland.nerospace.pipe.UniversalPipeBlockEntity;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * The Universal Pipe configuration menu. Edits one resource layer at a time across the pipe's six
 * faces (seven synced data values: [0]=selected layer, [1..6]=each face's I/O mode) <b>and</b> hosts
 * the six per-face <b>filter slots</b> (issue #25 follow-up): real slots holding the physical
 * Pipe Filter / Advanced Pipe Filter item installed on each face, so the GUI shows exactly what
 * every face is doing — drop a filter in to filter that face, take it out to clear it, hover for
 * the filter's contents (the filter items list their configuration in their tooltip).
 *
 * <p>Slot indices: {@code 0..5} = face filter slots by {@link Direction#get3DDataValue()},
 * {@code 6..41} = player inventory. Mode buttons still route through {@link #clickMenuButton}
 * (cycle-layer = 0, per-face cycle = {@code FACE_BASE + face}) — no custom packets.</p>
 */
public class PipeConfigMenu extends AbstractContainerMenu {

    public static final int DATA_COUNT = 7;
    public static final int BUTTON_CYCLE_TYPE = 0;
    /** Cycle face {@code n} (0..5 by {@link Direction#get3DDataValue()}) via button id {@code FACE_BASE + n}. */
    public static final int FACE_BASE = 1;

    public static final int FILTER_SLOTS = 6;
    /** Filter-slot column X and first-row Y (18px pitch) — the screen draws its sockets here. */
    public static final int SLOT_X = 148;
    public static final int FIRST_ROW_Y = 36;
    public static final int ROW_STEP = 18;
    public static final int INV_Y = 158;

    @Nullable
    private final UniversalPipeBlockEntity pipe;
    private final ContainerData data;
    private final Container filterItems;

    public PipeConfigMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, new SimpleContainerData(DATA_COUNT));
    }

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public PipeConfigMenu(int containerId, Inventory playerInventory,
            @Nullable UniversalPipeBlockEntity pipe, ContainerData data) {
        super(ModMenuTypes.PIPE_CONFIG.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.pipe = pipe;
        this.data = data;
        this.filterItems = pipe != null ? pipe.filterItems() : new SimpleContainer(FILTER_SLOTS);
        for (int f = 0; f < FILTER_SLOTS; f++) {
            this.addSlot(new FilterSlot(this.filterItems, f, SLOT_X, FIRST_ROW_Y + f * ROW_STEP));
        }
        this.addStandardInventorySlots(playerInventory, 8, INV_Y);
        this.addDataSlots(data);
    }

    /** A face's filter containment: one Pipe Filter / Advanced Pipe Filter item, nothing else. */
    private static final class FilterSlot extends Slot {
        FilterSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof PipeFilterItem
                    || stack.getItem() instanceof AdvancedPipeFilterItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        UniversalPipeBlockEntity current = this.pipe; // local copy so the null check holds for the analyzer
        if (current == null) {
            return false;
        }
        if (id == BUTTON_CYCLE_TYPE) {
            current.cycleConfigType();
            return true;
        }
        if (id >= FACE_BASE && id < FACE_BASE + 6) {
            current.cycleMode(Direction.from3DDataValue(id - FACE_BASE), getSelectedType());
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        UniversalPipeBlockEntity current = this.pipe;
        if (current == null) {
            return true;
        }
        return !current.isRemoved() && player.distanceToSqr(
                current.getBlockPos().getX() + 0.5, current.getBlockPos().getY() + 0.5,
                current.getBlockPos().getZ() + 0.5) < 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < FILTER_SLOTS) {
            // Face slot -> player inventory.
            if (!moveItemStackTo(stack, FILTER_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Player inventory -> first free face slot (single filter per face).
            if (!moveItemStackTo(stack, 0, FILTER_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    // --- Screen helpers -----------------------------------------------------

    /** The resource layer currently being edited. */
    public PipeResourceType getSelectedType() {
        return PipeResourceType.VALUES[Math.floorMod(this.data.get(0), PipeResourceType.VALUES.length)];
    }

    /** The I/O mode of face {@code faceIndex} (0..5 by {@link Direction#get3DDataValue()}) for the layer. */
    public PipeIoMode getFaceMode(int faceIndex) {
        return PipeIoMode.VALUES[Math.floorMod(this.data.get(1 + faceIndex), PipeIoMode.VALUES.length)];
    }
}
