package za.co.neroland.nerospace.meteor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Per-{@link ServerLevel} driver + persistent state for natural meteor events (meteor-events design
 * §3). Holds the live impact sites and a cooldown, schedules a rare meteor near a random online
 * player when the cooldown elapses, advances each site (SCHEDULED → spawns the falling entity →
 * LANDED), and answers "nearest site" for the tracker.
 *
 * <p>Cross-loader port note: the first {@link SavedData} in the multiloader (vanilla
 * {@code SavedDataType} codec — only the sites + cooldown persist; everything reconverges from them on
 * load). The meteor pacing config keys are not yet ported, so they are inlined to the root's shipped
 * defaults; the config seam is a deferred incremental batch. {@link #nearestSite} is consumed by the
 * deferred tracker batch (item + sync payload + client HUD).</p>
 */
public final class MeteorEventManager extends SavedData {

    public static final @org.jspecify.annotations.NonNull Identifier ID = NerospaceCommon.id("meteor_events");

    // 26.x NeoForm (pure vanilla) exposes only the 4-arg ctor. Custom mod saved data has no vanilla
    // schema; the command-storage saved-data type is the narrowest generic saved-data fixer.
    public static final @NonNull SavedDataType<MeteorEventManager> TYPE =
            new SavedDataType<>(ID, MeteorEventManager::new, codec(), DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    // --- Inlined from Config (root shipped defaults) until the config seam lands ---
    /** Whether meteors fall naturally near players. */
    private static final boolean NATURAL_SPAWN = true;
    /** Max simultaneous scheduled/falling meteors tracked per dimension. */
    private static final int MAX_ACTIVE_SITES = 4;
    /** Warning window (seconds) a meteor is tracked as 'incoming' before it falls. */
    private static final int WARNING_SECONDS = 30;
    /** Average seconds between natural impacts on an eligible dimension with players online (~2.5h). */
    private static final int AVG_INTERVAL_SECONDS = 9000;
    /** Minimum horizontal distance (blocks) from the anchor player a meteor targets. */
    private static final int MIN_DISTANCE = 200;
    /** Maximum horizontal distance (blocks) from the anchor player a meteor targets. */
    private static final int MAX_DISTANCE = 500;

    /** Ticks a landed site lingers so the tracker can still lead players to a fresh crater (5 min). */
    private static final int LANDED_EXPIRY_TICKS = 6000;
    /** Failsafe: drop a FALLING site if its entity never reports impact (e.g. unloaded). */
    private static final int FALLING_TIMEOUT_TICKS = 600;

    private final List<MeteorSite> sites;
    private int cooldown;

    public MeteorEventManager() {
        this(new ArrayList<>(), 0);
    }

    private MeteorEventManager(List<MeteorSite> sites, int cooldown) {
        this.sites = new ArrayList<>(sites);
        this.cooldown = cooldown;
    }

    private static @NonNull Codec<@NonNull MeteorEventManager> codec() {
        return NerospaceCommon.requireNonNull(RecordCodecBuilder.create(inst -> inst.group(
                MeteorSite.CODEC.listOf().fieldOf("sites").forGetter(m -> m.sites),
                Codec.INT.fieldOf("cooldown").forGetter(m -> m.cooldown)
        ).apply(inst, MeteorEventManager::of)));
    }

    private static @NonNull MeteorEventManager of(List<MeteorSite> sites, Integer cooldown) {
        return new MeteorEventManager(NerospaceCommon.requireNonNull(sites),
                NerospaceCommon.requireNonNull(cooldown).intValue());
    }

    public static MeteorEventManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // --- Tick driver --------------------------------------------------------

    /** One server tick on an eligible dimension (called from {@link MeteorEvents}). */
    public void tick(ServerLevel level) {
        boolean dirty = scheduleIfDue(level);
        dirty |= advanceSites(level);
        if (dirty) {
            setDirty();
        }
    }

    private boolean scheduleIfDue(ServerLevel level) {
        if (!NATURAL_SPAWN || level.players().isEmpty()) {
            return false;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return this.cooldown % 200 == 0; // persist roughly once every 10s of countdown
        }
        // Cooldown elapsed: schedule one meteor near a random online player (rarity is global per level).
        this.cooldown = nextInterval(level);
        if (countByState(MeteorSite.SCHEDULED, MeteorSite.FALLING) >= MAX_ACTIVE_SITES) {
            return true;
        }
        ServerPlayer anchor = level.players().get(level.getRandom().nextInt(level.players().size()));
        BlockPos target = pickTarget(level, anchor);
        if (target != null) {
            this.sites.add(new MeteorSite(target.asLong(), MeteorSite.SCHEDULED,
                    Math.max(1, WARNING_SECONDS * 20)));
        }
        return true;
    }

    private boolean advanceSites(ServerLevel level) {
        boolean dirty = false;
        Iterator<MeteorSite> it = this.sites.iterator();
        while (it.hasNext()) {
            MeteorSite site = it.next();
            switch (site.state) {
                case MeteorSite.SCHEDULED -> {
                    if (--site.timer <= 0) {
                        FallingMeteorEntity.spawn(level, site.blockPos(), level.getRandom().nextLong());
                        site.state = MeteorSite.FALLING;
                        site.timer = FALLING_TIMEOUT_TICKS;
                        dirty = true;
                    }
                }
                case MeteorSite.FALLING -> {
                    if (--site.timer <= 0) {
                        it.remove(); // failsafe: entity never impacted
                        dirty = true;
                    }
                }
                case MeteorSite.LANDED -> {
                    if (--site.timer <= 0) {
                        it.remove();
                        dirty = true;
                    }
                }
                default -> it.remove();
            }
        }
        return dirty;
    }

    /** Called by {@link FallingMeteorEntity} on impact: flip the matching site to LANDED (or add one). */
    public void onImpact(BlockPos pos) {
        for (MeteorSite site : this.sites) {
            BlockPos checkedPos = NerospaceCommon.requireNonNull(pos);
            if (site.state != MeteorSite.LANDED && site.blockPos().closerThan(checkedPos, 8.0D)) {
                site.state = MeteorSite.LANDED;
                site.timer = LANDED_EXPIRY_TICKS;
                site.pos = checkedPos.asLong();
                setDirty();
                return;
            }
        }
        // Creative-spawned (or unscheduled) meteor: add a transient landed site for the tracker.
        this.sites.add(new MeteorSite(NerospaceCommon.requireNonNull(pos).asLong(), MeteorSite.LANDED, LANDED_EXPIRY_TICKS));
        setDirty();
    }

    /** Nearest tracked site to {@code from} (any state), or {@code null} if none. For the tracker. */
    @Nullable
    public MeteorSite nearestSite(BlockPos from) {
        MeteorSite best = null;
        double bestSq = Double.MAX_VALUE;
        for (MeteorSite site : this.sites) {
            double sq = site.blockPos().distSqr(NerospaceCommon.requireNonNull(from));
            if (sq < bestSq) {
                bestSq = sq;
                best = site;
            }
        }
        return best;
    }

    // --- Helpers ------------------------------------------------------------

    private int countByState(int... states) {
        int n = 0;
        for (MeteorSite site : this.sites) {
            for (int s : states) {
                if (site.state == s) {
                    n++;
                    break;
                }
            }
        }
        return n;
    }

    private static int nextInterval(ServerLevel level) {
        int avg = Math.max(1, AVG_INTERVAL_SECONDS) * 20;
        // Spread 0.66x .. 1.33x of the average so impacts feel irregular.
        return (int) (avg * 0.66D) + level.getRandom().nextInt(Math.max(1, (int) (avg * 0.67D)));
    }

    @Nullable
    private static BlockPos pickTarget(ServerLevel level, ServerPlayer anchor) {
        int min = Math.max(0, MIN_DISTANCE);
        int max = Math.max(min + 1, MAX_DISTANCE);
        double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
        double d = min + level.getRandom().nextDouble() * (max - min);
        int x = (int) Math.floor(anchor.getX() + Math.cos(angle) * d);
        int z = (int) Math.floor(anchor.getZ() + Math.sin(angle) * d);
        // getHeight loads/generates the target chunk — acceptable for a rare event.
        int surfaceAir = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int groundY = surfaceAir - 1;
        if (groundY <= level.getMinY() + 1) {
            return null; // void / no terrain
        }
        return new BlockPos(x, groundY, z);
    }
}
