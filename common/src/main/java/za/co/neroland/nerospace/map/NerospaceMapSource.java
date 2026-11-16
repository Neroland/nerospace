package za.co.neroland.nerospace.map;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.api.PlanetId;
import za.co.neroland.nerospace.rocket.PadRegistry;
import za.co.neroland.nerospace.rocket.StationRegistry;
import za.co.neroland.nerospace.route.PadRecord;

/**
 * Where {@link NerospaceMapLayers} reads its data. The provider owns every visibility decision; a source only
 * hands over what the stores already hold in memory, so the rules can be tested against a seeded fake without
 * a running server. Every method answers an empty collection when {@code server} is {@code null}.
 *
 * <p>Implementations never load or generate a chunk and never touch disk beyond the saved-data handles the
 * mod already keeps open.</p>
 */
public interface NerospaceMapSource {

    /** Every founded station (the provider filters by owner and member list). */
    Collection<StationRegistry.StationEntry> stations(@Nullable MinecraftServer server);

    /** Every registered cargo pad (the provider filters by owner, access list and public flag). */
    Collection<PadRecord> routePads(@Nullable MinecraftServer server);

    /** Unowned launch-pad nodes in {@code dimension}. */
    Collection<PadRegistry.PadNode> padNodes(@Nullable MinecraftServer server, ResourceKey<Level> dimension);

    /**
     * Rockets standing landed and empty (not launching, not descending, nobody aboard) among the entities
     * already loaded within {@code radius} blocks of {@code around} in {@code dimension}.
     */
    Collection<ParkedRocket> parkedRockets(@Nullable MinecraftServer server, ResourceKey<Level> dimension,
            BlockPos around, int radius);

    /** Registered terraformer reach in {@code dimension}: the frontier centre and stage-1 radius. */
    Collection<Terraformed> terraformed(@Nullable MinecraftServer server, ResourceKey<Level> dimension);

    /** The worlds {@code player} has reached (their own visit record). */
    Set<PlanetId> visited(@Nullable MinecraftServer server, UUID player);

    /** A landed, empty rocket: the entity's own id (world content, not a person) and where it stands. */
    record ParkedRocket(UUID entityId, BlockPos pos) {
    }

    /** One terraformer's reach. Used only to draw an area; the centre is never drawn as a marker. */
    record Terraformed(BlockPos centre, int radius) {
    }
}
