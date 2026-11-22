package za.co.neroland.nerospace.suit;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerolandcore.progression.CoreGates;
import za.co.neroland.nerolandcore.progression.ProgressionGates;
import za.co.neroland.nerospace.api.NerospaceSuitPolicy;
import za.co.neroland.nerospace.api.TemperatureBand;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.suit.SuitFlightMath.Input;
import za.co.neroland.nerospace.suit.SuitFlightMath.Mode;
import za.co.neroland.nerospace.world.gravity.GravityManager;
import za.co.neroland.nerospace.world.planet.Planets;

/** Server-authoritative flight. The client only sends an input bitmask. */
public final class SuitFlight {

    /** In-memory flight state. Weakly keyed, gone when the player object is. No identifier is stored. */
    public static final class Live {
        public int energy;
        public int rcs;
        public int heat;
        public int burst;
        public int combat;
        public Mode mode = Mode.GROUNDED;
        public int flags;
        public boolean seeded;
    }

    private static final Map<ServerPlayer, Live> LIVE = new WeakHashMap<>();

    private SuitFlight() {
    }

    public static Live live(ServerPlayer player) {
        return LIVE.get(player);
    }

    public static void input(ServerPlayer player, int flags) {
        Live live = LIVE.computeIfAbsent(player, key -> new Live());
        live.flags = flags;
    }

