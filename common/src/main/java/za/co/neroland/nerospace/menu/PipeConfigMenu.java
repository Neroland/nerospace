package za.co.neroland.nerospace.menu;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.pipe.PipeIoMode;
import za.co.neroland.nerospace.pipe.PipeResourceType;
import za.co.neroland.nerospace.pipe.UniversalPipeBlockEntity;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * The Universal Pipe configuration menu (advanced-pipes slice B). A slot-less menu that edits one
 * resource layer at a time across the pipe's six faces: seven synced data values ([0]=selected layer,
 * [1..6]=each face's I/O mode for that layer). Buttons route through {@link #clickMenuButton} so no
 * custom packet is needed — the cycle-layer and per-face cycle buttons are handled server-side where
 * the menu holds the {@link UniversalPipeBlockEntity}.
 *
 * <p>Cross-loader note: a plain (non-extended) menu opened via the vanilla {@code openMenu} path, so it
 * needs no loader-specific extended-menu API and no client-screen-open seam — the standalone mod's
 * client-screen + {@code SetPipeModePayload} approach is replaced by this server-authoritative menu.</p>
 */
public class PipeConfigMenu extends AbstractContainerMenu {

    public static final int DATA_COUNT = 7;
    public static final int BUTTON_CYCLE_TYPE = 0;
    /** Cycle face {@code n} (0..5 by {@link Direction#get3DDataValue()}) via button id {@code FACE_BASE + n}. */
    public static final int FACE_BASE = 1;

    @Nullable
    private final UniversalPipeBlockEntity pipe;
    private final @org.jspecify.annotations.NonNull ContainerData data;

    public PipeConfigMenu(int containerId, @org.jspecify.annotations.NonNull Inventory playerInventory) {
        this(containerId, playerInventory, null, new SimpleContainerData(DATA_COUNT));
    }

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public PipeConfigMenu(int containerId, @org.jspecify.annotations.NonNull Inventory playerInventory,
            @Nullable UniversalPipeBlockEntity pipe, @org.jspecify.annotations.NonNull ContainerData data) {
        super(ModMenuTypes.PIPE_CONFIG.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.pipe = pipe;
        this.data = data;
        this.addDataSlots(data);
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
        return ItemStack.EMPTY; // slot-less config readout — nothing to move
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
