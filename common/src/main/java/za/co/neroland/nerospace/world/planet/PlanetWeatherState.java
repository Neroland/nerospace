package za.co.neroland.nerospace.world.planet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import za.co.neroland.nerolandcore.data.SavedDataCompat;
import za.co.neroland.nerolandcore.data.SavedDataRecovery;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Per-level persisted weather state ({@code nerospace:planet_weather}): the {@link WeatherMachine} fields, with
 * the enums stored <b>by name</b>. Every field optional; a missing file is CLEAR. Holds no player data.
 */
public final class PlanetWeatherState extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "planet_weather");
    public static final String RECOVERY_NAME = "nerospace:planet_weather";

    public static final SavedDataType<PlanetWeatherState> TYPE =
            SavedDataCompat.type(ID, PlanetWeatherState::new, codec());

    final WeatherMachine machine = new WeatherMachine();

    public PlanetWeatherState() {
    }

    public static PlanetWeatherState get(ServerLevel level) {
        return SavedDataRecovery.get(level, TYPE, PlanetWeatherState::new, RECOVERY_NAME);
    }

    /** Read-only view of the machine (callers must not mutate it). */
    public WeatherMachine machine() {
        return this.machine;
    }

    private static Codec<PlanetWeatherState> codec() {
        return RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("state", WeatherState.CLEAR.name()).forGetter(s -> s.machine.state.name()),
                Codec.STRING.optionalFieldOf("event", WeatherEvent.NONE.name()).forGetter(s -> s.machine.event.name()),
                Codec.FLOAT.optionalFieldOf("intensity", 0.0F).forGetter(s -> s.machine.intensity),
                Codec.FLOAT.optionalFieldOf("target", 0.0F).forGetter(s -> s.machine.target),
                Codec.INT.optionalFieldOf("remaining_ticks", 0).forGetter(s -> s.machine.remainingTicks),
                Codec.INT.optionalFieldOf("cooldown_ticks", -1).forGetter(s -> s.machine.cooldownTicks),
                Codec.LONG.optionalFieldOf("seed", 0L).forGetter(s -> s.machine.seed)
        ).apply(i, PlanetWeatherState::of));
    }

    private static PlanetWeatherState of(String state, String event, float intensity, float target, int remaining,
            int cooldown, long seed) {
        PlanetWeatherState s = new PlanetWeatherState();
        s.machine.state = WeatherState.byName(state);
        s.machine.event = WeatherEvent.byName(event);
        s.machine.intensity = Math.max(0.0F, Math.min(1.0F, intensity));
        s.machine.target = Math.max(0.0F, Math.min(1.0F, target));
        s.machine.remainingTicks = Math.max(0, remaining);
        s.machine.cooldownTicks = cooldown;
        s.machine.seed = seed;
        if (s.machine.event == WeatherEvent.NONE) {
            s.machine.state = WeatherState.CLEAR;
        }
        return s;
    }

    void markChanged() {
        setDirty();
    }
}
