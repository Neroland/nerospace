package za.co.neroland.nerospace.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import za.co.neroland.nerolandcore.map.MapArea;
import za.co.neroland.nerolandcore.map.MapAreaStyle;
import za.co.neroland.nerolandcore.map.MapBounds;
import za.co.neroland.nerolandcore.map.MapDimensionBadge;
import za.co.neroland.nerolandcore.map.MapLabel;
import za.co.neroland.nerolandcore.map.MapLayerInfo;
import za.co.neroland.nerolandcore.map.MapLayerProvider;
import za.co.neroland.nerolandcore.map.MapLayerRegistry;
import za.co.neroland.nerolandcore.map.MapLayerSink;
import za.co.neroland.nerolandcore.map.MapMarker;
import za.co.neroland.nerolandcore.map.MapScope;
import za.co.neroland.nerolandcore.map.MapViewer;
import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.api.PlanetId;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.rocket.PadRegistry;
import za.co.neroland.nerospace.rocket.StationRegistry;
import za.co.neroland.nerospace.route.PadRecord;

/**
 * Nerospace's layers for Neroland Core's map API (NeroMaps and the companion app draw them; Nerospace draws
 * nothing). One scope per layer:
 * <ul>
 *   <li>{@code stations} (MEMBERS): stations the viewer owns or is a member of. Same rule as the link
 *       module's station section ({@link #stationVisibleTo}); an unowned or anonymised station is nobody's.</li>
 *   <li>{@code launch_pads} (MEMBERS): cargo pads the viewer owns or is on the access list of, anywhere.</li>
 *   <li>{@code pads_nearby} (PROXIMITY, 64): other players' public cargo pads and unowned pads/launch-pad
 *       nodes, only within 64 blocks of the online viewer's own position. No name and no owner on them: the
 *       public flag is permission to land, not consent to be advertised.</li>
 *   <li>{@code rockets} (PROXIMITY, 64): landed, empty rockets among the loaded entities near the viewer. A
 *       crewed, launching or descending rocket is a player position and is never drawn.</li>
 *   <li>{@code terraform_regions} (PROXIMITY, 64): the reach of a terraformer as a chunk area. The machine
 *       centre is never drawn as a marker.</li>
 * </ul>
 * Dimension badges: one per world the viewer has visited (their own visit record), for the star chart.
 *
 * <p>POPIA/GDPR: no marker id, label, detail or colour carries a player's name, UUID or online state. A
 * station's or pad's own name appears only to the viewer entitled to that station or pad.</p>
 */
public final class NerospaceMapLayers implements MapLayerProvider {

    public static final Identifier STATIONS = id("stations");
    public static final Identifier LAUNCH_PADS = id("launch_pads");
    public static final Identifier PADS_NEARBY = id("pads_nearby");
    public static final Identifier ROCKETS = id("rockets");
    public static final Identifier TERRAFORM_REGIONS = id("terraform_regions");

    /** Proximity radius for player-built things (rule G4). */
    static final int NEAR = MapLayerInfo.BUILT_PROXIMITY;

    private static final int TERRAFORM_FILL = 0x33009E73;
    private static final int TERRAFORM_BORDER = 0xCC009E73;

    private static final List<MapLayerInfo> LAYERS = List.of(
            MapLayerInfo.of(STATIONS, "map.layer.nerospace.stations", id("station"), MapScope.MEMBERS),
            MapLayerInfo.of(LAUNCH_PADS, "map.layer.nerospace.launch_pads", id("launch_pad"), MapScope.MEMBERS),
            MapLayerInfo.proximityBuilt(PADS_NEARBY, "map.layer.nerospace.pads_nearby", id("launch_pad")),
            MapLayerInfo.proximityBuilt(ROCKETS, "map.layer.nerospace.rockets", id("rocket")),
            MapLayerInfo.proximityBuilt(TERRAFORM_REGIONS, "map.layer.nerospace.terraform_regions",
                    id("terraform_region")));

    private final NerospaceMapSource source;

    public NerospaceMapLayers() {
        this(new ServerMapSource());
    }

    /** For tests: the same rules over a seeded source. */
    public NerospaceMapLayers(NerospaceMapSource source) {
        this.source = source;
    }

    /** Registers the provider with Core. A failure is logged and never takes the mod down. */
    public static void register() {
        try {
            MapLayerRegistry.register(new NerospaceMapLayers());
        } catch (RuntimeException e) {
            NerospaceCommon.LOGGER.warn("[Nerospace] map layers not registered: {}", e.getClass().getSimpleName());
        }
    }

    @Override
    public String modId() {
        return NerospaceCommon.MOD_ID;
    }

