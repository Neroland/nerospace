package za.co.neroland.nerospace.world.planet.greenxertz;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import za.co.neroland.nerospace.block.rules.HealBedRules;

/**
 * Stripped Bio-Bark heal-on-wake: a player who slept long enough to skip the night and wakes within 4 blocks
 * of at least one Stripped Bio-Bark Log heals 4 HP and gets Regeneration I for 10 s, once per sleep. Driven from the
 * shared server tick ({@link #tick}); it only remembers, per online player and in memory, whether they are mid-sleep
 * (never persisted, cleared on wake or logout).
 */
public final class BioBarkHeal {

    /** Players currently in a completed sleep (weak keys: a logged-out player drops out). */
    private static final Map<ServerPlayer, Boolean> SLEPT = Collections.synchronizedMap(new WeakHashMap<>());

    private BioBarkHeal() {
    }

    /** Pure: a wake completes a sleep when the player had slept long enough last tick and is no longer asleep. */
    public static boolean wokeFromFullSleep(boolean sleptLongEnoughBefore, boolean sleepingNow) {
        return sleptLongEnoughBefore && !sleepingNow;
    }

    /** Once per server tick. Cheap: one boolean per sleeping player; the 9x9x9 scan only runs on a wake. */
    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                boolean slept = Boolean.TRUE.equals(SLEPT.get(player));
                boolean sleeping = player.isSleeping();
                if (sleeping) {
                    if (player.isSleepingLongEnough()) {
                        SLEPT.put(player, Boolean.TRUE);
                    }
                    continue;
                }
                if (wokeFromFullSleep(slept, false)) {
                    SLEPT.remove(player);
                    heal(level, player);
                }
            }
        }
    }

    private static void heal(ServerLevel level, ServerPlayer player) {
        float hp = HealBedRules.healOnWake(countStripped(level, player.blockPosition()));
        if (hp <= 0.0F) {
            return;
        }
        player.heal(hp);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, HealBedRules.REGEN_TICKS, 0));
    }

    /** Stripped Bio-Bark Logs within {@link HealBedRules#RADIUS} (a cube), stopping at the first one found. */
    static int countStripped(ServerLevel level, BlockPos centre) {
        int r = HealBedRules.RADIUS;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    m.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    if (level.isLoaded(m)
                            && level.getBlockState(m).is(GreenxertzContent.STRIPPED_BIO_BARK_LOG.get())) {
                        return 1;
                    }
                }
            }
        }
        return 0;
    }
}
