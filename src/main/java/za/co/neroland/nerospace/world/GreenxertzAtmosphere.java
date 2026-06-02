package za.co.neroland.nerospace.world;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.registry.ModAttachments;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.registry.ModItems;
// OxygenFieldManager is in this same package (za.co.neroland.nerospace.world).

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

        // The check is throttled; oxygen still mirrors to the HUD every tick from the stored value.
        if (player.tickCount % CHECK_INTERVAL_TICKS == 0) {
            if (isBreathable(level, player)) {
                // A breathable zone (pad radius, oxygen field, or terraformed ground) refills.
                oxygen = max;
            } else if (isWearingFullSuit(player)) {
                // The Oxygen Suit's finite air tank drains slowly while exposed.
                oxygen = Math.max(0, oxygen - Config.OXYGEN_SUIT_DRAIN.get());
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

    /**
     * Breathability (terraform design §1.6): an O(1) oxygen-field lookup at the player's head, plus the
     * permanent terraformed-ground flag (§3.4) and the launch-pad safe zone. This replaces the old
     * per-tick scan + flood-fill — cheaper than before and correct about dissipation and leaks.
     */
    private static boolean isBreathable(ServerLevel level, Player player) {
        BlockPos eye = player.blockPosition().above(); // ~head height
        if (OxygenFieldManager.get(level).isBreathable(eye)
                || OxygenFieldManager.get(level).isBreathable(player.blockPosition())) {
            return true;
        }
        if (terraformedBreathable(level, player)) {
            return true;
        }
        return nearLaunchPad(level, player.blockPosition());
    }

    /** Terraformed chunks are permanently breathable at/above the surface (not in a buried vault). */
    private static boolean terraformedBreathable(ServerLevel level, Player player) {
        BlockPos pos = player.blockPosition();
        if (!Boolean.TRUE.equals(level.getChunkAt(pos).getData(ModAttachments.TERRAFORMED))) {
            return false;
        }
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return pos.getY() >= surfaceY - 2;
    }

    /** Cheap landing-site safety: within {@code atmosphereSafeRadius} of a Rocket Launch Pad. */
    private static boolean nearLaunchPad(Level level, BlockPos center) {
        int safeR = Config.ATMOSPHERE_SAFE_RADIUS.get();
        if (safeR <= 0) {
            return false;
        }
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-safeR, -safeR, -safeR),
                center.offset(safeR, safeR, safeR))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.ROCKET_LAUNCH_PAD.get()) && chebyshev(center, pos) <= safeR) {
                return true;
            }
        }
        return false;
    }

    /** @return true if all four Oxygen Suit pieces are worn (personal life support). */
    private static boolean isWearingFullSuit(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.OXYGEN_SUIT_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.OXYGEN_SUIT_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.OXYGEN_SUIT_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.OXYGEN_SUIT_BOOTS.get());
    }

    private static int chebyshev(BlockPos a, BlockPos b) {
        return Math.max(Math.abs(a.getX() - b.getX()),
                Math.max(Math.abs(a.getY() - b.getY()), Math.abs(a.getZ() - b.getZ())));
    }
}
