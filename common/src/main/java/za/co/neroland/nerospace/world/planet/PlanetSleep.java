package za.co.neroland.nerospace.world.planet;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.compat.mc.SleepCompat;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.world.planet.nyxara.worldgen.FixedXBands;
import za.co.neroland.nerospace.world.planet.nyxara.worldgen.NyxaraBand;

/**
 * Sleeping on planets (server glue for {@link PlanetSleepRules}). Vanilla time stays frozen on every planet with its
 * own clock, so vanilla's "is it night" never holds there; each planet dimension type lets a bed be used at any time
 * ({@code bed_rule can_sleep: always}) and this class is the only "when" rule:
 *
 * <ul>
 *   <li>{@link #refusal} (NeoForge, Fabric) or {@link #allowsTime} (Forge) answers a bed click; each loader's hook
 *       runs after vanilla has set the respawn point, so a refused bed is still a spawn point;</li>
 *   <li>{@link #serverTickStart} runs at the start of every server tick, before any level ticks: once enough players
 *       on a planet have slept long enough it sets that planet's clock to dawn through {@link PlanetClock#set} (the
 *       {@code /nerospace clock set} path, which resyncs the clock to the planet's players) and wakes them. It runs
 *       before vanilla's own end-of-night check, so vanilla never sees a full night of sleepers on a planet and never
 *       moves the shared vanilla clock for it.</li>
 * </ul>
 *
 * <p>Greenxertz mirrors the vanilla day, so vanilla owns its sleep and its skip; it is never touched here. A frozen
 * clock (ops freeze, sky gallery) and Nyxara's Nightside let a player rest without skipping anything: each sleeper is
 * woken alone after a deep sleep. A planet sleep never clears planet weather, and the vanilla {@code doDaylightCycle}
 * rule has no say on a planet (the planet clock ignores it everywhere). No player data is stored; the one log line per
 * skip is DEBUG and names no player.</p>
 */
public final class PlanetSleep {

    private PlanetSleep() {
    }

