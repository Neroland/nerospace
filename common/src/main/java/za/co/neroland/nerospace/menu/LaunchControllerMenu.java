package za.co.neroland.nerospace.menu;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModMenuTypes;
import za.co.neroland.nerospace.rocket.LaunchControllerBlockEntity;

/**
 * Menu for the Launch Controller: three material slots (Launch Pad / Station Wall / Launch Gantry), the
 * player inventory, and synced data (target tier + needed counts + buildability). Buttons route through
 * {@link #clickMenuButton} — ids 1–4 pick the target tier, id 0 triggers the build.
 */
public class LaunchControllerMenu extends AbstractContainerMenu {

    public static final int BUTTON_BUILD = 0;
    /** Pick target tier {@code n} via button id {@code n} (1..4). */
    public static final int TIER_BASE = 0;
    /** Toggle the holographic pad preview. */
    public static final int BUTTON_TOGGLE_HOLOGRAM = 5;
    /** Switch between pad-build and launch modes. */
    public static final int BUTTON_TOGGLE_MODE = 6;
    /** Cycle the docked rocket's destination (launch mode). */
    public static final int BUTTON_CYCLE_DEST = 7;
    /** Launch the docked rocket (launch mode). */
    public static final int BUTTON_LAUNCH = 8;

    private static final int SLOTS = 3;
    private static final int DATA_COUNT = 21;

    private final Container container;
    private final ContainerData data;
    @Nullable
    private final LaunchControllerBlockEntity controller;

    public LaunchControllerMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(SLOTS), new SimpleContainerData(DATA_COUNT), null);
    }

    @SuppressWarnings("this-escape")
    public LaunchControllerMenu(int id, Inventory playerInventory, Container container, ContainerData data,
            @Nullable LaunchControllerBlockEntity controller) {
        super(ModMenuTypes.LAUNCH_CONTROLLER.get(), id);
        checkContainerSize(container, SLOTS);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        this.controller = controller;

        this.addSlot(new MaterialSlot(container, LaunchControllerBlockEntity.SLOT_PAD, 44, 40));
        this.addSlot(new MaterialSlot(container, LaunchControllerBlockEntity.SLOT_WALL, 70, 40));
        this.addSlot(new MaterialSlot(container, LaunchControllerBlockEntity.SLOT_GANTRY, 96, 40));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 118 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 176));
        }
        this.addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        LaunchControllerBlockEntity current = this.controller;
        if (current == null) {
            return false;
        }
        if (id == BUTTON_BUILD) {
            current.build(player);
            return true;
        }
        if (id == BUTTON_TOGGLE_HOLOGRAM) {
            current.toggleHologram();
            return true;
        }
        if (id == BUTTON_TOGGLE_MODE) {
            current.toggleMode();
            return true;
        }
        if (id == BUTTON_CYCLE_DEST) {
            current.cycleRocketDestination();
            return true;
        }
        if (id == BUTTON_LAUNCH) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                    && current.launchRocket(serverPlayer)) {
                serverPlayer.closeContainer(); // watch the ascent
            }
            return true;
        }
        if (id >= 1 && id <= 4) {
            current.setTargetTier(id);
            return true;
        }
        return false;
    }

    public int targetTier() {
        return this.data.get(0);
    }

    public int neededPads() {
        return this.data.get(1);
    }

    public int neededWall() {
        return this.data.get(2);
    }

    public int neededGantry() {
        return this.data.get(3);
    }

    public boolean canBuild() {
        return this.data.get(4) != 0;
    }

    public boolean isHologram() {
        return this.data.get(5) != 0;
    }

    public float fuelFrac() {
        return frac(6, 7);
    }

    public float oxygenFrac() {
        return frac(8, 9);
    }

    public float powerFrac() {
        return frac(10, 11);
    }

    private float frac(int amount, int cap) {
        int c = this.data.get(cap);
        return c <= 0 ? 0f : Math.min(1f, this.data.get(amount) / (float) c);
    }

    // --- Launch mode ---------------------------------------------------------

    public boolean isLaunchMode() {
        return this.data.get(12) == 1;
    }

    public boolean rocketPresent() {
        return this.data.get(13) != 0;
    }

    public int rocketTier() {
        return this.data.get(14) + 1; // ordinal → 1-based tier
    }

    public float rocketFuelFrac() {
        return this.data.get(15) / 100f;
    }

    public float rocketOxygenFrac() {
        return this.data.get(16) / 100f;
    }

    public float rocketPowerFrac() {
        return this.data.get(17) / 100f;
    }

    public boolean rocketLaunchable() {
        return this.data.get(20) != 0;
    }

    /** Display name of the docked rocket's selected destination. */
    public String rocketDestName() {
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key =
                za.co.neroland.nerospace.rocket.Destinations.byIndex(this.data.get(18));
        return key == null ? "—" : za.co.neroland.nerospace.rocket.Destinations.name(key);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack raw = slot.getItem();
            moved = raw.copy();
            int invStart = SLOTS;
            int invEnd = invStart + 36;
            if (index < invStart) {
                if (!this.moveItemStackTo(raw, invStart, invEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(raw, 0, invStart, false)) {
                return ItemStack.EMPTY;
            }
            if (raw.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return moved;
    }

    /** A material slot that only accepts the pad block its index expects. */
    private class MaterialSlot extends Slot {
        private final int slotIndex;

        MaterialSlot(Container container, int slotIndex, int x, int y) {
            super(container, slotIndex, x, y);
            this.slotIndex = slotIndex;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            LaunchControllerBlockEntity c = controller;
            return c == null || c.accepts(this.slotIndex, stack);
        }
    }
}
