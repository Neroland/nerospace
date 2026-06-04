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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.gas.GasCapability;
import za.co.neroland.nerospace.gas.GasResource;
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

        SuitTier suit = suitTier(player);
        int max = suit.capacity();

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
            } else if (suit != SuitTier.NONE) {
                // Airlock refill: a worn suit taps a nearby Gas Tank / Oxygen Generator holding O2.
                int refilled = airlockRefill(level, player, suit, max - oxygen);
                if (refilled > 0) {
                    oxygen += refilled;
                } else {
                    // The Oxygen Suit's finite air tank drains slowly while exposed.
                    oxygen = Math.max(0, oxygen - Config.OXYGEN_SUIT_DRAIN.get());
                }
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

    /**
     * The worn Oxygen Suit tier. A full set of any mix of suit pieces is life support; only a full
     * Tier 2 set earns the Tier 2 tank and refill speed (a mixed set counts as Tier 1).
     */
    public static SuitTier suitTier(Player player) {
        int h = pieceTier(player.getItemBySlot(EquipmentSlot.HEAD),
                ModItems.OXYGEN_SUIT_HELMET.get(), ModItems.OXYGEN_SUIT_T2_HELMET.get());
        int c = pieceTier(player.getItemBySlot(EquipmentSlot.CHEST),
                ModItems.OXYGEN_SUIT_CHESTPLATE.get(), ModItems.OXYGEN_SUIT_T2_CHESTPLATE.get());
        int l = pieceTier(player.getItemBySlot(EquipmentSlot.LEGS),
                ModItems.OXYGEN_SUIT_LEGGINGS.get(), ModItems.OXYGEN_SUIT_T2_LEGGINGS.get());
        int b = pieceTier(player.getItemBySlot(EquipmentSlot.FEET),
                ModItems.OXYGEN_SUIT_BOOTS.get(), ModItems.OXYGEN_SUIT_T2_BOOTS.get());
        if (h == 0 || c == 0 || l == 0 || b == 0) {
            return SuitTier.NONE;
        }
        return (h == 2 && c == 2 && l == 2 && b == 2) ? SuitTier.TIER_2 : SuitTier.TIER_1;
    }

    /** 2 = Tier 2 piece, 1 = Tier 1 piece, 0 = not a suit piece. */
    private static int pieceTier(net.minecraft.world.item.ItemStack worn,
            net.minecraft.world.item.Item t1, net.minecraft.world.item.Item t2) {
        if (worn.is(t2)) {
            return 2;
        }
        return worn.is(t1) ? 1 : 0;
    }

    /**
     * Airlock refill (suit-and-station integration): a worn suit within
     * {@code oxygenAirlockRadius} of a Gas Tank or Oxygen Generator holding Oxygen draws gas from it
     * to refill the suit's air tank — so a tank at the base entrance acts as an airlock. Each air
     * unit costs {@code oxygenAirlockMbPerAir} mB; a Tier 2 suit refills at double rate.
     *
     * <p>Public for the gametests, which exercise it directly (the event path only runs on airless
     * dimensions).</p>
     *
     * @param need air units missing from the suit tank
     * @return air units actually restored (0 when no usable store is in range)
     */
    public static int airlockRefill(ServerLevel level, Player player, SuitTier suit, int need) {
        int radius = Config.OXYGEN_AIRLOCK_RADIUS.get();
        if (radius <= 0 || need <= 0) {
            return 0;
        }
        int rate = Config.OXYGEN_AIRLOCK_REFILL_PER_CHECK.get() * (suit == SuitTier.TIER_2 ? 2 : 1);
        int want = Math.min(need, rate);
        int mbPerAir = Config.OXYGEN_AIRLOCK_MB_PER_AIR.get();

        BlockPos center = player.blockPosition();
        int restored = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (want <= 0) {
                break;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.GAS_TANK.get())
                    && !state.is(ModBlocks.CREATIVE_GAS_TANK.get())
                    && !state.is(ModBlocks.OXYGEN_GENERATOR.get())) {
                continue;
            }
            ResourceHandler<GasResource> handler =
                    GasCapability.BLOCK.getCapability(level, pos.immutable(), state, null, null);
            if (handler == null) {
                continue;
            }
            int gained = drawOxygen(handler, want, mbPerAir);
            restored += gained;
            want -= gained;
        }
        if (restored > 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.3F, 1.4F);
        }
        return restored;
    }

    /** Extracts whole air units of Oxygen from {@code handler}: peek, floor to units, then commit. */
    private static int drawOxygen(ResourceHandler<GasResource> handler, int wantAir, int mbPerAir) {
        if (mbPerAir <= 0) {
            // Free air (config): the store just has to hold any oxygen.
            try (Transaction tx = Transaction.openRoot()) {
                ResourceStack<GasResource> peeked = ResourceHandlerUtil.extractFirst(
                        handler, r -> r == GasResource.OXYGEN, 1, tx);
                return peeked == null ? 0 : wantAir;
            }
        }
        int available;
        try (Transaction tx = Transaction.openRoot()) {
            ResourceStack<GasResource> peeked = ResourceHandlerUtil.extractFirst(
                    handler, r -> r == GasResource.OXYGEN, wantAir * mbPerAir, tx);
            available = peeked == null ? 0 : peeked.amount();
            // Aborted (no commit): this was only a peek.
        }
        int air = available / mbPerAir;
        if (air <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            ResourceHandlerUtil.extractFirst(handler, r -> r == GasResource.OXYGEN, air * mbPerAir, tx);
            tx.commit();
        }
        return air;
    }

    /** The worn-suit tiers and their air-tank capacities. */
    public enum SuitTier {
        NONE,
        TIER_1,
        TIER_2;

        /** The air capacity this tier grants (bare lungs share the Tier 1 scale). */
        public int capacity() {
            return this == TIER_2 ? Config.OXYGEN_SUIT_T2_MAX.get() : Config.OXYGEN_MAX.get();
        }
    }

    private static int chebyshev(BlockPos a, BlockPos b) {
        return Math.max(Math.abs(a.getX() - b.getX()),
                Math.max(Math.abs(a.getY() - b.getY()), Math.abs(a.getZ() - b.getZ())));
    }
}
