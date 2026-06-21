package za.co.neroland.nerospace.progression;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.platform.Services;

/**
 * Code-grants the terraform progression advancements the multiloader can't fire with a custom criterion
 * trigger. {@code ModCriteria} is deferred (its {@code PlayerTrigger} base moved packages 26.1↔26.2), so
 * the {@code guide/terraformed_ground} and {@code guide/living_world} advancements ship as
 * {@code minecraft:impossible} (they load and keep the advancement tree intact). This awards their
 * remaining criteria directly when the player stands on terraformed / fully-living ground — replicating
 * the standalone mod's {@code PlayerTrigger} without the version-split class. Driven from the per-player
 * server tick (alongside {@code OxygenManager.tick}). {@code guide/station_charter} stays inert until the
 * station-founding system is ported.
 */
public final class StarGuideGrants {

    /** Throttle: progression is slow, so a periodic chunk-stage check is plenty. */
    private static final int CHECK_INTERVAL_TICKS = 40;

    private StarGuideGrants() {
    }

    public static void tick(ServerPlayer player) {
        if (player.tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        LevelChunk chunk = player.level().getChunkAt(player.blockPosition());
        int stage = Services.PLATFORM.getTerraformStage(chunk);
        if (stage >= 1) {
            grant(player, "guide/terraformed_ground");
        }
        if (stage >= 3) {
            grant(player, "guide/living_world");
        }
    }

    private static void grant(ServerPlayer player, String path) {
        ServerAdvancementManager manager = player.level().getServer().getAdvancements();
        AdvancementHolder holder = manager.get(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, path));
        if (holder == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        if (progress.isDone()) {
            return;
        }
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(holder, criterion);
        }
    }
}
