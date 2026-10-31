package za.co.neroland.nerospace.world.planet;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.network.ModNetwork;
import za.co.neroland.nerospace.network.PlanetWeatherPayload;
import za.co.neroland.nerospace.world.OxygenManager;

/**
 * Server-authoritative planet weather: one {@link WeatherMachine} per planet level, stepped
 * once per second, persisted in {@link PlanetWeatherState}. The client only receives a tiny enum + intensity
 * payload to render. Vanilla rain is untouched (Greenxertz keeps it alongside spore blooms).
 *
 * <p>This applies the effects that need no new blocks: Mars storm radiation outdoors (via
 * {@link #raisesRadiation}, read by the hazard resolver) and solar dimming ({@link #solarFactor}), and spore-bloom
 * nausea for players outside a sealed suit or oxygen field. Block effects hang off {@link WeatherEffects}.</p>
 */
public final class PlanetWeather {

    /** Payload resend cadence while an event is active (client interpolates between). */
    private static final int ACTIVE_SYNC_TICKS = 100;
    /** Spore-bloom nausea: applied every 200 ticks for 100 ticks to exposed players. */
    private static final int SPORE_NAUSEA_PERIOD = 200;
    private static final int SPORE_NAUSEA_TICKS = 100;

    /**
     * Per-level reference to the weather store (the store object lives as long as its level), so per-panel and
     * per-player reads do not go through the saved-data lookup every time. Weak keys: no level is kept alive.
     */
    private static final Map<ServerLevel, PlanetWeatherState> STATES = Collections.synchronizedMap(new WeakHashMap<>());

    private PlanetWeather() {
    }

    private static PlanetWeatherState state(ServerLevel level) {
        return STATES.computeIfAbsent(level, PlanetWeatherState::get);
    }

    /** The config-scaled weather parameters for a planet. */
    public static WeatherParams params(PlanetDefinition planet) {
        return planet.weather().scaled(NerospaceConfig.weatherFrequencyMultiplier(),
                NerospaceConfig.weatherDurationMultiplier());
    }

    /**
     * Per-server tick. The state machine steps once per second on every loaded planet level, players or not (so
     * a storm never freezes at PEAK over unattended machinery); the payload resend and player effects only run
     * where players are.
     */
    public static void tick(MinecraftServer server) {
        for (PlanetDefinition planet : Planets.ALL) {
            if (!planet.weather().hasWeather()) {
                continue;
            }
            ServerLevel level = server.getLevel(planet.dimension());
            if (level == null) {
                continue;
            }
            long time = level.getGameTime();
            PlanetWeatherState state = state(level);
            WeatherMachine m = state.machine;
            if (time % WeatherMachine.STEP_TICKS == 0) {
                stepLevel(level, planet, state);
            }
            if (level.players().isEmpty()) {
                continue;
            }
            if (m.active() && time % ACTIVE_SYNC_TICKS == 0) {
                syncLevel(level);
            }
            if (m.active() && m.event == WeatherEvent.SPORE_BLOOM && time % SPORE_NAUSEA_PERIOD == 0) {
                applySporeNausea(level);
            }
        }
    }

    private static void stepLevel(ServerLevel level, PlanetDefinition planet, PlanetWeatherState state) {
        WeatherMachine m = state.machine;
        WeatherEvent before = m.event;
        boolean enabled = NerospaceConfig.weatherEnabled() && NerospaceConfig.planetWeatherEnabled(planet.weatherConfigKey());
        boolean mayStart = planet.weatherMayStart(PlanetClock.phase(level));
        WeatherMachine.Change change = m.step(params(planet), enabled, mayStart, () -> level.getRandom().nextDouble());
        if (change == WeatherMachine.Change.NONE) {
            if (m.active()) {
                WeatherEffects.Registry.get(m.event).onSecond(level, m);
                state.markChanged();
            }
            return;
        }
        state.markChanged();
        switch (change) {
            case STARTED -> WeatherEffects.Registry.get(m.event).onStart(level, m);
            case ENDED -> WeatherEffects.Registry.get(before).onEnd(level, before);
            default -> WeatherEffects.Registry.get(m.event).onSecond(level, m);
        }
        syncLevel(level);
    }