    public static void tick(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof SuitPieceItem piece) || piece.piece() != SuitPiece.CHEST) {
            MovementAllowance.clear(player);
            LIVE.remove(player);
            return;
        }
        Live live = LIVE.computeIfAbsent(player, key -> new Live());
        if (!live.seeded) {
            SuitCoreState core = SuitStacks.core(chest);
            live.energy = core.energy();
            live.rcs = core.rcs();
            live.heat = core.heat();
            live.seeded = true;
        }
        SuitLoadout loadout = SuitStacks.loadout(chest);
        SuitProtection.Reading worn = SuitView.worn(player);
        boolean full = worn.suit() != za.co.neroland.nerospace.world.planet.HazardOrderRules.SuitClass.NONE;
        boolean breached = SuitView.slot(chest).breached();
        int capacity = SuitEnergyMath.capacity(loadout.frameEnum(), loadout.moduleCount(SuitIds.POWER_CELL),
                NerospaceConfig.suitEnergyCapacityMultiplier());
        boolean harness = full && !breached && loadout.hasModule(SuitIds.FLIGHT_HARNESS)
                && worn.effective() == SuitFrame.MK4
                && ProgressionGates.isOpen(player, CoreGates.DEEP_SPACE);
        boolean jet = full && !breached && !harness && loadout.hasModule(SuitIds.JUMP_JET)
                && worn.effective() != null && worn.effective().ordinal() >= SuitFrame.MK3.ordinal();
        ServerLevel level = player.level();
        double gravity = GravityManager.factorAt(level, player.blockPosition());
        TemperatureBand band = Planets.get(level.dimension()) == null ? TemperatureBand.TEMPERATE
                : Planets.get(level.dimension()).traits().temperatureBand();
        if (player.getLastHurtByMob() instanceof Player && player.hurtTime > 0) {
            live.combat = NerospaceConfig.suitFlightCombatLockTicks();
        } else if (live.combat > 0) {
            live.combat--;
        }
        boolean jump = (live.flags & 1) != 0;
        boolean sneak = (live.flags & 2) != 0;
        boolean sprint = (live.flags & 4) != 0;
        boolean brake = (live.flags & 8) != 0;
        boolean engage = (live.flags & 16) != 0;
        boolean allowed = flightAllowed(player) && (harness || jet);
        Input input = new Input(jump, sneak, sprint, brake, engage && harness, !player.onGround(), live.combat > 0,
                allowed);
        boolean reserve = SuitEnergyMath.aboveReserve(live.energy, capacity, NerospaceConfig.suitFlightReservePercent());
        var step = SuitFlightMath.step(input, live.mode, player.getLookAngle().x, player.getLookAngle().z, gravity,
                live.heat, live.burst, NerospaceConfig.suitHoverMaxBurstTicks(), NerospaceConfig.suitFlightMaxSpeed(),
                reserve, band);
        live.mode = step.mode();
        live.heat = step.heat();
        if (step.mode() == Mode.HOVER && step.vy() > 0) {
            live.burst++;
        } else if (step.mode() != Mode.HOVER) {
            live.burst = 0;
        }
        int cost = 0;
        if (step.mode() == Mode.HOVER && step.vy() > 0) {
            cost = SuitEnergyMath.flightCost(SuitEnergyMath.HOVER_PER_TICK, gravity, NerospaceConfig.suitEnergyUseMultiplier());
        } else if (step.mode() == Mode.CRUISE || step.mode() == Mode.BOOST) {
            int base = step.mode() == Mode.BOOST ? SuitEnergyMath.BOOST_PER_TICK : SuitEnergyMath.CRUISE_PER_TICK;
            cost = SuitEnergyMath.flightCost(base, gravity, NerospaceConfig.suitEnergyUseMultiplier());
        }
        if (cost > 0 && live.energy > SuitEnergyMath.reserve(capacity, NerospaceConfig.suitFlightReservePercent())) {
            live.energy = Math.max(0, live.energy - cost);
        }
        if (step.mode() == Mode.CRUISE || step.mode() == Mode.BOOST || step.mode() == Mode.HOVER) {
            player.setDeltaMovement(step.vx(), Math.max(step.vy(), player.getDeltaMovement().y * 0.9), step.vz());
            player.fallDistance = 0;
            MovementAllowance.activate(player, NerospaceConfig.suitFlightMaxSpeed() * 1.6D, false);
        } else if (step.mode() == Mode.FAILSAFE) {
            player.setDeltaMovement(0, step.vy(), 0);
            player.fallDistance = 0;
            MovementAllowance.activate(player, 0.2D, false);
        } else {
            MovementAllowance.clear(player);
        }
        passive(player, chest, loadout, live, full && !breached);
    }

    private static void passive(ServerPlayer player, ItemStack chest, SuitLoadout loadout, Live live, boolean active) {
        if (!active) {
            return;
        }
        double use = NerospaceConfig.suitEnergyUseMultiplier();
        if (loadout.hasModule(SuitIds.SOLAR_SKIN) && player.level().canSeeSky(player.blockPosition())) {
            live.energy = Math.min(Integer.MAX_VALUE, live.energy + NerospaceConfig.suitSolarSkinRate());
        }
        int capacity = SuitEnergyMath.capacity(loadout.frameEnum(), loadout.moduleCount(SuitIds.POWER_CELL),
                NerospaceConfig.suitEnergyCapacityMultiplier());
        live.energy = Math.min(live.energy, capacity);
        if (loadout.hasModule(SuitIds.NIGHT_VISOR) && loadout.toggleOn(index(loadout, SuitIds.NIGHT_VISOR))) {
            live.energy = Math.max(0, live.energy - SuitEnergyMath.cost(SuitEnergyMath.NIGHT_VISOR_PER_TICK, use));
        }
        SuitStacks.setCore(chest, new SuitCoreState(live.energy, live.rcs, live.heat));
    }

    private static int index(SuitLoadout loadout, String id) {
        return loadout.modules().indexOf(id);
    }

    private static boolean flightAllowed(ServerPlayer player) {
        if (!NerospaceConfig.suitFlightEnabled()) {
            return false;
        }
        String id = player.level().dimension().identifier().toString();
        String mode = NerospaceConfig.suitFlightDimensionMode();
        if ("none".equals(mode)) {
            return false;
        }
        if ("nerospace_only".equals(mode) && !id.startsWith("nerospace:")) {
            return false;
        }
        String deny = NerospaceConfig.suitFlightDimensionDenylist();
        if (deny != null && !deny.isBlank()) {
            for (String part : deny.split(",")) {
                if (id.equals(part.trim())) {
                    return false;
                }
            }
        }
        Vec3 pos = player.position();
        if (NerospaceSuitPolicy.vetoed(player.level(), pos, player.getType())) {
            return false;
        }
        return !StationFlight.denied(player);
    }
}
