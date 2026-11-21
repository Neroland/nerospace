package za.co.neroland.nerospace.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import za.co.neroland.nerospace.api.NerospaceVisits;
import za.co.neroland.nerospace.api.PlanetId;
import za.co.neroland.nerospace.rocket.PadRegistry;
import za.co.neroland.nerospace.rocket.RocketEntity;
import za.co.neroland.nerospace.rocket.StationRegistry;
import za.co.neroland.nerospace.route.PadRecord;
import za.co.neroland.nerospace.route.RouteRegistry;
import za.co.neroland.nerospace.village.VillageCoreBlockEntity;
import za.co.neroland.nerospace.village.VillagerCultures;
import za.co.neroland.nerospace.world.TerraformManager;

/** The live source: Nerospace's existing stores and the entities already loaded around the viewer. */
final class ServerMapSource implements NerospaceMapSource {

    /** Vertical reach of the rocket entity box around the viewer; distance is still checked horizontally. */
    private static final int ROCKET_HEIGHT = 384;

    @Override
    public Collection<StationRegistry.StationEntry> stations(@Nullable MinecraftServer server) {
        return server == null ? List.of() : StationRegistry.get(server).all();
    }

    @Override
    public Collection<PadRecord> routePads(@Nullable MinecraftServer server) {
        return server == null ? List.of() : RouteRegistry.get(server).allPads();
    }

    @Override
    public Collection<PadRegistry.PadNode> padNodes(@Nullable MinecraftServer server, ResourceKey<Level> dimension) {
        return server == null ? List.of() : PadRegistry.get(server).inDimension(dimension);
    }

    @Override
    public Collection<ParkedRocket> parkedRockets(@Nullable MinecraftServer server, ResourceKey<Level> dimension,
            BlockPos around, int radius) {
        ServerLevel level = server == null ? null : server.getLevel(dimension);
        if (level == null) {
            return List.of();
        }
        AABB box = new AABB(around.getX() - radius, around.getY() - ROCKET_HEIGHT, around.getZ() - radius,
                around.getX() + radius + 1, around.getY() + ROCKET_HEIGHT, around.getZ() + radius + 1);
        List<ParkedRocket> out = new ArrayList<>();
        for (RocketEntity rocket : level.getEntitiesOfClass(RocketEntity.class, box)) {
            if (!rocket.isRemoved() && !rocket.isLaunching() && !rocket.isDescending()
                    && rocket.getPassengers().isEmpty()) {
                out.add(new ParkedRocket(rocket.getUUID(), rocket.blockPosition()));
            }
        }
        return out;
    }

    @Override
    public Collection<Terraformed> terraformed(@Nullable MinecraftServer server, ResourceKey<Level> dimension) {
        ServerLevel level = server == null ? null : server.getLevel(dimension);
        if (level == null) {
            return List.of();
        }
        List<Terraformed> out = new ArrayList<>();
        TerraformManager.get(level).forEachMachine((centre, radius, hydration, life) -> {
            if (radius > 0) {
                out.add(new Terraformed(centre, radius));
            }
        });
        return out;
    }

    @Override
    public Collection<Habitat> habitatsNear(@Nullable MinecraftServer server, ResourceKey<Level> dimension,
            BlockPos centre, int radius) {
        ServerLevel level = server == null ? null : server.getLevel(dimension);
        if (level == null) {
            return List.of();
        }
        // The culture follows the dimension, exactly as the settlement builder picks it (Xertz where none).
        String culture = VillagerCultures.forDimension(dimension).orElse(VillagerCultures.XERTZ).id();
        List<Habitat> out = new ArrayList<>();
        int minCx = (centre.getX() - radius) >> 4;
        int maxCx = (centre.getX() + radius) >> 4;
        int minCz = (centre.getZ() - radius) >> 4;
        int maxCz = (centre.getZ() + radius) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz); // loaded chunks only
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof VillageCoreBlockEntity core && !core.isRemoved()) {
                        out.add(new Habitat(core.getBlockPos().immutable(), culture));
                    }
                }
            }
        }
        return out;
    }

    @Override
    public Set<PlanetId> visited(@Nullable MinecraftServer server, UUID player) {
        return server == null ? Set.of() : NerospaceVisits.visitedPlanets(server, player);
    }
}
