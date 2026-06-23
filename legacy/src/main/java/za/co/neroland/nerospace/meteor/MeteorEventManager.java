package za.co.neroland.nerospace.meteor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.Nerospace;

/**
 * Per-{@link ServerLevel} driver + persistent state for natural meteor events (meteor-events design
 * §3). Holds the live impact sites and a cooldown, schedules a rare meteor near a random online
 * player when the cooldown elapses, advances each site (SCHEDULED → spawns the falling entity →
 * LANDED), and answers "nearest site" for the tracker compass.
 *
 * <p>Modelled on {@link za.co.neroland.nerospace.world.OxygenFieldManager}: only the sites + cooldown
 * are persisted via the {@link SavedDataType} codec; everything reconverges from them on load.</p>
 */
public final class MeteorEventManager extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(Nerospace.MODID, "meteor_events");

    public static final SavedDataType<MeteorEventManager> TYPE =
            new SavedDataType<>(ID, MeteorEventManager::new, codec());

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

    private static Codec<MeteorEventManager> codec() {
        return RecordCodecBuilder.create(inst -> inst.group(
                MeteorSite.CODEC.listOf().fieldOf("sites").forGetter(m -> m.sites),
                Codec.INT.fieldOf("cooldown").forGetter(m -> m.cooldown)
        ).apply(inst, MeteorEventManager::new));
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
        if (!Config.METEOR_NATURAL_SPAWN.get() || level.players().isEmpty()) {
            return false;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return this.cooldown % 200 == 0; // persist roughly once every 10s of countdown
        }
        // Cooldown elapsed: schedule one meteor near a random online player (rarity is global per level).
        this.cooldown = nextInterval(level);
        if (countByState(MeteorSite.SCHEDULED, MeteorSite.FALLING) >= Config.METEOR_MAX_ACTIVE_SITES.get()) {
            return true;
        }
        ServerPlayer anchor = level.players().get(level.getRandom().nextInt(level.players().size()));
        BlockPos target = pickTarget(level, anchor);
        if (target != null) {
            this.sites.add(new MeteorSite(target.asLong(), MeteorSite.SCHEDULED,
                    Math.max(1, Config.METEOR_WARNING_SECONDS.get() * 20)));
            if (Config.METEOR_DEBUG_LOG.get()) {
                Nerospace.LOGGER.info("[meteor] scheduled dim={} target={}", level.dimension(), target);
            }
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
            if (site.state != MeteorSite.LANDED && site.blockPos().closerThan(pos, 8.0D)) {
                site.state = MeteorSite.LANDED;
                site.timer = LANDED_EXPIRY_TICKS;
                site.pos = pos.asLong();
                setDirty();
                return;
            }
        }
        // Creative-spawned (or unscheduled) meteor: add a transient landed site for the tracker.
        this.sites.add(new MeteorSite(pos.asLong(), MeteorSite.LANDED, LANDED_EXPIRY_TICKS));
        setDirty();
    }

    /** Nearest tracked site to {@code from} (any state), or {@code null} if none. For the tracker. */
    @Nullable
    public MeteorSite nearestSite(BlockPos from) {
        MeteorSite best = null;
        double bestSq = Double.MAX_VALUE;
        for (MeteorSite site : this.sites) {
            double sq = site.blockPos().distSqr(from);
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
        int avg = Math.max(1, Config.METEOR_AVG_INTERVAL_SECONDS.get()) * 20;
        // Spread 0.66x .. 1.33x of the average so impacts feel irregular.
        return (int) (avg * 0.66D) + level.getRandom().nextInt(Math.max(1, (int) (avg * 0.67D)));
    }

    @Nullable
    private static BlockPos pickTarget(ServerLevel level, ServerPlayer anchor) {
        int min = Math.max(0, Config.METEOR_MIN_DISTANCE.get());
        int max = Math.max(min + 1, Config.METEOR_MAX_DISTANCE.get());
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