    private static void applySporeNausea(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.getAbilities().instabuild) {
                continue;
            }
            if (OxygenManager.isSealedFromAtmosphere(level, player)) {
                continue;
            }
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, SPORE_NAUSEA_TICKS, 0, true, false));
        }
    }

    // --- Queries ---------------------------------------------------------------------------------------

    /** The machine for a planet level (read-only use), or null for a level without weather. */
    @Nullable
    public static WeatherMachine current(ServerLevel level) {
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null || !planet.weather().hasWeather()) {
            return null;
        }
        return state(level).machine;
    }

    /** The active event at a level, NONE when clear. */
    public static WeatherEvent event(ServerLevel level) {
        WeatherMachine m = current(level);
        return m != null && m.active() ? m.event : WeatherEvent.NONE;
    }

    /** Intensity in [0,1] of the active event (0 when clear). */
    public static float intensity(ServerLevel level) {
        WeatherMachine m = current(level);
        return m != null && m.active() ? m.intensity : 0.0F;
    }

    /**
     * Whether the weather irradiates {@code pos}: a Mars dust storm or a Nyxara flare (dayside and band only),
     * outdoors (open to the sky) and outside an oxygen field. Reads only loaded chunks.
     */
    public static boolean raisesRadiation(ServerLevel level, BlockPos pos) {
        WeatherEvent event = event(level);
        if (event != WeatherEvent.DUST_STORM && event != WeatherEvent.FLARE) {
            return false;
        }
        if (!exposedUnsealed(level, pos)) {
            return false;
        }
        return event == WeatherEvent.DUST_STORM
                || RadiationRules.flareApplies(event, PlanetHazards.biomeId(level, pos), true);
    }

    /** Open to the sky (at or above the motion-blocking surface) and outside a sealed oxygen field. Loaded only. */
    public static boolean exposedUnsealed(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return false;
        }
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        return pos.getY() >= surface - 1 && !OxygenManager.isBreathableAt(level, pos);
    }

    /** Weather multiplier on solar output at a level, 1.0 when clear (Mars storm: 1 − 0.8·intensity). */
    public static double solarFactor(Level level) {
        if (!(level instanceof ServerLevel server)) {
            return 1.0;
        }
        WeatherMachine m = current(server);
        if (m == null || !m.active()) {
            return 1.0;
        }
        return m.event == WeatherEvent.DUST_STORM ? 1.0 - 0.8 * m.intensity : 1.0;
    }

    // --- Ops -------------------------------------------------------------------------------------------

    /** Starts the planet's event now. @return false when the planet has no weather. */
    public static boolean start(ServerLevel level, float intensity) {
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null || !planet.weather().hasWeather()) {
            return false;
        }
        PlanetWeatherState state = state(level);
        WeatherParams p = params(planet);
        state.machine.forceStart(p, intensity, p.maxDurationTicks(), level.getRandom().nextLong());
        state.markChanged();
        WeatherEffects.Registry.get(state.machine.event).onStart(level, state.machine);
        syncLevel(level);
        return true;
    }

    /**
     * Starts the planet's event now at full strength, already at its peak (the sky gallery): the same state machine
     * as {@link #start}, without the ramp. @return false when the planet has no weather.
     */
    public static boolean startAtPeak(ServerLevel level) {
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null || !planet.weather().hasWeather()) {
            return false;
        }
        PlanetWeatherState state = state(level);
        WeatherParams p = params(planet);
        state.machine.forceStart(p, WeatherMachine.MAX_TARGET, p.maxDurationTicks(), level.getRandom().nextLong());
        state.machine.reachPeak();
        state.markChanged();
        WeatherEffects.Registry.get(state.machine.event).onStart(level, state.machine);
        syncLevel(level);
        return true;
    }

    /** Whether the server settings let this planet's weather run at all (a started event is cleared otherwise). */
    public static boolean enabledFor(PlanetDefinition planet) {
        return NerospaceConfig.weatherEnabled() && NerospaceConfig.planetWeatherEnabled(planet.weatherConfigKey());
    }

    /** Clears the weather now. @return false when the planet has no weather. */
    public static boolean clear(ServerLevel level) {
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null || !planet.weather().hasWeather()) {
            return false;
        }
        PlanetWeatherState state = state(level);
        WeatherEvent before = state.machine.event;
        boolean wasActive = state.machine.active();
        state.machine.forceClear(params(planet));
        state.markChanged();
        if (wasActive) {
            WeatherEffects.Registry.get(before).onEnd(level, before);
        }
        syncLevel(level);
        return true;
    }

    // --- Sync ------------------------------------------------------------------------------------------

    public static PlanetWeatherPayload snapshot(ServerLevel level) {
        WeatherMachine m = current(level);
        if (m == null || !m.active()) {
            return PlanetWeatherPayload.clear(level.dimension().identifier());
        }
        return new PlanetWeatherPayload(level.dimension().identifier(), (byte) m.event.ordinal(),
                (byte) m.state.ordinal(), m.intensity, m.seed);
    }

    public static void syncTo(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, snapshot(player.level()));
    }

    public static void syncLevel(ServerLevel level) {
        if (level.players().isEmpty()) {
            return;
        }
        PlanetWeatherPayload payload = snapshot(level);
        for (ServerPlayer player : level.players()) {
            ModNetwork.sendToPlayer(player, payload);
        }
    }
}
