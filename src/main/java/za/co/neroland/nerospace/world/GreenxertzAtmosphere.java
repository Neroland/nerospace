package za.co.neroland.nerospace.world;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.machine.OxygenGeneratorBlockEntity;
import za.co.neroland.nerospace.registry.ModAttachments;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModDimensions;

/**
 * Oxygen / atmosphere handling (Phase 8c). On airless Nerospace dimensions a survival/adventure
 * player carries a finite {@link ModAttachments#OXYGEN} supply that drains while exposed and refills
 * inside a breathable zone — near a {@link za.co.neroland.nerospace.rocket.RocketLaunchPadBlock}
 * (the landing site) or an active {@link OxygenGeneratorBlockEntity}. When oxygen hits zero the
 * player suffocates. Remaining oxygen is mirrored onto the vanilla air-supply bar so the bubble HUD
 * shows it for free (a bespoke oxygen gauge is a later polish pass). See {@code OXYGEN_SPEC.md}.
 */
@EventBusSubscriber(modid = Nerospace.MODID)
public final class GreenxertzAtmosphere {

    private static final int DAMAGE_INTERVAL_TICKS = 40; // suffocation tick, every 2 s
    /** How often the (relatively costly) breathable-zone scan + oxygen update runs. */
    private static final int CHECK_INTERVAL_TICKS = 10;

    /** Every Nerospace planet (and the vacuum of the station) is airless. */
    private static final Set<ResourceKey<Level>> PLANETS = Set.of(
            ModDimensions.GREENXERTZ_LEVEL, ModDimensions.CINDARA_LEVEL, ModDimensions.STATION_LEVEL);

    private GreenxertzAtmosphere() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        // Server-authoritative only; bind a ServerLevel so we can use the non-deprecated hurtServer.
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        int max = Config.OXYGEN_MAX.get();

        // Off-world only; the overworld (and any non-listed dimension) stays breathable and tops up.
        boolean airless = PLANETS.contains(level.dimension())
                && Config.ATMOSPHERE_DAMAGE_ENABLED.get()
                && !player.getAbilities().instabuild
                && !player.isSpectator();
        if (!airless) {
            setOxygen(player, max);
            return;
        }

        int oxygen = Math.min(getOxygen(player, max), max);

        // The scan is throttled; oxygen still mirrors to the HUD every tick from the stored value.
        if (player.tickCount % CHECK_INTERVAL_TICKS == 0) {
            if (isBreathable(level, player.blockPosition())) {
                oxygen = max;
            } else {
                oxygen = Math.max(0, oxygen - Config.OXYGEN_DRAIN_PER_TICK.get() * CHECK_INTERVAL_TICKS);
            }
            setOxygen(player, oxygen);
        }

        mirrorToAirSupply(player, oxygen, max);

        if (oxygen <= 0 && player.tickCount % DAMAGE_INTERVAL_TICKS == 0) {
            player.hurtServer(level, level.damageSources().generic(), Config.ATMOSPHERE_DAMAGE.get().floatValue());
            if (player.tickCount % (DAMAGE_INTERVAL_TICKS * 3) == 0) {
                player.sendSystemMessage(Component.translatable("message.nerospace.greenxertz.no_air"));
            }
        }
    }

    private static int getOxygen(Player player, int fallback) {
        Integer stored = player.getData(ModAttachments.OXYGEN);
        return stored == null ? fallback : stored;
    }

    private static void setOxygen(Player player, int value) {
        if (getOxygen(player, value) != value) {
            player.setData(ModAttachments.OXYGEN, value);
        }
    }

    /** Maps oxygen onto the vanilla air-supply bar (full oxygen → no bubbles shown). */
    private static void mirrorToAirSupply(Player player, int oxygen, int max) {
        int airMax = player.getMaxAirSupply();
        int air = (int) ((long) oxygen * airMax / Math.max(1, max));
        player.setAirSupply(Math.min(airMax, Math.max(0, air)));
    }

    /** @return true if a launch pad or an active Oxygen Generator pressurises {@code center}. */
    private static boolean isBreathable(Level level, BlockPos center) {
        int safeR = Config.ATMOSPHERE_SAFE_RADIUS.get();
        int bubbleR = Config.OXYGEN_BUBBLE_RADIUS.get();
        int scan = Math.max(safeR, bubbleR);
        int bubbleSq = bubbleR * bubbleR;

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-scan, -scan, -scan),
                center.offset(scan, scan, scan))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.ROCKET_LAUNCH_PAD.get())) {
                if (chebyshev(center, pos) <= safeR) {
                    return true;
                }
            } else if (state.is(ModBlocks.OXYGEN_GENERATOR.get())) {
                if (center.distSqr(pos) <= bubbleSq
                        && level.getBlockEntity(pos) instanceof OxygenGeneratorBlockEntity gen
                        && gen.isActive()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int chebyshev(BlockPos a, BlockPos b) {
        return Math.max(Math.abs(a.getX() - b.getX()),
                Math.max(Math.abs(a.getY() - b.getY()), Math.abs(a.getZ() - b.getZ())));
    }
}
