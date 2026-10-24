package za.co.neroland.nerospace.world.planet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.api.PlanetId;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.world.planet.cindara.CindaraPlanet;
import za.co.neroland.nerospace.world.planet.glacira.GlaciraPlanet;
import za.co.neroland.nerospace.world.planet.greenxertz.GreenxertzPlanet;
import za.co.neroland.nerospace.world.planet.luna.LunaPlanet;
import za.co.neroland.nerospace.world.planet.mars.MarsPlanet;
import za.co.neroland.nerospace.world.planet.nyxara.NyxaraPlanet;
import za.co.neroland.nerospace.world.planet.station.StationPlanet;

/**
 * The registry of Nerospace worlds. Order is the public 1.x order first (Greenxertz, Cindara, Glacira, Station)
 * with the 2.0 worlds appended (Luna, Mars, Nyxara) — sibling mods iterate it, so it is append-only.
 *
 * <p>{@code nerospace:capture} (the gallery backdrop dimension) is deliberately <b>not</b> a planet.</p>
 */
public final class Planets {

    public static final PlanetDefinition GREENXERTZ = GreenxertzPlanet.INSTANCE;
    public static final PlanetDefinition CINDARA = CindaraPlanet.INSTANCE;
    public static final PlanetDefinition GLACIRA = GlaciraPlanet.INSTANCE;
    public static final PlanetDefinition STATION = StationPlanet.INSTANCE;
    public static final PlanetDefinition LUNA = LunaPlanet.INSTANCE;
    public static final PlanetDefinition MARS = MarsPlanet.INSTANCE;
    public static final PlanetDefinition NYXARA = NyxaraPlanet.INSTANCE;

    /** Every planet, in the stable public order. */
    public static final List<PlanetDefinition> ALL = List.of(GREENXERTZ, CINDARA, GLACIRA, STATION, LUNA, MARS, NYXARA);

    private static final Map<ResourceKey<Level>, PlanetDefinition> BY_DIMENSION = new HashMap<>();
    private static final Map<Identifier, PlanetDefinition> BY_ID = new HashMap<>();

    static {
        for (PlanetDefinition p : ALL) {
            BY_DIMENSION.put(p.dimension(), p);
            BY_ID.put(p.id().id(), p);
        }
    }

    private Planets() {
    }

    public static Optional<PlanetDefinition> byDimension(@Nullable ResourceKey<Level> dimension) {
        return Optional.ofNullable(dimension == null ? null : BY_DIMENSION.get(dimension));
    }

    /** The planet for a dimension, or null when the dimension is not a Nerospace planet. */
    @Nullable
    public static PlanetDefinition get(@Nullable ResourceKey<Level> dimension) {
        return dimension == null ? null : BY_DIMENSION.get(dimension);
    }

    public static Optional<PlanetDefinition> byId(@Nullable PlanetId id) {
        return Optional.ofNullable(id == null ? null : BY_ID.get(id.id()));
    }

    public static Optional<PlanetDefinition> byId(@Nullable Identifier id) {
        return Optional.ofNullable(id == null ? null : BY_ID.get(id));
    }

    public static boolean isPlanet(@Nullable ResourceKey<Level> dimension) {
        return get(dimension) != null;
    }

    /**
     * Whether {@code dimension} can be travelled to on this server: the Overworld always, a planet only when
     * its dimension exists, anything else never. A null server (client side) treats every planet as available;
     * the server's synced destination mask is the authority there.
     */
    public static boolean isAvailable(@Nullable MinecraftServer server, ResourceKey<Level> dimension) {
        if (ModDimensions.OVERWORLD.equals(dimension)) {
            return true;
        }
        PlanetDefinition p = get(dimension);
        if (p == null) {
            return false;
        }
        return server == null || p.isAvailable(server);
    }
}
