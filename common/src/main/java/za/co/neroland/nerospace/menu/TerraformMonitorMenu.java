package za.co.neroland.nerospace.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.machine.TerraformMonitorBlockEntity;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * Menu for the Terraform Monitor (DEEPER_TERRAFORM_DESIGN.md §6): pure readout — no slots, seven
 * synced data values (link, three stage radii, hydration, stall flag, local stage).
 */
public class TerraformMonitorMenu extends AbstractContainerMenu {

    @Nullable
    private final TerraformMonitorBlockEntity monitor;
    private final ContainerData data;

    public TerraformMonitorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null,
                new SimpleContainerData(TerraformMonitorBlockEntity.DATA_COUNT));
    }

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public TerraformMonitorMenu(int containerId, Inventory playerInventory,
            @Nullable TerraformMonitorBlockEntity monitor, ContainerData data) {
        super(ModMenuTypes.TERRAFORM_MONITOR.get(), containerId);
        checkContainerDataCount(data, TerraformMonitorBlockEntity.DATA_COUNT);
        this.monitor = monitor;
        this.data = data;
        this.addStandardInventorySlots(playerInventory, 8, 84);
        this.addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        TerraformMonitorBlockEntity local = this.monitor; // local copy for the null analysis
        return local == null || local.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // readout only — nothing to move into
    }

    // --- Screen helpers -----------------------------------------------------

    public boolean isLinked() {
        return this.data.get(0) != 0;
    }

    public int getRootedRadius() {
        return this.data.get(1);
    }

    public int getHydrationRadius() {
        return this.data.get(2);
    }

    public int getLifeRadius() {
        return this.data.get(3);
    }

    public int getHydration() {
        return this.data.get(4);
    }

    public boolean isStalled() {
        return this.data.get(5) != 0;
    }

    public int getLocalStage() {
        return this.data.get(6);
    }
}
