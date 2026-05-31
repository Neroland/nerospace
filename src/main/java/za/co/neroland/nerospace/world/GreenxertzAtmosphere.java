package za.co.neroland.nerospace.world;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModDimensions;

/**
 * The thin Greenxertz atmosphere (Phase 5). While on the planet, a survival/adventure player slowly
 * suffocates unless they are standing near a {@link za.co.neroland.nerospace.rocket.RocketLaunchPadBlock}
 * — their landing site is treated as a safe, pressurised zone. This is a deliberately simple placeholder
 * for a future oxygen-suit / space-station system; the launch pad is the protection hook for now.
 */
@EventBusSubscriber(modid = Nerospace.MODID)
public final class GreenxertzAtmosphere {

    private static final int DAMAGE_INTERVAL_TICKS = 40; // every 2 seconds

    /** Every Nerospace planet (and the vacuum of the station) has a hostile atmosphere. */
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
        if (!PLANETS.contains(level.dimension())) {
            return;
        }
        if (!Config.ATMOSPHERE_DAMAGE_ENABLED.get()) {
            return;
        }
        if (player.getAbilities().instabuild || player.isSpectator()) {
            return;
        }
        if (player.tickCount % DAMAGE_INTERVAL_TICKS != 0) {
            return;
        }
        if (isPressurised(level, player.blockPosition())) {
            return;
        }

        player.hurtServer(level, level.damageSources().generic(), Config.ATMOSPHERE_DAMAGE.get().floatValue());
        if (player.tickCount % (DAMAGE_INTERVAL_TICKS * 3) == 0) {
            player.sendSystemMessage(Component.translatable("message.nerospace.greenxertz.no_air"));
        }
    }

    /** @return true if a launch pad sits within the configured safe radius (a pressurised zone). */
    private static boolean isPressurised(Level level, BlockPos center) {
        int radius = Config.ATMOSPHERE_SAFE_RADIUS.get();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (level.getBlockState(pos).is(ModBlocks.ROCKET_LAUNCH_PAD.get())) {
                return true;
            }
        }
        return false;
    }
}
