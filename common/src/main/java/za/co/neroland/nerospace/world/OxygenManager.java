package za.co.neroland.nerospace.world;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.platform.GasLookup;
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
 * per-loader server-tick hook and keeps the self-contained survival core. The diffusion field, terraform
 * breathability, and per-planet hazard shields (heat/cold) are now wired in; advancement criteria and the
 * gas-tank airlock refill remain deferred. Values are inlined (the config seam is deferred).</p>
 *
 * <p><b>Hazards (SUIT_HAZARD_DESIGN.md).</b> Cindara runs HOT and Glacira runs COLD: an uncountered
 * hazard multiplies oxygen drain ×{@link #HAZARD_DRAIN_MULTIPLIER} (no separate damage path — lethality
 * stays with zero-O₂ suffocation). A full set of the matching {@link HazardShield} suit variant negates
 * it; a mixed set does not.</p>
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
    /** Drain factor on a hazard dimension without the matching suit variant (SUIT_HAZARD_DESIGN.md §2). */
    private static final int HAZARD_DRAIN_MULTIPLIER = 4;

    /** Airlock refill: scan radius around a suited player for a Gas Tank / Oxygen Generator holding O2. */
    private static final int AIRLOCK_RADIUS = 4;
    /** Air units a suit's tank regains per check from an in-range oxygen store (drain offsets it nicely). */
    private static final int AIRLOCK_REFILL_PER_CHECK = 90;
    /** Gas (mB) consumed per air unit restored — the conversion rate from a tank's oxygen to suit air. */
    private static final int AIRLOCK_MB_PER_AIR = 2;

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
        int max = NerospaceConfig.scale(suited ? OXYGEN_SUIT_MAX : OXYGEN_MAX,
                NerospaceConfig.oxygenCapacityMultiplier());

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
                // An uncountered dimension hazard (Cindara heat / Glacira cold) multiplies the drain.
                int drain = NerospaceConfig.scale(suited ? SUIT_DRAIN_PER_CHECK : BARE_DRAIN_PER_CHECK,
                        NerospaceConfig.oxygenDrainMultiplier()) * hazardDrainMultiplier(level, player);
                oxygen = Math.max(0, oxygen - drain);
                hazardFeedback(level, player);
                // Airlock: a worn suit beside a Gas Tank / Oxygen Generator holding O2 taps it to top up —
                // a tank by the base door keeps you fuelled without a breathable bubble.
                if (suited && oxygen < max) {
                    oxygen = Math.min(max, oxygen + airlockRefill(level, player, max - oxygen));
                }
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
    private static boolean isBreathable(ServerLevel level, @NonNull BlockPos center) {
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
            BlockPos checkPos = NerospaceCommon.requireNonNull(pos);
            if (level.getBlockState(checkPos).is(ModBlocks.ROCKET_LAUNCH_PAD.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Airlock refill: a worn suit within {@link #AIRLOCK_RADIUS} of a Gas Tank / Creative Gas Tank /
     * Oxygen Generator holding Oxygen draws gas from it to refill the suit's air tank — so a tank at the
     * base entrance acts as an airlock. {@link #AIRLOCK_MB_PER_AIR} mB of oxygen restores one air unit.
     *
     * @param need air units missing from the suit tank
     * @return air units actually restored (0 when no usable oxygen store is in range)
     */
    private static int airlockRefill(ServerLevel level, Player player, int need) {
        if (need <= 0) {
            return 0;
        }
        int want = Math.min(need, AIRLOCK_REFILL_PER_CHECK);
        BlockPos center = player.blockPosition();
        int restored = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-AIRLOCK_RADIUS, -AIRLOCK_RADIUS, -AIRLOCK_RADIUS),
                center.offset(AIRLOCK_RADIUS, AIRLOCK_RADIUS, AIRLOCK_RADIUS))) {
            if (want <= 0) {
                break;
            }
            BlockPos checkPos = NerospaceCommon.requireNonNull(pos);
            BlockState state = level.getBlockState(checkPos);
            if (!state.is(ModBlocks.GAS_TANK.get())
                    && !state.is(ModBlocks.CREATIVE_GAS_TANK.get())
                    && !state.is(ModBlocks.OXYGEN_GENERATOR.get())) {
                continue;
            }
            NerospaceGasStorage store = GasLookup.INSTANCE.find(level, checkPos.immutable(), null);
            if (store == null) {
                continue;
            }
            int gained = drawOxygen(store, want);
            restored += gained;
            want -= gained;
        }
        if (restored > 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 0.3F, 1.4F);
        }
        return restored;
    }

    /** Extract whole air units of Oxygen from {@code store}: simulate, floor to whole units, then commit. */
    private static int drawOxygen(NerospaceGasStorage store, int wantAir) {
        if (store.getGas() != GasResource.OXYGEN || store.getAmount() <= 0) {
            return 0;
        }
        // The mB-of-oxygen cost per restored air unit is a consumable running cost: scale by fuelCost.
        int mbPerAir = NerospaceConfig.scale(AIRLOCK_MB_PER_AIR, NerospaceConfig.fuelCostMultiplier());
        long peeked = store.drain((long) wantAir * mbPerAir, true);
        int units = (int) (peeked / mbPerAir);
        if (units <= 0) {
            return 0;
        }
        store.drain((long) units * mbPerAir, false);
        return units;
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

    // --- Hazard shields (SUIT_HAZARD_DESIGN.md) -----------------------------

    /** Per-planet environmental hazard a suit variant must counter (NONE elsewhere). */
    public enum HazardShield {
        NONE, HEAT, COLD
    }

    /** Oxygen-suit capacity tier worn (orthogonal to the heat/cold {@link HazardShield} variant). */
    public enum SuitTier {
        NONE, TIER_1, TIER_2
    }

    /**
     * The worn suit's capacity tier for the HUD badge: a full Tier-2 set reads TIER_2, any other full
     * Oxygen Suit set (base / heat / cold) reads TIER_1, otherwise NONE. Client-safe (armour slots only).
     */
    public static SuitTier suitTier(Player player) {
        if (isFullSet(player, ModItems.OXYGEN_SUIT_T2_HELMET.get(), ModItems.OXYGEN_SUIT_T2_CHESTPLATE.get(),
                ModItems.OXYGEN_SUIT_T2_LEGGINGS.get(), ModItems.OXYGEN_SUIT_T2_BOOTS.get())) {
            return SuitTier.TIER_2;
        }
        return isFullSuit(player) ? SuitTier.TIER_1 : SuitTier.NONE;
    }

    /** Whether all four armour slots hold exactly the given pieces. */
    private static boolean isFullSet(Player player, Item head, Item chest, Item legs, Item feet) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(head)
                && player.getItemBySlot(EquipmentSlot.CHEST).is(chest)
                && player.getItemBySlot(EquipmentSlot.LEGS).is(legs)
                && player.getItemBySlot(EquipmentSlot.FEET).is(feet);
    }

    /** The hazard a dimension carries: Cindara runs hot, Glacira runs cold. (Public for the HUD.) */
    public static HazardShield hazardFor(@NonNull ResourceKey<Level> dimension) {
        if (ModDimensions.CINDARA_LEVEL.equals(dimension)) {
            return HazardShield.HEAT;
        }
        if (ModDimensions.GLACIRA_LEVEL.equals(dimension)) {
            return HazardShield.COLD;
        }
        return HazardShield.NONE;
    }

    /** Drain factor for the player: ×{@link #HAZARD_DRAIN_MULTIPLIER} on an uncountered hazard, else 1. */
    private static int hazardDrainMultiplier(ServerLevel level, Player player) {
        HazardShield hazard = hazardFor(level.dimension());
        if (hazard == HazardShield.NONE || hazardShield(player) == hazard) {
            return 1;
        }
        return HAZARD_DRAIN_MULTIPLIER;
    }

    /**
     * The worn hazard shield — requires ALL FOUR pieces of the SAME variant (a heat helmet on a cryo
     * suit grants the suit's air tank but no shield), orthogonal to the base oxygen-suit set.
     */
    public static HazardShield hazardShield(Player player) {
        HazardShield head = pieceVariant(player.getItemBySlot(EquipmentSlot.HEAD));
        if (head == HazardShield.NONE) {
            return HazardShield.NONE;
        }
        if (pieceVariant(player.getItemBySlot(EquipmentSlot.CHEST)) == head
                && pieceVariant(player.getItemBySlot(EquipmentSlot.LEGS)) == head
                && pieceVariant(player.getItemBySlot(EquipmentSlot.FEET)) == head) {
            return head;
        }
        return HazardShield.NONE;
    }

    /** Which hazard variant a single worn piece belongs to (NONE for plain/T2/non-suit items). */
    private static HazardShield pieceVariant(ItemStack worn) {
        if (worn.is(ModItems.OXYGEN_SUIT_HEAT_HELMET.get())
                || worn.is(ModItems.OXYGEN_SUIT_HEAT_CHESTPLATE.get())
                || worn.is(ModItems.OXYGEN_SUIT_HEAT_LEGGINGS.get())
                || worn.is(ModItems.OXYGEN_SUIT_HEAT_BOOTS.get())) {
            return HazardShield.HEAT;
        }
        if (worn.is(ModItems.OXYGEN_SUIT_COLD_HELMET.get())
                || worn.is(ModItems.OXYGEN_SUIT_COLD_CHESTPLATE.get())
                || worn.is(ModItems.OXYGEN_SUIT_COLD_LEGGINGS.get())
                || worn.is(ModItems.OXYGEN_SUIT_COLD_BOOTS.get())) {
            return HazardShield.COLD;
        }
        return HazardShield.NONE;
    }

    /**
     * Thematic feedback for an exposed, unprotected player (no extra damage — the O₂ bar is the cost):
     * a building frost vignette on the cold world (capped below fully-frozen so vanilla freeze damage
     * never double-dips), sparse smoke shimmer on the hot one.
     */
    private static void hazardFeedback(ServerLevel level, Player player) {
        HazardShield hazard = hazardFor(level.dimension());
        if (hazard == HazardShield.NONE || hazardShield(player) == hazard) {
            return;
        }
        if (hazard == HazardShield.COLD) {
            int cap = player.getTicksRequiredToFreeze() - 2; // never "fully frozen" => no freeze damage
            player.setTicksFrozen(Math.min(cap, player.getTicksFrozen() + CHECK_INTERVAL_TICKS * 2 + 15));
        } else if (player.tickCount % (CHECK_INTERVAL_TICKS * 4) == 0) {
            level.sendParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 1.2D, player.getZ(), 3, 0.25D, 0.4D, 0.25D, 0.01D);
        }
    }
}
