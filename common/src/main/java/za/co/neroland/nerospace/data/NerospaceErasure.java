package za.co.neroland.nerospace.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.EntityTypeTest;

import za.co.neroland.nerolandcore.data.PlayerDataErasure;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.entity.AlienVillager;
import za.co.neroland.nerospace.link.NerospaceLinkEvents;
import za.co.neroland.nerospace.platform.Services;
import za.co.neroland.nerospace.progression.PlanetVisitState;
import za.co.neroland.nerospace.rocket.StationRegistry;
import za.co.neroland.nerospace.world.OxygenManager;
import za.co.neroland.nerospace.world.SavedDataRecovery;

/**
 * Registers Nerospace's player-keyed stores with Neroland Core's shared {@link PlayerDataErasure} hook, so
 * one {@code /neroland data eraseme} (or Core's retention sweep) purges Nerospace together with every
 * other Nero mod. Keyed only by UUID; player identity is never logged.
 *
 * <h2>The five stores</h2>
 * <ol>
 *   <li><b>Station ownership</b> ({@link StationRegistry}) — the owner UUID is anonymised to {@code ""},
 *       keeping the physical station as shared world content. The last-known-good backup file is rewritten
 *       immediately so the erased UUID is not retained there until the next periodic backup pass.</li>
 *   <li><b>Historical planet visits</b> ({@link PlanetVisitState}) — the player's whole row is dropped, and
 *       like stations the change is pushed into the backup file at once. This store holds only planet ids,
 *       never timestamps or coordinates, so there is nothing left to anonymise once the row is gone.</li>
 *   <li><b>Alien Villager reputation</b> — each {@link AlienVillager} keeps a {@code UUID -> score} map in
 *       its own entity NBT. This is a genuine player-data store and it is swept below.</li>
 *   <li><b>The oxygen attachment</b> — reset to a full tank.</li>
 *   <li><b>The Star Guide "seen" attachment</b> — reset to empty.</li>
 * </ol>
 *
 * <p>The two API-facing stores added alongside the visit history — external oxygen contributions and
 * terraforming overlays — are deliberately <em>not</em> listed: neither records a player, not even
 * transiently, so neither has anything to erase.</p>
 * <p>The NeroLink module's in-memory event bookkeeping (an edge latch and an alert cooldown, both keyed by
 * UUID, neither persisted) is dropped too.</p>
 *
 * <h2>What this can and cannot reach — stated honestly</h2>
 * <p>Two of the five stores are only fully reachable while the player is online or the world is loaded, and
 * pretending otherwise would be worse than saying so:</p>
 * <ul>
 *   <li><b>Alien Villagers in unloaded chunks.</b> The sweep walks every <em>loaded</em> level. A villager
 *       whose chunk is not loaded keeps the UUID in its NBT until it loads again. {@link #erasedThisSession}
 *       closes most of that window: a villager that loads later <em>in the same server session</em> has the
 *       entry stripped by {@link AlienVillager}'s load-time filter. Across a server restart the filter is
 *       gone, so a villager in a chunk that is never visited again retains the entry. Deliberately no
 *       persistent suppression list: storing the very UUID we were asked to erase, forever, in order to
 *       erase it, is a worse outcome than this bounded residue — and the residue is inert (it maps to a
 *       trade score for an account that no longer has any Nerospace record anywhere else). Re-running
 *       erasure after visiting the region closes it completely.</li>
 *   <li><b>An offline player's attachment data.</b> Both attachments are declared {@code .serialize(...)}
 *       with {@code copyOnDeath()}, so they persist in the player's own save file and do <em>not</em>
 *       "reset on their own" — the previous comment here was wrong. There is no cross-loader seam that
 *       reaches an offline player's attachments (NeoForge and Fabric both expose them only through a live
 *       {@code Player}), so for an offline player {@link #applyOnJoin} resets them the next time they log
 *       in during the same server session. Across a restart that latch is gone and the values stay until
 *       Core's account-level purge removes the player file itself.</li>
 * </ul>
 *
 * <p><b>POPIA/GDPR:</b> nothing on this path logs who was erased — the summary logs counts only.</p>
 */
public final class NerospaceErasure {

