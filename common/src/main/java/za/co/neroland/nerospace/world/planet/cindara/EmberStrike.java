package za.co.neroland.nerospace.world.planet.cindara;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import za.co.neroland.nerospace.block.LoadedChunks;
import za.co.neroland.nerospace.block.rules.LightningRules;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.registry.ModTags;
import za.co.neroland.nerospace.world.planet.WeatherEffects;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.planet.WeatherMachine;

/**
 * Cindara ember strikes. While an Ash Fall runs at intensity ≥ {@link LightningRules#MIN_INTENSITY}, a strike step
 * runs at most once per {@code weather.emberStrikeIntervalTicks} (default 100) per level. Each step tries one random
 * surface column in a loaded chunk near each player (one strike per player area per step), and a level never takes
 * more than {@link #LEVEL_CAP_PER_SECOND} strikes in one second. Only columns with sky access are struck; a column
 * topped by a {@code #nerospace:lightning_safe} block (Basalt Spire) never is. A strike hurts and ignites the entities
 * it hits like vanilla lightning but places no fire on blocks (so player builds are never set alight).
 * The only state is the last step's game time per level (no player data).
 */
public final class EmberStrike implements WeatherEffects {

    /** Hard ceiling on strikes per level per second (a step runs at most once per second). */
    public static final int LEVEL_CAP_PER_SECOND = 4;

    /** Vanilla lightning's entity hit box around the bolt (x/z ±3, y −3..+9). */
    private static final double HIT_XZ = 3.0;
    private static final double HIT_DOWN = 3.0;
    private static final double HIT_UP = 9.0;

    /** No step has run in this level yet. */
    static final long NEVER = Long.MIN_VALUE;

    /** Level → game time of its last strike step (transient, weakly held). */
    private static final Map<ServerLevel, Long> LAST_STEP = Collections.synchronizedMap(new WeakHashMap<>());

    private EmberStrike() {
    }

    static void register() {
        WeatherEffects.Registry.register(WeatherEvent.ASH_FALL, new EmberStrike());
    }

    /**
     * Pure gate: a strike step is due in a strong Ash Fall once {@code intervalTicks} have passed since the last
     * step (a clock that ran backwards, e.g. after {@code /time set}, also restarts the cadence).
     */
    public static boolean stepDue(boolean ashFall, double intensity, long now, long lastStep, int intervalTicks) {
        if (!LightningRules.emberStorm(ashFall, intensity)) {
            return false;
        }
        return lastStep == NEVER || now < lastStep || now - lastStep >= intervalTicks;
    }

    /** Pure: strikes a step may place — one per player area, never more than the level cap. */
    public static int strikeBudget(int playerAreas) {
        return Math.max(0, Math.min(playerAreas, LEVEL_CAP_PER_SECOND));
    }

    /** Pure: a column may be struck only with sky access and a top that is not lightning-safe. */
    public static boolean columnStrikable(boolean skyAccess, boolean topIsSafe) {
        return skyAccess && LightningRules.strikeAllowed(topIsSafe);
    }

    @Override
    public void onSecond(ServerLevel level, WeatherMachine weather) {
        long now = level.getGameTime();
        Long last = LAST_STEP.get(level);
        if (!stepDue(weather.event == WeatherEvent.ASH_FALL, weather.intensity, now, last == null ? NEVER : last,
                NerospaceConfig.emberStrikeIntervalTicks())) {
            return;
        }
        LAST_STEP.put(level, now);
        List<ServerPlayer> players = level.players();
        int budget = strikeBudget(players.size());
        if (budget == 0) {
            return;
        }
        RandomSource random = level.getRandom();
        int start = random.nextInt(players.size()); // fair rotation when the level cap trims the list
        int span = 2 * LoadedChunks.RADIUS + 1;
        for (int i = 0; i < players.size() && budget > 0; i++) {
            ServerPlayer player = players.get((start + i) % players.size());
            int cx = (player.getBlockX() >> 4) + random.nextInt(span) - LoadedChunks.RADIUS;
            int cz = (player.getBlockZ() >> 4) + random.nextInt(span) - LoadedChunks.RADIUS;
            if (level.getChunkSource().getChunkNow(cx, cz) == null) {
                continue;
            }
            int x = (cx << 4) + random.nextInt(16);
            int z = (cz << 4) + random.nextInt(16);
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
            if (top.getY() <= level.getMinY()) {
                continue;
            }
            boolean safe = level.getBlockState(top.below()).is(ModTags.Blocks.LIGHTNING_SAFE);
            if (columnStrikable(level.canSeeSky(top), safe)) {
                strike(level, top);
                budget--;
            }
        }
    }

    private static void strike(ServerLevel level, BlockPos at) {
        //? if >=26.2 {
        LightningBolt bolt = new LightningBolt(net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT, level);
        //?} else {
        /*LightningBolt bolt = new LightningBolt(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, level);
        *///?}
        bolt.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        bolt.setVisualOnly(true); // no block fire; entity hits are applied below exactly once
        level.addFreshEntity(bolt);
        AABB box = new AABB(bolt.getX() - HIT_XZ, bolt.getY() - HIT_DOWN, bolt.getZ() - HIT_XZ,
                bolt.getX() + HIT_XZ, bolt.getY() + HIT_UP, bolt.getZ() + HIT_XZ);
        // Living entities only (players, villagers, fauna): dropped items, frames, paintings, boats and minecarts
        // are left alone, so a storm near a base never grinds down its outdoor decorations.
        for (Entity e : level.getEntities(bolt, box, EmberStrike::strikeable)) {
            e.thunderHit(level, bolt);
        }
    }

    /** Whether an ember strike hits this entity: a living one (player, villager, fauna), never an item or decoration. */
    static boolean strikeable(Entity entity) {
        return entity instanceof LivingEntity && entity.isAlive();
    }
}
