package za.co.neroland.nerospace.progression;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * The Star Guide pedestal: holds the installed Star Guide Book and, while loaded, projects a hologram
 * of the nearest player's NEXT incomplete progression step (their personal "you are here" marker). The
 * hologram icon is computed server-side on a slow tick and synced via the vanilla block-entity update
 * packet.
 *
 * <p>Cross-loader port: vanilla MenuProvider + value-IO + block-entity sync; identical to the
 * standalone mod. (The client hologram renderer is the deferred cosmetic follow-up — the synced
 * hologram stack is harmless until a BER draws it.)</p>
 */
public class StarGuideBlockEntity extends BlockEntity implements MenuProvider {

    /** Server ticks between hologram refreshes (1s — progression changes are slow). */
    private static final int HOLOGRAM_INTERVAL = 20;
    /** Players within this radius drive the hologram's next-step lookup. */
    private static final double HOLOGRAM_PLAYER_RANGE = 12.0D;

    private @NonNull ItemStack book = ItemStack.EMPTY;
    /** Icon of the nearest player's next incomplete step (client-synced; EMPTY = show the book). */
    private @NonNull ItemStack hologram = ItemStack.EMPTY;

    public StarGuideBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STAR_GUIDE.get(), pos, state);
    }

    public boolean hasBook() {
        return !this.book.isEmpty();
    }

    /** Installs one book item (lectern-style). @return true if the pedestal accepted it. */
    public boolean installBook(@NonNull ItemStack stack) {
        if (hasBook() || stack.isEmpty()) {
            return false;
        }
        this.book = stack.split(1);
        markChangedAndSync();
        return true;
    }

    /** Removes and returns the installed book (EMPTY when the pedestal is bare). */
    public @NonNull ItemStack removeBook() {
        ItemStack removed = this.book;
        this.book = ItemStack.EMPTY;
        this.hologram = ItemStack.EMPTY;
        markChangedAndSync();
        return removed;
    }

    public @NonNull ItemStack getBook() {
        return this.book;
    }

    /** The hologram stack the client renderer floats above the pedestal. */
    public @NonNull ItemStack getHologram() {
        return this.hologram;
    }

    public int comparatorSignal() {
        return hasBook() ? 15 : 0;
    }

    // --- Ticking (server): refresh the hologram from the nearest player's progress -----------

    public void tick(@NonNull Level level, @NonNull BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel) || !hasBook()
                || level.getGameTime() % HOLOGRAM_INTERVAL != 0L) {
            return;
        }
        Player nearest = serverLevel.getNearestPlayer(
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, HOLOGRAM_PLAYER_RANGE, false);
        @NonNull ItemStack next = nearest instanceof ServerPlayer serverPlayer
                ? StarGuideProgress.nextStepIcon(serverPlayer)
                : ItemStack.EMPTY;
        if (!ItemStack.isSameItemSameComponents(next, this.hologram)) {
            this.hologram = next;
            markChangedAndSync();
        }
    }

    private void markChangedAndSync() {
        setChanged();
        Level currentLevel = this.level;
        if (currentLevel != null && !currentLevel.isClientSide()) {
            currentLevel.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Breaking a loaded pedestal pops the installed book (the block itself drops via loot). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level instanceof ServerLevel serverLevel && hasBook()) {
            Containers.dropItemStack(serverLevel,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, removeBook());
        }
    }

    // --- Persistence + client sync -----------------------------------------------------------

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        output.store("Book", za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC, this.book);
        output.store("Hologram", za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC, this.hologram);
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        this.book = za.co.neroland.nerospace.NerospaceCommon.orElse(
                input.read("Book", za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC), ItemStack.EMPTY);
        this.hologram = za.co.neroland.nerospace.NerospaceCommon.orElse(
                input.read("Hologram", za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC), ItemStack.EMPTY);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    // --- MenuProvider --------------------------------------------------------------------------

    @Override
    public @NonNull Component getDisplayName() {
        return Component.translatable("container.nerospace.star_guide");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @org.jspecify.annotations.NonNull Inventory playerInventory, Player player) {
        return new StarGuideMenu(containerId, playerInventory, player);
    }
}
