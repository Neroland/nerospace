package za.co.neroland.nerospace.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import za.co.neroland.nerolandcore.map.MapArea;
import za.co.neroland.nerolandcore.map.MapBounds;
import za.co.neroland.nerolandcore.map.MapDimensionBadge;
import za.co.neroland.nerolandcore.map.MapLayerConformance;
import za.co.neroland.nerolandcore.map.MapViewer;
import za.co.neroland.nerospace.api.PlanetId;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.rocket.PadRegistry;
import za.co.neroland.nerospace.rocket.StationRegistry;
import za.co.neroland.nerospace.route.PadRecord;

/** Core's map-layer conformance fixture over Nerospace's layers, with seeded data and a third player. */
class NerospaceMapLayersTest {

    static final ResourceKey<Level> STATION = ModDimensions.STATION_LEVEL;
    static final ResourceKey<Level> ELSEWHERE =
            ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("nerospace", "nowhere"));
    static final String STATION_ID = PadRegistry.dimId(STATION);

    static final UUID OWNER = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
    static final UUID MEMBER = UUID.fromString("00000000-0000-4000-8000-0000000000b2");
    static final UUID STRANGER = UUID.fromString("00000000-0000-4000-8000-0000000000c3");
    static final UUID THIRD = UUID.fromString("00000000-0000-4000-8000-0000000000d4");

    /** A seeded, in-memory source. Positions sit near (0, 0) in the station dimension. */
    static final class Seeded implements NerospaceMapSource {
        final List<StationRegistry.StationEntry> stations = new ArrayList<>();
        final List<PadRecord> pads = new ArrayList<>();
        final List<PadRegistry.PadNode> nodes = new ArrayList<>();
        final List<ParkedRocket> rockets = new ArrayList<>();
        final List<Terraformed> terraformed = new ArrayList<>();
        final List<Habitat> habitats = new ArrayList<>();
        Set<PlanetId> visited = Set.of();

        @Override
        public Collection<StationRegistry.StationEntry> stations(@Nullable MinecraftServer server) {
            return stations;
        }

        @Override
        public Collection<PadRecord> routePads(@Nullable MinecraftServer server) {
            return pads;
        }

        @Override
        public Collection<PadRegistry.PadNode> padNodes(@Nullable MinecraftServer server, ResourceKey<Level> dim) {
            return STATION.equals(dim) ? nodes : List.of();
        }

        @Override
        public Collection<ParkedRocket> parkedRockets(@Nullable MinecraftServer server, ResourceKey<Level> dim,
                BlockPos around, int radius) {
            return STATION.equals(dim) ? rockets : List.of(); // deliberately unfiltered: the provider must filter
        }

        @Override
        public Collection<Terraformed> terraformed(@Nullable MinecraftServer server, ResourceKey<Level> dim) {
            return STATION.equals(dim) ? terraformed : List.of();
        }

        @Override
        public Set<PlanetId> visited(@Nullable MinecraftServer server, UUID player) {
            return OWNER.equals(player) ? visited : Set.of();
        }

        @Override
        public Collection<Habitat> habitatsNear(@Nullable MinecraftServer server, ResourceKey<Level> dim,
                BlockPos centre, int radius) {
            return STATION.equals(dim) ? habitats : List.of(); // deliberately unfiltered: the provider must filter
        }
    }

    static Seeded seeded() {
        Seeded s = new Seeded();
        s.stations.add(new StationRegistry.StationEntry(1, "Alpha", new BlockPos(10, 64, 10), OWNER.toString())
                .withMembers(List.of(MEMBER.toString())));
        s.stations.add(new StationRegistry.StationEntry(2, "Charlie Base", new BlockPos(20, 64, 20),
                THIRD.toString()));
        s.stations.add(new StationRegistry.StationEntry(3, "Erased", new BlockPos(30, 64, 30), ""));
        s.pads.add(new PadRecord(1, "Home pad", STATION_ID, new BlockPos(5, 64, 5), OWNER.toString(), false,
                List.of(MEMBER.toString())));
        s.pads.add(new PadRecord(2, "Charlie Private", STATION_ID, new BlockPos(6, 64, 6), THIRD.toString(), false,
                List.of()));
        s.pads.add(new PadRecord(3, "Charlie Public", STATION_ID, new BlockPos(7, 64, 7), THIRD.toString(), true,
                List.of()));
        s.pads.add(new PadRecord(4, "", STATION_ID, new BlockPos(8, 64, -8), "", false, List.of()));
        s.pads.add(new PadRecord(5, "Charlie Far", STATION_ID, new BlockPos(300, 64, 300), THIRD.toString(), true,
                List.of()));
        s.nodes.add(new PadRegistry.PadNode(1, "Charlie Node", STATION_ID, new BlockPos(-12, 64, 3)));
        s.nodes.add(new PadRegistry.PadNode(2, "Far Node", STATION_ID, new BlockPos(-500, 64, 3)));
        s.rockets.add(new NerospaceMapSource.ParkedRocket(UUID.fromString("00000000-0000-4000-8000-00000000e5e5"),
                new BlockPos(3, 64, -20)));
        s.rockets.add(new NerospaceMapSource.ParkedRocket(UUID.fromString("00000000-0000-4000-8000-00000000f6f6"),
                new BlockPos(900, 64, 0)));
        s.terraformed.add(new NerospaceMapSource.Terraformed(new BlockPos(40, 64, 0), 48));
        s.terraformed.add(new NerospaceMapSource.Terraformed(new BlockPos(5_000, 64, 0), 32));
        s.habitats.add(new NerospaceMapSource.Habitat(new BlockPos(-10, 88, 12), "nerospace:brinekin"));
        s.habitats.add(new NerospaceMapSource.Habitat(new BlockPos(700, 88, 0), "nerospace:brinekin"));
        s.visited = Set.of(new PlanetId(Identifier.fromNamespaceAndPath("nerospace", "luna")));
        return s;
    }

    static MapViewer at(UUID who, int x, int z) {
        return MapViewer.forTest(who, null, STATION, new BlockPos(x, 64, z));
    }

    static MapLayerConformance fixture(NerospaceMapLayers layers, UUID entitled) {
        return MapLayerConformance.create(layers)
                .viewers(at(entitled, 0, 0), at(STRANGER, 4, 0))
                .dimensions(STATION, ELSEWHERE)
                .bounds(MapBounds.around(0, 0, 512))
                .farBounds(MapBounds.around(20_000, 20_000, 256))
                .forbiddenText("Charlie")
                .forbiddenIds(THIRD);
    }

    @Test
    void ownerConforms() {
        fixture(new NerospaceMapLayers(seeded()), OWNER)
                .expectEntitledSees(NerospaceMapLayers.STATIONS, 1)
                .expectEntitledSees(NerospaceMapLayers.LAUNCH_PADS, 1)
                .expectEntitledSees(NerospaceMapLayers.PADS_NEARBY, 3)
                .expectEntitledSees(NerospaceMapLayers.ROCKETS, 1)
                .expectEntitledSees(NerospaceMapLayers.TERRAFORM_REGIONS, 1)
                .expectEntitledSees(NerospaceMapLayers.HABITATS, 1)
                .verify();
    }

    @Test
    void memberConforms() {
        fixture(new NerospaceMapLayers(seeded()), MEMBER)
                .expectEntitledSees(NerospaceMapLayers.STATIONS, 1)
                .expectEntitledSees(NerospaceMapLayers.LAUNCH_PADS, 1)
                .expectEntitledSees(NerospaceMapLayers.PADS_NEARBY, 3)
                .expectEntitledSees(NerospaceMapLayers.ROCKETS, 1)
                .expectEntitledSees(NerospaceMapLayers.TERRAFORM_REGIONS, 1)
                .expectEntitledSees(NerospaceMapLayers.HABITATS, 1)
                .verify();
    }

    @Test
    void proximityStopsAtSixtyFourBlocksAndNeverListsAnotherPlayersPrivatePad() {
        NerospaceMapLayers layers = new NerospaceMapLayers(seeded());
        za.co.neroland.nerolandcore.map.MapLayerRegistry.register(layers);
        var result = za.co.neroland.nerolandcore.map.MapLayerRegistry.query("nerospace", at(STRANGER, 0, 0),
                STATION, MapBounds.around(0, 0, 4_096));
        List<String> nearby = new ArrayList<>();
        result.markers().forEach(m -> {
            if (m.layer().equals(NerospaceMapLayers.PADS_NEARBY)) {
                nearby.add(m.id());
            }
        });
        assertEquals(Set.of("pad.3", "pad.4", "node.1"), Set.copyOf(nearby),
                "public and unowned pads in range only; never a private pad or one 64+ blocks away");
        assertTrue(result.markers().stream().noneMatch(m -> m.layer().equals(NerospaceMapLayers.STATIONS)));
        assertTrue(result.markers().stream().noneMatch(m -> m.layer().equals(NerospaceMapLayers.LAUNCH_PADS)));
        assertEquals(1, result.markers().stream().filter(m -> m.layer().equals(NerospaceMapLayers.ROCKETS)).count());
    }

    /** 2.4.0 F25: six layers, the five old ids and scopes unchanged, habitats PROXIMITY. */
    @Test
    void layerRosterIsAppendOnly() {
        NerospaceMapLayers layers = new NerospaceMapLayers(seeded());
        List<za.co.neroland.nerolandcore.map.MapLayerInfo> infos = layers.layers();
        assertEquals(6, infos.size());
        assertEquals(List.of(NerospaceMapLayers.STATIONS, NerospaceMapLayers.LAUNCH_PADS, NerospaceMapLayers.PADS_NEARBY,
                NerospaceMapLayers.ROCKETS, NerospaceMapLayers.TERRAFORM_REGIONS, NerospaceMapLayers.HABITATS),
                infos.stream().map(za.co.neroland.nerolandcore.map.MapLayerInfo::id).toList());
        List<za.co.neroland.nerolandcore.map.MapScope> scopes = infos.stream()
                .map(za.co.neroland.nerolandcore.map.MapLayerInfo::scope).toList();
        assertEquals(List.of(za.co.neroland.nerolandcore.map.MapScope.MEMBERS,
                za.co.neroland.nerolandcore.map.MapScope.MEMBERS, za.co.neroland.nerolandcore.map.MapScope.PROXIMITY,
                za.co.neroland.nerolandcore.map.MapScope.PROXIMITY, za.co.neroland.nerolandcore.map.MapScope.PROXIMITY,
                za.co.neroland.nerolandcore.map.MapScope.PROXIMITY), scopes);
        assertEquals("habitats", NerospaceMapLayers.HABITATS.getPath());
        assertEquals("brinekin", NerospaceMapLayers.cultureName("nerospace:brinekin"));
    }

    @Test
    void habitatsOnlyNearTheOnlineViewerInTheirDimension() {
        NerospaceMapLayers layers = new NerospaceMapLayers(seeded());
        za.co.neroland.nerolandcore.map.MapLayerRegistry.register(layers);
        var near = za.co.neroland.nerolandcore.map.MapLayerRegistry.query("nerospace", at(STRANGER, 0, 0),
                STATION, MapBounds.around(0, 0, 4_096));
        List<String> ids = near.markers().stream().filter(m -> m.layer().equals(NerospaceMapLayers.HABITATS))
                .map(m -> m.id()).toList();
        assertEquals(List.of("habitat.-10.88.12"), ids, "the core 700 blocks away is not drawn");
        var offline = za.co.neroland.nerolandcore.map.MapLayerRegistry.query("nerospace",
                MapViewer.forTest(STRANGER, null, null, null), STATION, MapBounds.around(0, 0, 4_096));
        assertTrue(offline.markers().stream().noneMatch(m -> m.layer().equals(NerospaceMapLayers.HABITATS)),
                "nothing for an offline viewer");
        var elsewhere = za.co.neroland.nerolandcore.map.MapLayerRegistry.query("nerospace",
                MapViewer.forTest(STRANGER, null, ELSEWHERE, new BlockPos(0, 64, 0)), STATION,
                MapBounds.around(0, 0, 4_096));
        assertTrue(elsewhere.markers().stream().noneMatch(m -> m.layer().equals(NerospaceMapLayers.HABITATS)),
                "nothing for a viewer in another dimension");
    }

    @Test
    void terraformIsAChunkAreaThatDoesNotPinpointTheMachine() {
        MapArea area = NerospaceMapLayers.terraformArea(STATION, new BlockPos(41, 64, 3), 48);
        MapArea.Chunks chunks = assertInstanceOf(MapArea.Chunks.class, area);
        assertTrue(chunks.size() > 1);
        assertEquals(0, area.bounds().minX() & 15, "areas snap to chunk edges");
        MapArea huge = NerospaceMapLayers.terraformArea(STATION, new BlockPos(41, 64, 3), 4_000);
        MapArea.Circle disc = assertInstanceOf(MapArea.Circle.class, huge);
        assertEquals(40, disc.centreX(), "a large reach is centred on the machine's chunk, not its block");
    }

    @Test
    void visitedWorldBadgesAreTheViewersOwn() {
        NerospaceMapLayers layers = new NerospaceMapLayers(seeded());
        List<MapDimensionBadge> mine = layers.dimensionBadges(MapViewer.forTest(OWNER, null, null, null));
        assertEquals(1, mine.size());
        assertEquals("nerospace", mine.get(0).id().getNamespace());
        assertEquals(new PlanetId(Identifier.fromNamespaceAndPath("nerospace", "luna")).dimension(),
                mine.get(0).dimension());
        assertTrue(layers.dimensionBadges(at(STRANGER, 0, 0)).isEmpty());
        assertFalse(mine.get(0).id().toString().contains(OWNER.toString()));
    }
}
