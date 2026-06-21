package za.co.neroland.nerospace.world;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.platform.Services;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Oxygen / atmosphere survival (Phase 8c, simplified cross-loader port). On airless Nerospace
 * dimensions a survival player carries a finite oxygen supply (a per-player data attachment, accessed
 * through the {@link Services#PLATFORM} seam) that drains while exposed and refills inside a breathable
 * zone — near a Rocket Launch Pad (the landing site) or an Oxygen Generator. A worn Oxygen Suit grants a
 * larger tank and far slower drain. At zero oxygen the player suffocates. Remaining oxygen is mirrored
 * onto the vanilla air-supply bar so the bubble HUD shows it for free.
 *
 * <p><b>Cross-loader port note.</b> The root drives this from a NeoForge {@code PlayerTickEvent} and a
 * full diffusion {@code OxygenFieldManager} (sealed rooms + client overlay, networking-synced), plus
 * terraform breathability, hazard shields, and gas-tank airlock refills. The multiloader ticks it from a
 * per-loader server-tick hook and keeps the self-contained survival core; the diffusion field, terraform,
 * hazard variants, advancement criteria, and gas-airlock refill are deferred to their own batches. Values
 * are inlined (the config seam is deferred).</p>
 */
public final class OxygenManager {

    /** Default / bare-lungs oxygen capacity (also the attachment default — keep both loaders in sync). */
    public static final int OXYGEN_MAX = 300;
    /** A full Oxygen Suit's larger air tank. */
    public static final int OXYGEN_SUIT_MAX = 900;

    private static final int CHECK_INTERVAL_TICKS = 10;
    private static final int DAMAGE_INTERVAL_TICKS = 40;
    /** Bare-lungs drain per check (exposed) — ~a few seconds of air. */
    private static final int BARE_DRAIN_PER_CHECK = 30;
    /** Suited drain per check (the suit's tank lasts far longer). */
    private static final int SUIT_DRAIN_PER_CHECK = 3;
    /** Breathable-zone scan radius around the player (launch pad / oxygen generator). */
    private static final int SAFE_RADIUS = 6;
    private static final float SUFFOCATION_DAMAGE = 1.0F;

    /** Every Nerospace planet (and the vacuum of the station) is airless. */
    private static final Set<ResourceKey<Level>> PLANETS = Set.of(
            ModDimensions.GREENXERTZ_LEVEL, ModDimensions.CINDARA_LEVEL,
            ModDimensions.STATION_LEVEL, ModDimensions.GLACIRA_LEVEL);

    private OxygenManager() {
    }

    /** Per-player server tick (called from each loader's server-tick hook). */
    public static void tick(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        boolean suited = isFullSuit(player);
        int max = suited ? OXYGEN_SUIT_MAX : OXYGEN_MAX;

        boolean airless = PLANETS.contains(level.dimension())
                && !player.getAbilities().instabuild
                && !player.isSpectator();
        if (!airless) {
            Services.PLATFORM.setOxygen(player, max);
            mirrorToAirSupply(player, max, max);
            return;
        }

        int oxygen = Math.min(Services.PLATFORM.getOxygen(player), max);

        if (player.tickCount % CHECK_INTERVAL_TICKS == 0) {
            if (isBreathable(level, player.blockPosition())) {
                oxygen = max;
            } else {
                oxygen = Math.max(0, oxygen - (suited ? SUIT_DRAIN_PER_CHECK : BARE_DRAIN_PER_CHECK));
            }
            Services.PLATFORM.setOxygen(player, oxygen);
        }

        mirrorToAirSupply(player, oxygen, max);

        if (oxygen <= 0 && player.tickCount % DAMAGE_INTERVAL_TICKS == 0) {
            player.hurtServer(level, level.damageSources().generic(), SUFFOCATION_DAMAGE);
            if (player.tickCount % (DAMAGE_INTERVAL_TICKS * 3) == 0) {
                player.sendSystemMessage(Component.translatable("message.nerospace.greenxertz.no_air"));
            }
        }
    }

    /** Maps oxygen onto the vanilla air-supply bar (full oxygen → no bubbles shown). */
    private static void mirrorToAirSupply(Player player, int oxygen, int max) {
        int airMax = player.getMaxAirSupply();
        int air = (int) ((long) oxygen * airMax / Math.max(1, max));
        player.setAirSupply(Math.min(airMax, Math.max(0, air)));
    }

    /**
     * A breathable zone: the diffusion {@link OxygenFieldManager} reads breathable at {@code center}
     * (sealed rooms fill completely; an Oxygen Generator pressurises a bubble / its sealed room), or the
     * player is within {@link #SAFE_RADIUS} of a Rocket Launch Pad — a permanent pressurised safe zone at
     * the landing site (the pad is not a field source, so it stays a simple radius check).
     */
    private static boolean isBreathable(ServerLevel level, BlockPos center) {
        // Terraformed ground is permanently breathable (the Terraformer flags the chunk).
        if (Services.PLATFORM.isTerraformed(level.getChunkAt(center))) {
            return true;
        }
        if (OxygenFieldManager.get(level).isBreathable(center)) {
            return true;
        }
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-SAFE_RADIUS, -SAFE_RADIUS, -SAFE_RADIUS),
                center.offset(SAFE_RADIUS, SAFE_RADIUS, SAFE_RADIUS))) {
            if (level.getBlockState(pos).is(ModBlocks.ROCKET_LAUNCH_PAD.get())) {
                return true;
            }
        }
        return false;
    }

    /** Whether the player wears a full set of Oxygen Suit pieces (any tier / hazard variant counts). */
    private static boolean isFullSuit(Player player) {
        return isSuitPiece(player.getItemBySlot(EquipmentSlot.HEAD),
                        ModItems.OXYGEN_SUIT_HELMET.get(), ModItems.OXYGEN_SUIT_T2_HELMET.get(),
                        ModItems.OXYGEN_SUIT_HEAT_HELMET.get(), ModItems.OXYGEN_SUIT_COLD_HELMET.get())
                && isSuitPiece(player.getItemBySlot(EquipmentSlot.CHEST),
                        ModItems.OXYGEN_SUIT_CHESTPLATE.get(), ModItems.OXYGEN_SUIT_T2_CHESTPLATE.get(),
                        ModItems.OXYGEN_SUIT_HEAT_CHESTPLATE.get(), ModItems.OXYGEN_SUIT_COLD_CHESTPLATE.get())
                && isSuitPiece(player.getItemBySlot(EquipmentSlot.LEGS),
                        ModItems.OXYGEN_SUIT_LEGGINGS.get(), ModItems.OXYGEN_SUIT_T2_LEGGINGS.get(),
                        ModItems.OXYGEN_SUIT_HEAT_LEGGINGS.get(), ModItems.OXYGEN_SUIT_COLD_LEGGINGS.get())
                && isSuitPiece(player.getItemBySlot(EquipmentSlot.FEET),
                        ModItems.OXYGEN_SUIT_BOOTS.get(), ModItems.OXYGEN_SUIT_T2_BOOTS.get(),
                        ModItems.OXYGEN_SUIT_HEAT_BOOTS.get(), ModItems.OXYGEN_SUIT_COLD_BOOTS.get());
    }

    private static boolean isSuitPiece(ItemStack worn, Item... options) {
        for (Item option : options) {
            if (worn.is(option)) {
                return true;
            }
        }
        return false;
    }
}
