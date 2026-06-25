package za.co.neroland.nerospace.progression;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.progression.StarGuide.Chapter;
import za.co.neroland.nerospace.progression.StarGuide.Step;

/**
 * Server-side progression queries: completion is read straight from the player's advancements — the
 * guide never keeps its own completion state. Unresolvable advancement ids (a step whose advancement
 * is missing from the datapack) count as incomplete.
 *
 * <p>Cross-loader port: pure vanilla (advancement manager); identical to the standalone mod.</p>
 */
public final class StarGuideProgress {

    private StarGuideProgress() {
    }

    /** Whether {@code step} is complete for {@code player}. */
    public static boolean isComplete(ServerPlayer player, Step step) {
        ServerPlayer checkedPlayer = java.util.Objects.requireNonNull(player);
        Step checkedStep = java.util.Objects.requireNonNull(step);
        MinecraftServer server = checkedPlayer.level().getServer();
        if (server == null) {
            return false;
        }
        ServerAdvancementManager manager = server.getAdvancements();
        AdvancementHolder holder = manager.get(java.util.Objects.requireNonNull(checkedStep.advancement()));
        return holder != null && checkedPlayer.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** Per-chapter completion bitmask (bit i = step i complete). */
    public static int chapterMask(ServerPlayer player, int chapterIndex) {
        ServerPlayer checkedPlayer = java.util.Objects.requireNonNull(player);
        Chapter chapter = java.util.Objects.requireNonNull(StarGuide.CHAPTERS.get(chapterIndex));
        int mask = 0;
        for (int i = 0; i < chapter.steps().size(); i++) {
            Step step = java.util.Objects.requireNonNull(chapter.steps().get(i));
            if (isComplete(checkedPlayer, step)) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    /**
     * The icon of the player's FIRST incomplete step in chapter order — the pedestal hologram's
     * "you are here" marker. EMPTY once everything is complete (the hologram falls back to the book).
     */
    public static ItemStack nextStepIcon(ServerPlayer player) {
        ServerPlayer checkedPlayer = java.util.Objects.requireNonNull(player);
        for (Chapter chapter : StarGuide.CHAPTERS) {
            for (Step step : chapter.steps()) {
                Step checkedStep = java.util.Objects.requireNonNull(step);
                if (!isComplete(checkedPlayer, checkedStep)) {
                    return java.util.Objects.requireNonNull(checkedStep.iconStack());
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
