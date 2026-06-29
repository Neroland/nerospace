package za.co.neroland.nerospace.progression;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;

import za.co.neroland.nerolandcore.progression.CoreGates;
import za.co.neroland.nerolandcore.progression.ProgressionGates;

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
        driveCoreGates(player, stage);
    }

    /**
     * Drives Neroland Core's shared progression gates from Nerospace milestones (one-directional —
     * Nerospace only opens gates, never gates its own content on them; the Star Guide stays the
     * authority). {@code tryOpen} respects each gate's {@code requires}, so the canonical
     * {@code industrial_power → reached_orbit → first_colony → deep_space} arc only advances in order
     * and re-calling an already-open gate is a no-op. Reads existing per-player advancement completion +
     * terraform stage — no new player-keyed state (POPIA/GDPR: the gate store holds only UUID + gate id).
     * {@code reached_orbit}/{@code first_colony} are driven at their gameplay hooks (rocket launch /
     * station founding); see {@link #driveReachedOrbit} / {@code StationCharterItem.foundFromUi}.
     */
    private static void driveCoreGates(ServerPlayer player, int terraformStage) {
        // First powered machine → a built generator is the earliest power source.
        if (isDone(player, "guide/combustion_generator") || isDone(player, "guide/passive_generator")) {
            ProgressionGates.tryOpen(player, CoreGates.INDUSTRIAL_POWER);
        }
        // Terraforming a world to "living" is a deep-space colonisation milestone.
        if (terraformStage >= 3) {
            ProgressionGates.tryOpen(player, CoreGates.DEEP_SPACE);
        }
    }

    /** Opens {@code reached_orbit} when a player completes a rocket launch (called from the rocket). */
    public static void driveReachedOrbit(ServerPlayer player) {
        ProgressionGates.tryOpen(player, CoreGates.REACHED_ORBIT);
    }

    /** Opens {@code deep_space} when a player reaches a far planet (called from the rocket). */
    public static void driveDeepSpace(ServerPlayer player) {
        ProgressionGates.tryOpen(player, CoreGates.DEEP_SPACE);
    }

    /** Opens {@code first_colony} when a player founds a station (called from station founding). */
    public static void driveFirstColony(ServerPlayer player) {
        ProgressionGates.tryOpen(player, CoreGates.FIRST_COLONY);
    }

    private static boolean isDone(ServerPlayer player, String path) {
        ServerAdvancementManager manager = player.level().getServer().getAdvancements();
        AdvancementHolder holder = manager.get(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, path));
        if (holder == null) {
            return false;
        }
        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** Awards an impossible-criterion guide advancement directly (routes around the deferred ModCriteria). */
    public static void grant(ServerPlayer player, String path) {
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
