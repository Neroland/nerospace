package za.co.neroland.nerospace.world.planet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.registry.ModTags;

/**
 * Resolves the hazard at an exact position, in priority order: weather radiation (a Mars dust storm or a Nyxara
 * flare irradiates the open surface), then a biome hazard tag (radiation, then heat, then cold), then the planet's primary trait
 * hazard. Non-planet dimensions are always {@link Hazard#NONE}.
 */
public final class PlanetHazards {

    private PlanetHazards() {
    }

    public static Hazard at(ServerLevel level, BlockPos pos) {
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null) {
            return Hazard.NONE;
        }
        if (PlanetWeather.raisesRadiation(level, pos)) {
            return Hazard.RADIATION;
        }
        if (level.hasChunkAt(pos)) {
            Holder<Biome> biome = level.getBiome(pos);
            if (biome.is(ModTags.Biomes.HAZARD_RADIATION)) {
                return Hazard.RADIATION;
            }
            if (biome.is(ModTags.Biomes.HAZARD_HEAT)) {
                return Hazard.HEAT;
            }
            if (biome.is(ModTags.Biomes.HAZARD_COLD)) {
                // A flaring Dawnband Stone nearby keeps the cold away.
                return za.co.neroland.nerospace.world.planet.nyxara.NyxaraWarmth.shieldedFromCold(level, pos)
                        ? Hazard.NONE : Hazard.COLD;
            }
        }
        Hazard trait = planet.traits().hazard();
        return trait == Hazard.COLD
                && za.co.neroland.nerospace.world.planet.nyxara.NyxaraWarmth.shieldedFromCold(level, pos)
                ? Hazard.NONE : trait;
    }

    /**
     * Effective radiation (0..3) at a position: the planet trait, Nyxara's band biomes overriding it, plus weather
     * on exposed positions outside an oxygen field (dust storm lifts to 2, a flare adds 1 off the nightside).
     * 0 off a planet. Reads only loaded chunks.
     */
    public static int radiationLevel(ServerLevel level, BlockPos pos) {
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null) {
            return 0;
        }
        String biome = biomeId(level, pos);
        int base = RadiationRules.base(planet.traits().radiation(), biome);
        WeatherEvent event = PlanetWeather.event(level);
        boolean exposed = event != WeatherEvent.NONE && PlanetWeather.exposedUnsealed(level, pos);
        return RadiationRules.effective(base, event, biome, exposed);
    }

    /** The biome id at a loaded position as {@code namespace:path}, or "" when unloaded/unbound. */
    static String biomeId(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return "";
        }
        return level.getBiome(pos).unwrapKey().map(k -> k.identifier().toString()).orElse("");
    }
}
