package za.co.neroland.nerospace.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * A slotless menu that opens the station naming screen cross-loader (via {@code openMenu}). It carries
 * two synced ints: a mode (0 = found a new station, 1 = rename an existing one) and the target station
 * slot (for rename). The screen reads these and sends the typed name back as a {@code FoundStationPayload}
 * or {@code RenameStationPayload}. Station slots are small (0–63) so they survive the 16-bit sync.
 */
public class StationCharterMenu extends AbstractContainerMenu {

    public static final int MODE_FOUND = 0;
    public static final int MODE_RENAME = 1;

    private final ContainerData data;

    /** Client constructor (menu type): state arrives via the synced data. */
    public StationCharterMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainerData(2));
    }

    /** Server constructor for a given mode + slot. */
    public StationCharterMenu(int containerId, Inventory playerInventory, int mode, int slot) {
        this(containerId, playerInventory, dataOf(mode, slot));
    }

    @SuppressWarnings("this-escape")
    private StationCharterMenu(int containerId, Inventory playerInventory, ContainerData data) {
        super(ModMenuTypes.STATION_CHARTER.get(), containerId);
        this.data = data;
        addDataSlots(data);
    }

    private static ContainerData dataOf(int mode, int slot) {
        SimpleContainerData data = new SimpleContainerData(2);
        data.set(0, mode);
        data.set(1, slot);
        return data;
    }

    public int mode() {
        return this.data.get(0);
    }

    public int slot() {
        return this.data.get(1);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