    /** What the sleep rules say about {@code player} using the bed at {@code bed}. */
    public static PlanetSleepRules.Decision decide(ServerPlayer player, BlockPos bed) {
        ServerLevel level = player.level();
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null) {
            return PlanetSleepRules.Decision.VANILLA;
        }
        PlanetSleepRules.World world = worldOf(planet);
        PlanetSleepRules.Side side = world == PlanetSleepRules.World.NYXARA ? side(level, bed) : null;
        return PlanetSleepRules.decide(world, NerospaceConfig.clockEnabled(), PlanetClock.frozen(level),
                PlanetClock.phase(level), planet.sleepWindow(), side);
    }

    /**
     * The bed problem to report when the planet rules refuse this bed, or null to leave vanilla's answer alone. The
     * problem carries the action-bar line ({@code nerospace.sleep.*}).
     */
    @Nullable
    public static Player.BedSleepingProblem refusal(ServerPlayer player, BlockPos bed) {
        PlanetSleepRules.Decision d = decide(player, bed);
        return d.refused() ? SleepCompat.refusal(player, Component.translatable(d.messageKey())) : null;
    }

    /**
     * For a loader whose hook replaces vanilla's "may you sleep at this time" check (Forge's
     * {@code SleepingTimeCheckEvent}): false when the planet rules refuse. That check runs when the bed is clicked
     * (after the respawn point is set) and every tick while asleep, so the action-bar line is sent only on the click
     * (the player is not asleep yet); a refusal while asleep wakes the player, as vanilla does at dawn.
     */
    public static boolean allowsTime(ServerPlayer player, BlockPos bed) {
        PlanetSleepRules.Decision d = decide(player, bed);
        if (d.refused() && !player.isSleeping()) {
            player.sendOverlayMessage(Component.translatable(d.messageKey()));
        }
        return !d.refused();
    }

    /** Planet kind for the rules. */
    static PlanetSleepRules.World worldOf(PlanetDefinition planet) {
        if (planet.clockSource() == ClockSource.LEVEL_TIME) {
            return PlanetSleepRules.World.VANILLA;
        }
        if (planet == Planets.NYXARA) {
            return PlanetSleepRules.World.NYXARA;
        }
        if (planet.traits().fixedClock()) {
            return PlanetSleepRules.World.STATION;
        }
        return PlanetSleepRules.World.CLOCKED;
    }

    /** Nyxara band position of a bed: the biome source's own edges, or the biome when the source is not fixed-X. */
    private static PlanetSleepRules.Side side(ServerLevel level, BlockPos bed) {
        FixedXBands.Side s = NyxaraBand.sideAt(level, bed.getX(), bed.getZ());
        if (s != null) {
            return switch (s) {
                case WEST -> PlanetSleepRules.Side.DAYSIDE;
                case BAND -> PlanetSleepRules.Side.BAND;
                case EAST -> PlanetSleepRules.Side.NIGHTSIDE;
            };
        }
        String biome = level.getBiome(bed).unwrapKey().map(k -> k.identifier().toString()).orElse("");
        if (RadiationRules.NYXARA_NIGHTSIDE.equals(biome)) {
            return PlanetSleepRules.Side.NIGHTSIDE;
        }
        return RadiationRules.NYXARA_DAYSIDE.equals(biome) ? PlanetSleepRules.Side.DAYSIDE
                : PlanetSleepRules.Side.BAND;
    }

    /**
     * Start of every server tick, before the levels tick. O(players) per planet level with a sleeper; nothing at all
     * on a planet where nobody is in bed.
     */
    public static void serverTickStart(MinecraftServer server) {
        for (PlanetDefinition planet : Planets.ALL) {
            if (planet.clockSource() != ClockSource.PLANET_CLOCK) {
                continue; // Greenxertz: vanilla sleep, vanilla skip
            }
            ServerLevel level = server.getLevel(planet.dimension());
            if (level == null || level.players().isEmpty()) {
                continue;
            }
            tickLevel(level, planet);
        }
    }

    private static void tickLevel(ServerLevel level, PlanetDefinition planet) {
        int active = 0;
        int sleeping = 0;
        int deep = 0;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) {
                continue;
            }
            active++;
            if (p.isSleeping()) {
                sleeping++;
                if (p.isSleepingLongEnough()) {
                    deep++;
                }
            }
        }
        if (sleeping == 0) {
            return;
        }
        PlanetSleepRules.World world = worldOf(planet);
        if (world != PlanetSleepRules.World.CLOCKED) {
            // Nyxara's Nightside (and anything that slipped into a station bed): rest, each sleeper woken alone.
            wakeRested(level, null);
            return;
        }
        boolean enabled = NerospaceConfig.clockEnabled();
        double phase = PlanetClock.phase(level);
        if (!enabled || !planet.sleepWindow().contains(phase)) {
            // The night ended (or the clock was switched off) while they slept: get up, nothing to skip.
            wakeAll(level);
            return;
        }
        if (PlanetClock.frozen(level)) {
            wakeRested(level, Component.translatable(PlanetSleepRules.KEY_FROZEN));
            return;
        }
        if (deep > 0 && PlanetSleepRules.skips(SleepCompat.sleepingPercentage(level), active, sleeping, deep)) {
            PlanetClock.set(level, 0.0);
            NerospaceCommon.LOGGER.debug("planet {}: night skipped by sleep ({} players)", planet.englishName(),
                    sleeping);
            wakeAll(level);
        }
    }

    /** Wakes every sleeper on the level (a skip, or the night ended). */
    private static void wakeAll(ServerLevel level) {
        List<ServerPlayer> sleepers = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.isSleeping()) {
                sleepers.add(p);
            }
        }
        for (ServerPlayer p : sleepers) {
            p.stopSleepInBed(false, true);
        }
    }

    /** Wakes each player who has slept long enough, without touching the clock; optional action-bar line. */
    private static void wakeRested(ServerLevel level, @Nullable Component line) {
        List<ServerPlayer> rested = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.isSleeping() && p.isSleepingLongEnough()) {
                rested.add(p);
            }
        }
        for (ServerPlayer p : rested) {
            p.stopSleepInBed(false, true);
            if (line != null) {
                p.sendOverlayMessage(line);
            }
        }
    }
}
