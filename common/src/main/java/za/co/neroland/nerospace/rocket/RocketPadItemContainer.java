package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

/**
 * The item half of the rocket pipe/hopper automation proxy (the counterpart to {@link RocketPadFluidProxy}):
 * a stateless single-slot {@link Container} exposed as the launch-pad block's item capability. A hopper or
 * item pipe feeding it deposits a fuel container (bucket / canister) into the docked {@link RocketEntity}'s
 * intake slot, which the rocket drains into its tank on tick — so the pad block forwards items into the
 * moving rocket entity without a cross-loader entity-capability seam.
 *
 * <p>Re-finds the rocket on every call (safe to cache per loader, no invalidation) and rejects everything
 * when no rocket is docked, so a hopper never loses an item to an empty pad.</p>
 */
public final class RocketPadItemContainer implements WorldlyContainer {

    private static final int @org.jspecify.annotations.NonNull[] SLOTS = {0};

    private final Level level;
    private final BlockPos padPos;

    public RocketPadItemContainer(Level level, BlockPos padPos) {
        this.level = level;
        this.padPos = padPos;
    }

    /** The docked rocket's 1-slot fuel-intake container, or {@code null} when no rocket is on the pad. */
    @Nullable
    private Container rocketInput() {
        RocketEntity rocket = LaunchPadMultiblock.dockedRocket(this.level, this.padPos);
        return rocket == null ? null : rocket.getFuelInput();
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        Container input = rocketInput();
        return input == null || input.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        Container input = rocketInput();
        return input == null ? ItemStack.EMPTY : input.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        Container input = rocketInput();
        return input == null ? ItemStack.EMPTY : input.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        Container input = rocketInput();
        return input == null ? ItemStack.EMPTY : input.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        Container input = rocketInput();
        if (input != null) {
            input.setItem(slot, stack);
        }
    }

    @Override
    public void setChanged() {
        Container input = rocketInput();
        if (input != null) {
            input.setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        Container input = rocketInput();
        if (input != null) {
            input.clearContent();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // Only accept fuel containers, and only while a (non-launching) rocket is docked.
        return rocketInput() != null && RocketEntity.isFuelContainer(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        // Let automation retrieve the empty bucket the rocket leaves behind, but never pull a full container.
        return !RocketEntity.isFuelContainer(stack);
    }
}