    @Override
    public List<MapLayerInfo> layers() {
        return LAYERS;
    }

    @Override
    public void query(MapViewer viewer, ResourceKey<Level> dimension, MapBounds bounds, MapLayerSink sink) {
        MinecraftServer server = viewer.server();
        String me = viewer.id().toString();
        stations(server, me, dimension, sink);
        ownPads(server, viewer.id(), dimension, sink);
        if (!viewer.online() || !viewer.dimension().map(dimension::equals).orElse(false)) {
            return; // every remaining layer is proximity: nothing for an offline viewer or another dimension
        }
        padsNearby(server, viewer, dimension, sink);
        rockets(server, viewer, dimension, sink);
        terraform(server, viewer, dimension, sink);
    }

    @Override
    public List<MapDimensionBadge> dimensionBadges(MapViewer viewer) {
        Set<PlanetId> visited = this.source.visited(viewer.server(), viewer.id());
        List<MapDimensionBadge> out = new ArrayList<>();
        Identifier icon = Identifier.fromNamespaceAndPath("neromaps", "star_chart");
        for (PlanetId planet : visited) {
            if (out.size() == MapDimensionBadge.MAX_PER_PROVIDER) {
                break;
            }
            out.add(new MapDimensionBadge(id("visited/" + planet.id().getPath()), planet.dimension(), icon,
                    MapLabel.translatable("map.badge.nerospace.visited")));
        }
        return out;
    }

    // --- MEMBERS layers -------------------------------------------------------------------------

    private void stations(@Nullable MinecraftServer server, String me, ResourceKey<Level> dimension,
            MapLayerSink sink) {
        if (!ModDimensions.STATION_LEVEL.equals(dimension)) {
            return;
        }
        for (StationRegistry.StationEntry entry : this.source.stations(server)) {
            if (sink.isFull(STATIONS)) {
                return;
            }
            if (!stationVisibleTo(entry, me)) {
                continue;
            }
            MapLabel name = entry.name() == null || entry.name().isBlank()
                    ? MapLabel.translatable("map.marker.nerospace.station", Integer.toString(entry.slot()))
                    : MapLabel.literal(entry.name());
            sink.marker(MapMarker.builder("station." + entry.slot(), STATIONS, dimension, entry.center())
                    .label(name)
                    .detail(MapLabel.translatable(me.equals(entry.owner())
                            ? "map.detail.nerospace.yours" : "map.detail.nerospace.member"))
                    .build());
        }
    }

    private void ownPads(@Nullable MinecraftServer server, UUID me, ResourceKey<Level> dimension,
            MapLayerSink sink) {
        String dimId = PadRegistry.dimId(dimension);
        for (PadRecord pad : this.source.routePads(server)) {
            if (sink.isFull(LAUNCH_PADS)) {
                return;
            }
            if (!dimId.equals(pad.dim()) || !padListed(pad, me)) {
                continue;
            }
            MapLabel name = pad.name().isBlank()
                    ? MapLabel.translatable("map.marker.nerospace.cargo_pad")
                    : MapLabel.literal(pad.name());
            MapMarker.Builder marker = MapMarker.builder("pad." + pad.id(), LAUNCH_PADS, dimension, pad.pos())
                    .label(name)
                    .detail(MapLabel.translatable(pad.ownedBy(me)
                            ? "map.detail.nerospace.yours" : "map.detail.nerospace.access"));
            if (pad.isPublic()) {
                marker.detail(MapLabel.translatable("map.detail.nerospace.public_pad"));
            }
            sink.marker(marker.build());
        }
    }

    // --- PROXIMITY layers -----------------------------------------------------------------------

    private void padsNearby(@Nullable MinecraftServer server, MapViewer viewer, ResourceKey<Level> dimension,
            MapLayerSink sink) {
        String dimId = PadRegistry.dimId(dimension);
        for (PadRecord pad : this.source.routePads(server)) {
            if (sink.isFull(PADS_NEARBY)) {
                return;
            }
            if (!dimId.equals(pad.dim()) || padListed(pad, viewer.id())
                    || !(pad.isPublic() || pad.owner().isEmpty())
                    || !viewer.within(dimension, pad.pos().getX(), pad.pos().getZ(), NEAR)) {
                continue;
            }
            sink.marker(MapMarker.builder("pad." + pad.id(), PADS_NEARBY, dimension, pad.pos())
                    .label(MapLabel.translatable("map.marker.nerospace.cargo_pad"))
                    .build());
        }
        for (PadRegistry.PadNode node : this.source.padNodes(server, dimension)) {
            if (sink.isFull(PADS_NEARBY)) {
                return;
            }
            BlockPos pos = node.centerOrPos();
            if (!dimId.equals(node.dim()) || !viewer.within(dimension, pos.getX(), pos.getZ(), NEAR)) {
                continue;
            }
            sink.marker(MapMarker.builder("node." + node.id(), PADS_NEARBY, dimension, pos)
                    .label(MapLabel.translatable("map.marker.nerospace.launch_pad"))
                    .build());
        }
    }

