package za.co.neroland.nerospace.world.planet.greenxertz;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerospace.world.planet.WeatherEffects;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.planet.WeatherMachine;

/**
 * Greenxertz's spore-bloom hook. The bloom's gameplay already runs elsewhere: Spore Moss reads the event itself
 * (spread x3 per random tick, never extra ticks) and the exposed-player nausea is in {@code PlanetWeather}. This
 * handler adds the visible part on the server side: drifting spore particles around each player in the open, scaled
 * by intensity, once per second. No block is touched and nothing is stored. Call once at common init.
 */
public final class GreenxertzWeather {

    /** Particles per player per second at full intensity. */
    public static final int MAX_PARTICLES = 24;

    private static boolean registered;

    private GreenxertzWeather() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        WeatherEffects.Registry.register(WeatherEvent.SPORE_BLOOM, new WeatherEffects() {
            @Override
            public void onSecond(ServerLevel level, WeatherMachine weather) {
                int count = particles(weather.intensity);
                if (count <= 0) {
                    return;
                }
                for (ServerPlayer player : level.players()) {
                    if (!level.canSeeSky(player.blockPosition().above())) {
                        continue;
                    }
                    level.sendParticles(player, ParticleTypes.SPORE_BLOSSOM_AIR, false, false, player.getX(),
                            player.getY() + 2.0, player.getZ(), count, 6.0, 3.0, 6.0, 0.0);
                }
            }
        });
    }

    /** Pure: particles per player for an intensity in [0, 1]. */
    public static int particles(float intensity) {
        float i = Math.max(0.0F, Math.min(1.0F, intensity));
        return Math.round(MAX_PARTICLES * i);
    }
}
