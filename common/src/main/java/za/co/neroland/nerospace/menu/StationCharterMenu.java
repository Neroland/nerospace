package za.co.neroland.nerospace.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * A slotless menu whose only job is to open the Station Charter naming screen cross-loader (via
 * {@code openMenu}). The screen sends the typed name back to the server as a {@code FoundStationPayload};
 * this menu carries no state.
 */
public class StationCharterMenu extends AbstractContainerMenu {

    public StationCharterMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.STATION_CHARTER.get(), containerId);
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
