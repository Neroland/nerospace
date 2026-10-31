package za.co.neroland.nerospace.compat.mc;

import java.util.function.Consumer;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

/**
 * Version seam for menus. 1.21.2 gave {@code AbstractContainerMenu} {@code addStandardInventorySlots(..)}; on
 * Minecraft 1.21.1 the build rewrites {@code this.addStandardInventorySlots(inv, x, y)} to this helper (fed the menu's
 * own {@code addSlot}; see legacyRenames in stonecutter.gradle). Same layout as vanilla: the 3x9 main inventory at
 * (x, y) and the hotbar 58 pixels below it.
 */
public final class MenuCompat {

    private MenuCompat() {
    }

    public static void addStandardInventorySlots(Consumer<Slot> addSlot, Container inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot.accept(new Slot(inventory, col + (row + 1) * 9, left + col * 18, top + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot.accept(new Slot(inventory, col, left + col * 18, top + 58));
        }
    }
}
