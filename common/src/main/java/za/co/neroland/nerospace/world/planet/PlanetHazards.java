package za.co.neroland.nerospace.world.planet;

import java.util.EnumSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.block.rules.PrismBeamRules;
import za.co.neroland.nerospace.block.rules.RadShadeRules;
import za.co.neroland.nerospace.registry.ModTags;
import za.co.neroland.nerospace.world.OxygenManager;
import za.co.neroland.nerospace.world.planet.nyxara.NyxaraCrystalHazards;

/**
 * Resolves the hazard at an exact position. The player-less {@link #at(ServerLevel, BlockPos)} returns one primary
 * hazard in priority order: weather radiation (a Mars dust storm or a Nyxara flare irradiates the open surface), then a
 * biome hazard tag (radiation, then heat, then cold), then the planet's primary trait hazard. The per-player
 * {@link #at(ServerLevel, BlockPos, Player)} collects every hazard present ({@link #present}) and returns the first one
 * in [RADIATION, HEAT, COLD] the player is not shielded from ({@link HazardOrderRules}). Non-planet dimensions are
 * always {@link Hazard#NONE}.
 */
public final class PlanetHazards {

    private PlanetHazards() {
    }

    /**
     * True for a creature native to a Nerospace planet ({@code #nerospace:native_fauna}: Moon Hopper, Dust Strider,
     * Dusk Moth). Every hazard that touches non-player living entities checks this first and leaves natives alone.
     * Oxygen ({@code OxygenManager}) and weather radiation only ever tick players, so natives need no check there.
     */
    public static boolean isNative(Entity entity) {
        // EntityType#is(TagKey) is gone on 26.3; the registry holder answers the same on every line.
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType()).is(ModTags.EntityTypes.NATIVE_FAUNA);
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
     * Every hazard present at a position: weather radiation, each matching biome hazard tag and the planet's trait
     * hazard. Empty off a planet. Works on either side: on a client level the weather term is left out (the client
     * HUD passes its synced weather through {@link #present(Level, BlockPos, boolean)}).
     */
    public static Set<Hazard> present(ServerLevel level, BlockPos pos) {
        return present(level, pos, PlanetWeather.raisesRadiation(level, pos));
    }

    /** {@link #present(ServerLevel, BlockPos)} with the weather-radiation term supplied by the caller. */
    public static Set<Hazard> present(Level level, BlockPos pos, boolean weatherRadiation) {
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null) {
            return EnumSet.noneOf(Hazard.class);
        }
        boolean rad = false;
        boolean heat = false;
        boolean cold = false;
        if (level.hasChunkAt(pos)) {
            Holder<Biome> biome = level.getBiome(pos);
            rad = biome.is(ModTags.Biomes.HAZARD_RADIATION);
            heat = biome.is(ModTags.Biomes.HAZARD_HEAT);
            cold = biome.is(ModTags.Biomes.HAZARD_COLD);
        }
        return HazardOrderRules.present(planet.traits().hazard(), rad, heat, cold, weatherRadiation);
    }

    /**
     * Whether any cold at this position is frigid: Glacira everywhere, and the biomes tagged colder than their planet
     * (the Nyxara Nightside, the Martian Polar Cap). Frigid cold needs the Cryo Suit; the cold of a cold world (Mars)
     * is kept out by any Tier 2 class suit. Works on either side; false off a planet.
     */
    public static boolean frigidCold(Level level, BlockPos pos) {
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null) {
            return false;
        }
        boolean coldBiome = level.hasChunkAt(pos) && level.getBiome(pos).is(ModTags.Biomes.HAZARD_COLD);
        return HazardOrderRules.frigidCold(planet.traits().temperatureBand(), coldBiome);
    }

    /**
     * The hazards this player's worn suit keeps out at this position ({@link HazardOrderRules#shields}): Thermal heat,
     * Cryo cold, any Tier 2 class suit against a cold world's milder cold, and a full Rad Lining. Client-safe.
     */
    public static Set<Hazard> suitShields(Level level, BlockPos pos, Player player) {
        return HazardOrderRules.shields(OxygenManager.suitClass(player), OxygenManager.radLined(player),
                frigidCold(level, pos));
    }

    /**
     * The hazard that bites this player here: the first of [RADIATION, HEAT, COLD] present at {@code pos} that the
     * player is not shielded from. Shields: the Thermal suit (HEAT), the Cryo suit (COLD), any Tier 2 class suit
     * against cold that is not frigid ({@link #frigidCold}), a full Rad-Lined suit (RADIATION), and a flaring
     * Dawnband Stone within 4 blocks (COLD, read only when COLD would otherwise bite). The player-less overload and
     * {@code traits().hazard()} are unchanged.
     */
    public static Hazard at(ServerLevel level, BlockPos pos, Player player) {
        Set<Hazard> present = present(level, pos);
        if (present.isEmpty()) {
            return Hazard.NONE;
        }
        HazardOrderRules.SuitClass suit = OxygenManager.suitClass(player);
        boolean lined = OxygenManager.radLined(player);
        boolean frigid = frigidCold(level, pos);
        Hazard first = HazardOrderRules.resolve(present, suit, lined, frigid, false);
        if (first == Hazard.COLD
                && za.co.neroland.nerospace.world.planet.nyxara.NyxaraWarmth.shieldedFromCold(level, pos)) {
            first = HazardOrderRules.resolve(present, suit, lined, frigid, true);
        }
        return first;
    }

    /**
     * Effective radiation (0..3) at a position: the planet trait, Nyxara's band biomes overriding it, one lower under a
     * Sunglass Crystal roof (three cells above the head), plus weather on exposed positions outside an oxygen field (dust
     * storm lifts to 2, a flare adds 1 off the nightside, 2 inside a flaring Nyx Prism's beam).
     * 0 off a planet. Reads only loaded chunks.
     */
    public static int radiationLevel(ServerLevel level, BlockPos pos) {
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null) {
            return 0;
        }
        String biome = biomeId(level, pos);
        int base = RadShadeRules.effectiveRadiation(RadiationRules.base(planet.traits().radiation(), biome),
                NyxaraCrystalHazards.shaded(level, pos));
        WeatherEvent event = PlanetWeather.event(level);
        boolean exposed = event != WeatherEvent.NONE && PlanetWeather.exposedUnsealed(level, pos);
        int effective = RadiationRules.effective(base, event, biome, exposed);
        if (RadiationRules.flareApplies(event, biome, exposed)) {
            // A flaring Nyx Prism's beam turns the flare's +1 into +2.
            effective += PrismBeamRules.flareRadiation(true, NyxaraCrystalHazards.inPrismBeam(level, pos))
                    - PrismBeamRules.flareRadiation(true, false);
        }
        return Math.min(RadiationRules.MAX, effective);
    }

    /** The biome id at a loaded position as {@code namespace:path}, or "" when unloaded/unbound. */
    static String biomeId(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return "";
        }
        return level.getBiome(pos).unwrapKey().map(k -> k.identifier().toString()).orElse("");
    }
}
