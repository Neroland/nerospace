package za.co.neroland.nerospace.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * Star Guide menu: no slots — just synced per-chapter completion bitmasks read live from the player's
 * advancements (bit i = step i of that chapter). Data slots sync as shorts, so masks are safe while
 * chapters stay ≤ 16 steps (enforced by {@link StarGuide}'s table shape).
 *
 * <p>Cross-loader port note (slice 1): the standalone mod also tracks a "seen" mask via a
 * {@code STAR_GUIDE_SEEN} player attachment (completed-but-unseen steps pulse). That attachment is a
 * separate cross-loader seam and is deferred — the multiloader menu syncs completion only.</p>
 */
public class StarGuideMenu extends AbstractContainerMenu {

    public static final int DATA_COUNT = StarGuide.CHAPTER_COUNT;

    private final ContainerData data;

    /** Client constructor (referenced by the {@code MenuType}). */
    public StarGuideMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, playerInventory.player);
    }

    /** Server constructor: data reads live from the player's advancements. */
    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public StarGuideMenu(int containerId, Inventory playerInventory, Player player) {
        super(ModMenuTypes.STAR_GUIDE.get(), containerId);
        this.data = player instanceof ServerPlayer serverPlayer
                ? new ProgressData(serverPlayer)
                : new SimpleContainerData(DATA_COUNT);
        checkContainerDataCount(this.data, DATA_COUNT);
        this.addDataSlots(this.data);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // no slots
    }

    // --- Screen helpers ------------------------------------------------------------------------

    public int completionMask(int chapter) {
        return this.data.get(chapter);
    }

    public boolean isStepComplete(int chapter, int step) {
        return (completionMask(chapter) & (1 << step)) != 0;
    }

    /** Live server-side view: per-chapter completion masks from the player's advancements. */
    private static final class ProgressData implements ContainerData {

        private final ServerPlayer player;

        ProgressData(ServerPlayer player) {
            this.player = player;
        }

        @Override
        public int get(int index) {
            return index >= 0 && index < StarGuide.CHAPTER_COUNT
                    ? StarGuideProgress.chapterMask(this.player, index)
                    : 0;
        }

        @Override
        public void set(int index, int value) {
            // Read-only from the client.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    }
}
