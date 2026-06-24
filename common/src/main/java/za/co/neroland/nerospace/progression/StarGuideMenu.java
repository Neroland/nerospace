package za.co.neroland.nerospace.progression;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.platform.Services;
import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * Star Guide menu: no slots — just synced per-chapter data. Slots {@code [0..N)} = completion masks
 * read live from the player's advancements (bit i = step i of that chapter); slots {@code [N..2N)} =
 * "seen" masks from the {@code STAR_GUIDE_SEEN} player attachment (a completed-but-unseen step pulses
 * in the GUI until clicked). Clicking a step sends a menu button ({@code chapter * 16 + step}) that
 * marks it seen server-side. Data slots sync as shorts, so masks are safe while chapters stay ≤ 16
 * steps (enforced by {@link StarGuide}'s table shape).
 *
 * <p>Cross-loader port: the "seen" attachment is reached through the {@link Services#PLATFORM} seam
 * (NeoForge {@code player.getData}/Fabric {@code getAttachedOrCreate}) instead of the root's direct
 * {@code ModAttachments} access.</p>
 */
public class StarGuideMenu extends AbstractContainerMenu {

    public static final int DATA_COUNT = StarGuide.CHAPTER_COUNT * 2;

    private final ContainerData data;

    /** Client constructor (referenced by the {@code MenuType}). */
    public StarGuideMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, playerInventory.player);
    }

    /** Server constructor: data reads live from the player's advancements + seen attachment. */
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

    /** Step click → mark seen (button id = chapter * 16 + step). */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        int chapter = id / 16;
        int step = id % 16;
        if (chapter < 0 || chapter >= StarGuide.CHAPTER_COUNT
                || step >= StarGuide.CHAPTERS.get(chapter).steps().size()) {
            return false;
        }
        List<Integer> seen = new ArrayList<>(Services.PLATFORM.getStarGuideSeen(player));
        while (seen.size() < StarGuide.CHAPTER_COUNT) {
            seen.add(0);
        }
        seen.set(chapter, seen.get(chapter) | (1 << step));
        Services.PLATFORM.setStarGuideSeen(player, List.copyOf(seen));
        return true;
    }

    // --- Screen helpers ------------------------------------------------------------------------

    public int completionMask(int chapter) {
        return this.data.get(chapter);
    }

    public int seenMask(int chapter) {
        return this.data.get(StarGuide.CHAPTER_COUNT + chapter);
    }

    public boolean isStepComplete(int chapter, int step) {
        return (completionMask(chapter) & (1 << step)) != 0;
    }

    public boolean isStepSeen(int chapter, int step) {
        return (seenMask(chapter) & (1 << step)) != 0;
    }

    /** Live server-side view: advancements (completion) + the seen attachment. */
    private static final class ProgressData implements ContainerData {

        private final ServerPlayer player;

        ProgressData(ServerPlayer player) {
            this.player = player;
        }

        @Override
        public int get(int index) {
            if (index < StarGuide.CHAPTER_COUNT) {
                return StarGuideProgress.chapterMask(this.player, index);
            }
            List<Integer> seen = Services.PLATFORM.getStarGuideSeen(this.player);
            int chapter = index - StarGuide.CHAPTER_COUNT;
            return chapter >= 0 && chapter < seen.size() ? seen.get(chapter) : 0;
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
