package za.co.neroland.nerospace.village;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import za.co.neroland.nerospace.entity.AlienVillager;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;

/**
 * The four villager cultures (003 §8.2). Luna, Mars and the station have none: silence on the Moon, wrecks and a
 * salvage drone on Mars. A planet names its culture through {@code PlanetTraits.villagerCulture()}.
 */
public final class VillagerCultures {

    public static final VillagerCulture XERTZ = new VillagerCulture(AlienTrades.XERTZ,
            AlienVillager.Planet.GREENXERTZ, true, false, "nerospace:xertz_quartz");
    public static final VillagerCulture EMBER_KIN = new VillagerCulture(AlienTrades.EMBER_KIN,
            AlienVillager.Planet.CINDARA, false, false, "nerospace:ash_dust");
    public static final VillagerCulture FROSTBOUND = new VillagerCulture(AlienTrades.FROSTBOUND,
            AlienVillager.Planet.GLACIRA, false, false, "nerospace:frost_shard");
    public static final VillagerCulture DUSKBORN = new VillagerCulture(AlienTrades.DUSKBORN,
            AlienVillager.Planet.NYXARA, true, true, "nerospace:shadow_lichen");

    /** In {@link AlienVillager.Planet} ordinal order. */
    public static final List<VillagerCulture> ALL = List.of(XERTZ, EMBER_KIN, FROSTBOUND, DUSKBORN);

    /** The Nyxara twilight band, where the Duskborn live. */
    public static final TagKey<Biome> NYXARA_BAND = TagKey.create(Registries.BIOME,
            Identifier.fromNamespaceAndPath("nerospace", "nyxara_band"));

    private VillagerCultures() {
    }

    public static Optional<VillagerCulture> byId(@Nullable String id) {
        for (VillagerCulture c : ALL) {
            if (c.id().equals(id)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    /** The culture of a persisted species; an unknown value degrades to Xertz (the 1.x default). */
    public static VillagerCulture byLegacy(@Nullable AlienVillager.Planet planet) {
        for (VillagerCulture c : ALL) {
            if (c.legacy() == planet) {
                return c;
            }
        }
        return XERTZ;
    }

    /** The culture native to a dimension, or empty (Luna, Mars, station, overworld, any non-planet). */
    public static Optional<VillagerCulture> forDimension(@Nullable ResourceKey<Level> dimension) {
        PlanetDefinition planet = Planets.get(dimension);
        if (planet == null) {
            return Optional.empty();
        }
        return planet.traits().villagerCulture().flatMap(id -> byId(id.toString()));
    }

    /** Whether a settlement of {@code culture} may stand in {@code biome} (the Duskborn band rule). */
    public static boolean allowsBiome(VillagerCulture culture, Holder<Biome> biome) {
        return !culture.bandOnly() || biome.is(NYXARA_BAND);
    }
}