    /**
     * Players erased during THIS server session. Session-scoped and in-memory only, so it is not a
     * persistent tombstone of the accounts we were asked to forget; it exists purely to finish the job on
     * data that was out of reach at the moment of the request (an unloaded villager, an offline player).
     */
    private static final Set<UUID> ERASED_THIS_SESSION = ConcurrentHashMap.newKeySet();

    private NerospaceErasure() {
    }

    /** Registers the eraser with Core. Called once from {@code NerospaceCommon.init()}, ahead of the stores. */
    public static void init() {
        PlayerDataErasure.register(NerospaceErasure::erasePlayer);
    }

    /** Core's {@code PlayerDataEraser} body: purge one player from every Nerospace store. */
    public static void erasePlayer(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) {
            return;
        }
        ERASED_THIS_SESSION.add(uuid);

        // 1. Station ownership — anonymise, then push the anonymisation into the backup file at once.
        StationRegistry stations = StationRegistry.get(server);
        stations.forgetPlayer(uuid);
        SavedDataRecovery.backupNow(server.overworld(), StationRegistry.TYPE, stations, "nerospace:stations");

        // 2. Historical planet visits — a UUID -> planet-id set, dropped whole. Like stations, the
        // anonymisation is pushed straight into the last-known-good backup so the erased UUID is not
        // retained there until the next periodic backup pass.
        PlanetVisitState visits = PlanetVisitState.get(server);
        visits.forget(uuid);
        SavedDataRecovery.backupNow(server.overworld(), PlanetVisitState.TYPE, visits,
                PlanetVisitState.RECOVERY_NAME);

        // 3. Alien Villager reputation — held in entity NBT.
        int forgotten = sweepAlienVillagers(server, uuid);
        if (forgotten > 0) {
            // Count only — never who was erased, and never which villagers (POPIA/GDPR).
            NerospaceCommon.LOGGER.info(
                    "[Nerospace] Erasure: cleared reputation from {} loaded alien villager(s).", forgotten);
        }

        // 4 + 5. The two per-player attachments, reachable only through a live player handle.
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            resetAttachments(player);
        }

        // The link module's in-memory event bookkeeping (never persisted).
        NerospaceLinkEvents.forgetPlayer(uuid);
    }

    /**
     * Walks every loaded level for Alien Villagers still holding this UUID and drops the entry.
     *
     * <p>Erasure is a rare, explicit operation (a command or a retention sweep), so a full pass over loaded
     * entities is an acceptable cost for being certain. Nothing on a tick path calls this.</p>
     *
     * @return how many villagers actually held an entry for the player
     */
    private static int sweepAlienVillagers(MinecraftServer server, UUID uuid) {
        int forgotten = 0;
        for (ServerLevel level : server.getAllLevels()) {
            List<AlienVillager> villagers = new ArrayList<>();
            level.getEntities(EntityTypeTest.forClass(AlienVillager.class),
                    villager -> villager.remembers(uuid), villagers);
            for (AlienVillager villager : villagers) {
                if (villager.forgetPlayer(uuid)) {
                    forgotten++;
                }
            }
        }
        return forgotten;
    }

    /**
     * Finishes an erasure that landed while the player was offline: resets the two attachments the moment
     * they rejoin. Called from the join handler on every loader. A no-op for everyone else, and a no-op
     * across a restart (see the class javadoc — this latch is deliberately not persisted).
     */
    public static void applyOnJoin(ServerPlayer player) {
        if (player == null || !ERASED_THIS_SESSION.contains(player.getUUID())) {
            return;
        }
        resetAttachments(player);
        NerospaceCommon.LOGGER.info(
                "[Nerospace] Erasure: reset the oxygen + Star Guide attachments for a player who was "
                        + "offline when their erasure request was processed.");
    }

    /**
     * Whether this player was erased during the current server session. Read by {@link AlienVillager} when
     * it loads from NBT, so a villager whose chunk was unloaded at erasure time is still cleaned up.
     */
    public static boolean erasedThisSession(UUID uuid) {
        return uuid != null && ERASED_THIS_SESSION.contains(uuid);
    }

    private static void resetAttachments(ServerPlayer player) {
        Services.PLATFORM.setOxygen(player, OxygenManager.OXYGEN_MAX);
        Services.PLATFORM.setStarGuideSeen(player, List.of());
    }
}
