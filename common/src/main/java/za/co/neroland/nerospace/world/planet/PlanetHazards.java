package za.co.neroland.nerospace.world.planet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.registry.ModTags;

/**
 * Resolves the hazard at an exact position, in priority order: weather radiation (a Mars dust storm irradiates
 * the open surface), then a biome hazard tag (radiation, then heat, then cold), then the planet's primary trait
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
                return Hazard.COLD;
            }
        }
        return planet.traits().hazard();
    }
}