    private void rockets(@Nullable MinecraftServer server, MapViewer viewer, ResourceKey<Level> dimension,
            MapLayerSink sink) {
        BlockPos at = viewer.position().orElse(null);
        if (at == null) {
            return;
        }
        for (NerospaceMapSource.ParkedRocket rocket : this.source.parkedRockets(server, dimension, at, NEAR)) {
            if (sink.isFull(ROCKETS)) {
                return;
            }
            if (!viewer.within(dimension, rocket.pos().getX(), rocket.pos().getZ(), NEAR)) {
                continue;
            }
            sink.marker(MapMarker.builder("rocket." + rocket.entityId(), ROCKETS, dimension, rocket.pos())
                    .label(MapLabel.translatable("map.marker.nerospace.rocket_parked"))
                    .build());
        }
    }

    private void terraform(@Nullable MinecraftServer server, MapViewer viewer, ResourceKey<Level> dimension,
            MapLayerSink sink) {
        for (NerospaceMapSource.Terraformed reach : this.source.terraformed(server, dimension)) {
            if (sink.isFull(TERRAFORM_REGIONS)) {
                return;
            }
            int radius = Math.max(1, Math.min(MapArea.Circle.MAX_RADIUS, reach.radius()));
            BlockPos c = reach.centre();
            if (!viewer.within(dimension, MapBounds.around(c.getX(), c.getZ(), radius), NEAR)) {
                continue;
            }
            MapArea area = terraformArea(dimension, c, radius);
            if (viewer.within(dimension, area.bounds(), NEAR)) {
                sink.area(area);
            }
        }
    }

    /**
     * The terraformed reach as whole chunks (so the machine's own block is not pinpointed); a reach too large
     * for one chunk area falls back to a disc centred on the machine's chunk, not on the machine.
     */
    static MapArea terraformArea(ResourceKey<Level> dimension, BlockPos centre, int radius) {
        int ccx = centre.getX() >> 4;
        int ccz = centre.getZ() >> 4;
        String areaId = "terraform." + ccx + "." + ccz;
        MapAreaStyle style = MapAreaStyle.of(TERRAFORM_FILL, TERRAFORM_BORDER)
                .withLabel(MapLabel.translatable("map.area.nerospace.terraform_region"));
        int span = (radius >> 4) + 1;
        long r2 = (long) radius * radius;
        List<Long> chunks = new ArrayList<>();
        for (int dx = -span; dx <= span && chunks.size() <= MapArea.Chunks.MAX_CHUNKS; dx++) {
            for (int dz = -span; dz <= span; dz++) {
                long ex = ((long) (ccx + dx) << 4) + 8 - centre.getX();
                long ez = ((long) (ccz + dz) << 4) + 8 - centre.getZ();
                if (ex * ex + ez * ez <= r2) {
                    chunks.add(MapArea.Chunks.pack(ccx + dx, ccz + dz));
                }
            }
        }
        if (chunks.isEmpty()) {
            chunks.add(MapArea.Chunks.pack(ccx, ccz));
        }
        if (chunks.size() > MapArea.Chunks.MAX_CHUNKS) {
            return new MapArea.Circle(areaId, TERRAFORM_REGIONS, dimension, style, (ccx << 4) + 8, (ccz << 4) + 8,
                    radius);
        }
        long[] packed = new long[chunks.size()];
        for (int i = 0; i < packed.length; i++) {
            packed[i] = chunks.get(i);
        }
        return new MapArea.Chunks(areaId, TERRAFORM_REGIONS, dimension, style, packed);
    }

    // --- Visibility rules -----------------------------------------------------------------------

    /**
     * The station rule, re-implemented from the link module (kept in step by a parity test): an owned station
     * whose owner is {@code me} or whose member list holds {@code me}. Unowned or anonymised: nobody's.
     */
    public static boolean stationVisibleTo(StationRegistry.StationEntry entry, @Nullable String me) {
        return me != null && !me.isEmpty() && entry.isOwned()
                && (me.equals(entry.owner()) || entry.members().contains(me));
    }

    /** Whether {@code me} owns the pad or is on its access list (the public flag does not count here). */
    public static boolean padListed(PadRecord pad, @Nullable UUID me) {
        return me != null && (pad.ownedBy(me) || pad.access().contains(me.toString()));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, path);
    }
}
