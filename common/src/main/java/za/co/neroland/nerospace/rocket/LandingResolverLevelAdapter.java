package za.co.neroland.nerospace.rocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.api.PlanetArchetype;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.route.PadRecord;
import za.co.neroland.nerospace.route.RouteRegistry;
import za.co.neroland.nerospace.world.TerraformOverlayState;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;
import za.co.neroland.nerospace.world.station.StationAccess;

/**
 * The level side of {@link LandingResolver} (005 §1.1): builds the {@link LandingContext} snapshot from the
 * live world (the only place that loads chunks for a landing), back-fills legacy pad centres once, and applies
 * a decision (places the Landing Pod and registers its {@code POD} node). Exercised by the runtime gate.
 */
public final class LandingResolverLevelAdapter {

    /** What the rocket knows about its flight. {@code rider} may be null (uncrewed / disconnected). */
    public record Flight(ResourceKey<Level> destination, int explicitPad, String returnDim, @Nullable BlockPos returnPos,
            Optional<String> originPadName, boolean originRegistered, @Nullable ServerPlayer rider,
            int stationSlot, @Nullable BlockPos stationCentre) {
    }

    /** The node the rocket launched from, if it was a registered one. */
    public record Origin(boolean registered, Optional<String> name) {
        public static final Origin NONE = new Origin(false, Optional.empty());
    }

    private LandingResolverLevelAdapter() {
    }

    // --- resolve ----------------------------------------------------------------------------------

    /** Gathers the context and resolves (station structure ensured first; 005 §1.8). */
    public static LandingDecision resolve(ServerLevel level, Flight flight, Set<Integer> excluded) {
        if (isStation(flight.destination()) && flight.stationCentre() != null) {
            ensureStation(level, flight.stationSlot(), flight.stationCentre());
        }
        return LandingResolver.resolve(gather(level, flight, excluded));
    }

    public static boolean isStation(@Nullable ResourceKey<Level> dim) {
        PlanetDefinition planet = Planets.get(dim);
        return ModDimensions.STATION_LEVEL.equals(dim)
                || (planet != null && planet.traits().archetype() == PlanetArchetype.ORBITAL);
    }

    /**
     * Makes sure the selected station exists. A founded station goes through {@link StationStructure#build}
     * (guarded by its Station Core); the shared origin platform is only built when it is genuinely empty
     * (001 §13.1, {@link OriginPlatformGuard}) — otherwise only its pad node is kept registered.
     */
    public static void ensureStation(ServerLevel level, int slot, BlockPos centre) {
        if (slot >= 0) {
            StationStructure.build(level, centre);
            return;
        }
        BlockPos padCentre = StationStructure.padCenter(centre);
        level.getChunk(centre.getX() >> 4, centre.getZ() >> 4);
        level.getChunk(padCentre.getX() >> 4, padCentre.getZ() >> 4);
        PadRegistry pads = PadRegistry.get(level.getServer());
        boolean hasCore = level.getBlockState(centre).is(ModBlocks.STATION_CORE.get());
        boolean hasNode = pads.at(level.dimension(), padCentre) != null;
        boolean floor = level.getBlockState(centre).is(ModBlocks.STATION_FLOOR.get());
        if (!OriginPlatformGuard.alreadyBuilt(hasCore, hasNode, floor)) {
            StationStructure.build(level, centre);
            return;
        }
        if (!hasNode && level.getBlockState(padCentre).getBlock() instanceof RocketLaunchPadBlock) {
            pads.register("Station Landing", level.dimension(), padCentre);
        }
    }

    /** Builds the resolver's snapshot. */
    public static LandingContext gather(ServerLevel level, Flight flight, Set<Integer> excluded) {
        PadRegistry registry = PadRegistry.get(level.getServer());
        ResourceKey<Level> dest = flight.destination();
        String destId = PadRegistry.dimId(dest);

        Optional<StationTarget> station = Optional.empty();
        List<PadCandidate> pads = new ArrayList<>();
        if (isStation(dest) && flight.stationCentre() != null) {
            BlockPos centre = flight.stationCentre();
            PadCandidate dock = null;
            PadCandidate legacy = null;
            double dockSq = Double.MAX_VALUE;
            double padSq = Double.MAX_VALUE;
            for (PadRegistry.PadNode node : registry.nodesIn(dest)) {
                if (LandingResolver.chebyshev(node.centerOrPos(), centre) > StationTarget.CLAIM_RADIUS) {
                    continue;
                }
                double sq = node.centerOrPos().distSqr(centre);
                if (node.kind() == PadKind.DOCK && sq < dockSq) {
                    dockSq = sq;
                    dock = candidate(level, registry, node);
                } else if (node.kind() == PadKind.LAUNCH_PAD && sq < padSq) {
                    padSq = sq;
                    legacy = candidate(level, registry, node);
                }
            }
            station = Optional.of(new StationTarget(flight.stationSlot(), centre, StationStructure.padCenter(centre),
                    Optional.ofNullable(dock), Optional.ofNullable(legacy)));
        } else {
            for (PadRegistry.PadNode node : registry.inDimension(dest)) {
                pads.add(candidate(level, registry, node));
            }
        }

        Optional<ReturnSite> returnSite = Optional.empty();
        BlockPos rp = flight.returnPos();
        if (station.isEmpty() && rp != null && destId.equals(flight.returnDim())) {
            returnSite = Optional.of(returnSite(level, registry, rp));
        }

        PlanetDefinition planet = Planets.get(dest);
        BlockPos anchor = planet != null ? planet.arrivalAnchor() : BlockPos.ZERO;
        int voidFloorY = station.isPresent() ? StationRegistry.PLATFORM_Y : level.getMinY();
        return new LandingContext(destId, flight.explicitPad(), pads, returnSite, flight.originPadName(),
                flight.originRegistered(), anchor, station, new LevelSampler(level, flight.rider(), voidFloorY),
                excluded);
    }

    // --- candidates -------------------------------------------------------------------------------

    private static PadCandidate candidate(ServerLevel level, PadRegistry registry, PadRegistry.PadNode node) {
        BlockPos pos = node.pos();
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        return switch (node.kind()) {
            case DOCK -> {
                BlockPos centre = node.centerOrPos();
                BlockState s = level.getBlockState(centre);
                boolean present = s.getBlock() instanceof DockingRingBlock
                        && s.getValue(DockingRingBlock.PART) == DockingRingPart.CENTER;
                Direction facing = node.facing().orElse(present ? s.getValue(DockingRingBlock.FACING) : null);
                if (present && (node.center().isEmpty() || node.facing().isEmpty())) {
                    registry.fill(node.id(), centre, facing);
                }
                yield new PadCandidate(node.id(), node.name(), PadKind.DOCK, pos, centre, facing, present,
                        present && rocketOn(level, centre, 1));
            }
            case POD -> {
                boolean present = level.getBlockState(pos).is(ModBlocks.LANDING_POD.get());
                yield new PadCandidate(node.id(), node.name(), PadKind.POD, pos, pos, null, present,
                        present && rocketOn(level, pos, 0));
            }
            default -> {
                boolean present = level.getBlockState(pos).getBlock() instanceof RocketLaunchPadBlock;
                Set<BlockPos> cluster = present ? LaunchPadMultiblock.connectedPads(level, pos) : Set.of();
                BlockPos centre = node.center().orElse(present ? LandingPadGeometry.centreOf(cluster, pos) : pos);
                Direction facing = node.facing().orElse(present ? apronFacing(level, cluster, centre) : null);
                if (present && (node.center().isEmpty() || node.facing().isEmpty())) {
                    registry.fill(node.id(), centre, facing); // 005 §1.6 step 4: written back once
                }
                boolean standing = level.getBlockState(centre).getBlock() instanceof RocketLaunchPadBlock;
                yield new PadCandidate(node.id(), node.name(), PadKind.LAUNCH_PAD, pos, centre, facing, standing,
                        standing && LaunchPadMultiblock.rocketAbove(level, cluster.isEmpty()
                                ? LaunchPadMultiblock.connectedPads(level, centre) : cluster) != null);
            }
        };
    }

    private static ReturnSite returnSite(ServerLevel level, PadRegistry registry, BlockPos pos) {
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        boolean standing = level.getBlockState(pos).getBlock() instanceof RocketLaunchPadBlock;
        if (!standing) {
            return new ReturnSite(pos, false, false, pos, null, Optional.empty());
        }
        Set<BlockPos> cluster = LaunchPadMultiblock.connectedPads(level, pos);
        BlockPos centre = LandingPadGeometry.centreOf(cluster, pos);
        Direction facing = apronFacing(level, cluster, centre);
        Optional<PadCandidate> registered = Optional.empty();
        for (PadRegistry.PadNode node : registry.inDimension(level.dimension())) {
            if (node.kind() == PadKind.LAUNCH_PAD && cluster.contains(node.pos())) {
                registered = Optional.of(candidate(level, registry, node));
                break;
            }
        }
        boolean occupied = LaunchPadMultiblock.rocketAbove(level, cluster) != null;
        return new ReturnSite(pos, true, occupied, centre, facing, registered);
    }

    /** 005 §1.6 step 3: the side whose first cell beyond the square is air while the opposite one is not. */
    public static Direction apronFacing(Level level, Set<BlockPos> cluster, BlockPos centre) {
        int half = LandingPadGeometry.halfSize(cluster, centre);
        boolean[] open = new boolean[4];
        for (int i = 0; i < 4; i++) {
            open[i] = level.getBlockState(centre.relative(LandingPadGeometry.ORDER[i], half + 1)).isAir();
        }
        return LandingPadGeometry.facingOf(open);
    }

    private static boolean rocketOn(ServerLevel level, BlockPos block, int halfWidth) {
        AABB box = new AABB(block.getX() - halfWidth, block.getY() + 0.05D, block.getZ() - halfWidth,
                block.getX() + 1 + halfWidth, block.getY() + 4.0D, block.getZ() + 1 + halfWidth);
        for (RocketEntity r : level.getEntitiesOfClass(RocketEntity.class, box)) {
            if (!r.isLaunching() && !r.isDescending()) {
                return true;
            }
        }
        return false;
    }

    // --- the rocket's origin node -----------------------------------------------------------------

    /**
     * The registered node the rocket stands on in {@code level} at launch (any node whose pad cluster contains
     * {@code padPos}, or a dock/pod within one block of the rocket). Auto-labels and blank names never pair.
     */
    public static Origin origin(ServerLevel level, @Nullable BlockPos padPos, BlockPos rocketPos) {
        PadRegistry registry = PadRegistry.get(level.getServer());
        Set<BlockPos> cluster = padPos != null && level.getBlockState(padPos).getBlock() instanceof RocketLaunchPadBlock
                ? LaunchPadMultiblock.connectedPads(level, padPos) : Set.of();
        for (PadRegistry.PadNode node : registry.inDimension(level.dimension())) {
            boolean match = cluster.contains(node.pos())
                    || LandingResolver.chebyshev(node.centerOrPos(), rocketPos) <= 1
                            && Math.abs(node.centerOrPos().getY() - rocketPos.getY()) <= 1;
            if (match) {
                String name = node.name() == null ? "" : node.name().strip();
                return new Origin(true, LandingResolver.isAutoLabel(name) ? Optional.empty() : Optional.of(name));
            }
        }
        return Origin.NONE;
    }

    // --- apply ------------------------------------------------------------------------------------

    /**
     * Places the decision's Landing Pod (if new; always empty — the arriving rocket stands on it) and registers
     * its {@code POD} node. Pads/docks need nothing.
     */
    public static void prepare(ServerLevel level, LandingDecision decision) {
        if (decision instanceof LandingDecision.PodLanding pod) {
            if (!pod.existingPodReused()) {
                ReturnSitePlacement.placePod(level, pod.site(), pod.climbUsed() > 0);
            }
            PadRegistry.get(level.getServer()).register(null, level.dimension(), pod.site(), PadKind.POD,
                    pod.site(), null);
        }
    }

    /** The block the rocket stands on: the pad/dock centre, or the pod itself. */
    public static BlockPos standBlock(LandingDecision decision) {
        if (decision instanceof LandingDecision.PodLanding pod) {
            return pod.site();
        }
        if (decision instanceof LandingDecision.StationDock dock) {
            return dock.target().center();
        }
        return ((LandingDecision.PadLanding) decision).pad().center();
    }

    /** The kind of block the rocket lands on, for the descent's pad-still-there check. */
    public static PadKind targetKind(LandingDecision decision) {
        if (decision instanceof LandingDecision.PodLanding) {
            return PadKind.POD;
        }
        if (decision instanceof LandingDecision.StationDock dock) {
            return dock.viaDockingRing() ? PadKind.DOCK : PadKind.LAUNCH_PAD;
        }
        return ((LandingDecision.PadLanding) decision).pad().kind();
    }

    /** Whether the block the rocket is descending onto still stands (005 §3.3). */
    public static boolean targetPresent(ServerLevel level, PadKind kind, BlockPos block) {
        BlockState s = level.getBlockState(block);
        return switch (kind) {
            case DOCK -> s.getBlock() instanceof DockingRingBlock;
            case POD -> s.is(ModBlocks.LANDING_POD.get());
            default -> s.getBlock() instanceof RocketLaunchPadBlock;
        };
    }

    // --- pod-site sampler -------------------------------------------------------------------------

    /** Samples candidate pod sites from the live level, caching per column (005 §1.7, G5). */
    private static final class LevelSampler implements PodSearch.Sampler {

        private final ServerLevel level;
        private final int voidFloorY;
        private final Map<Long, SiteSample> columns = new HashMap<>();
        private final List<int[]> inaccessible = new ArrayList<>(); // x, z, halfBox
        private final List<BlockPos> padCentres = new ArrayList<>();
        private final List<BlockPos> podNodes = new ArrayList<>();

        LevelSampler(ServerLevel level, @Nullable ServerPlayer rider, int voidFloorY) {
            this.level = level;
            this.voidFloorY = voidFloorY;
            String riderId = rider != null ? rider.getUUID().toString() : "";
            boolean op = rider != null && StationRegistry.isOp(rider);
            if (isStation(level.dimension())) {
                for (StationRegistry.StationEntry e : StationRegistry.get(level.getServer()).all()) {
                    if (!StationAccess.canUse(e.owner(), e.members(), riderId, op)) {
                        this.inaccessible.add(new int[] { e.center().getX(), e.center().getZ(), StationTarget.CLAIM_RADIUS });
                    }
                }
            }
            String dimId = PadRegistry.dimId(level.dimension());
            for (PadRecord pad : RouteRegistry.get(level.getServer()).allPads()) {
                if (!pad.dim().equals(dimId)) {
                    continue;
                }
                boolean access = pad.owner().isEmpty() || pad.isPublic() || pad.owner().equals(riderId)
                        || pad.access().contains(riderId);
                if (!access) {
                    this.inaccessible.add(new int[] { pad.pos().getX(), pad.pos().getZ(), 8 });
                }
            }
            for (PadRegistry.PadNode node : PadRegistry.get(level.getServer()).nodesIn(level.dimension())) {
                if (node.kind() == PadKind.POD) {
                    this.podNodes.add(node.pos());
                } else {
                    this.padCentres.add(node.centerOrPos());
                }
            }
        }

        @Override
        public SiteSample column(int x, int z) {
            return this.columns.computeIfAbsent(BlockPos.asLong(x, 0, z), k -> sampleColumn(x, z));
        }

        private SiteSample sampleColumn(int x, int z) {
            this.level.getChunk(x >> 4, z >> 4);
            int y = this.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y <= this.level.getMinY()) {
                y = this.voidFloorY; // a void column (station): the climb builds the floor
            }
            BlockPos site = new BlockPos(x, y, z);
            BlockPos below = site.below();
            if (this.level.getBlockState(below).is(ModBlocks.LANDING_POD.get())) {
                boolean empty = this.level.getBlockEntity(below) instanceof ReturnSiteBlockEntity be && be.isEmpty();
                if (empty && !rocketOn(this.level, below, 0)) {
                    return new SiteSample(below, SiteSample.fill(SiteSample.FOOTPRINT, true),
                            SiteSample.fill(SiteSample.FOOTPRINT, false), SiteSample.fill(SiteSample.BOX, true),
                            false, Integer.MAX_VALUE, false, false, true);
                }
            }
            return at(site);
        }

        @Override
        public SiteSample at(BlockPos site) {
            boolean[] solid = new boolean[SiteSample.FOOTPRINT];
            boolean[] forbidden = new boolean[SiteSample.FOOTPRINT];
            boolean[] clear = new boolean[SiteSample.BOX];
            int i = 0;
            for (int dz = -2; dz <= 2; dz++) {
                for (int dx = -2; dx <= 2; dx++) {
                    BlockPos f = site.offset(dx, -1, dz);
                    BlockState s = this.level.getBlockState(f);
                    solid[i] = s.isCollisionShapeFullBlock(this.level, f);
                    forbidden[i] = forbiddenFloor(f, s);
                    for (int h = 0; h < 3; h++) {
                        BlockPos c = site.offset(dx, h, dz);
                        BlockState cs = this.level.getBlockState(c);
                        clear[h * SiteSample.FOOTPRINT + i] = (cs.isAir() || cs.canBeReplaced())
                                && cs.getFluidState().isEmpty() && this.level.getBlockEntity(c) == null;
                    }
                    i++;
                }
            }
            return new SiteSample(site, solid, forbidden, clear, inStructure(site), claimDistance(site),
                    overlapsPad(site), podNearby(site), false);
        }

        /** Whether any structure start's bounding box covers the 5×5×3 box (its centre or a corner). */
        private boolean inStructure(BlockPos site) {
            var manager = this.level.structureManager();
            BlockPos[] probes = { site, site.offset(2, 0, 2), site.offset(-2, 0, -2), site.offset(2, 2, -2),
                    site.offset(-2, 2, 2) };
            for (BlockPos p : probes) {
                for (var structure : manager.getAllStructuresAt(p).keySet()) {
                    if (manager.getStructureAt(p, structure).isValid()) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean forbiddenFloor(BlockPos pos, BlockState s) {
            return s.is(BlockTags.LEAVES) || !s.getFluidState().isEmpty() || s.is(BlockTags.ICE)
                    || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.MAGMA_BLOCK) || this.level.getBlockEntity(pos) != null;
        }

        private int claimDistance(BlockPos site) {
            int best = Integer.MAX_VALUE;
            for (int[] c : this.inaccessible) {
                int d = Math.max(Math.abs(site.getX() - c[0]), Math.abs(site.getZ() - c[1])) - c[2];
                best = Math.min(best, Math.max(0, d));
            }
            // Terraformed ground: Nerospace's default policy treats every overlay region as inaccessible.
            TerraformOverlayState overlay = TerraformOverlayState.get(this.level);
            int r = PodSiteRules.CLAIM_DISTANCE - 1;
            if (overlay.at(site).isPresent() || overlay.at(site.offset(r, 0, r)).isPresent()
                    || overlay.at(site.offset(-r, 0, r)).isPresent() || overlay.at(site.offset(r, 0, -r)).isPresent()
                    || overlay.at(site.offset(-r, 0, -r)).isPresent()) {
                best = 0;
            }
            return best;
        }

        private boolean overlapsPad(BlockPos site) {
            for (BlockPos c : this.padCentres) {
                if (LandingResolver.chebyshev(c, site) <= 4 && Math.abs(c.getY() - site.getY()) <= 3) {
                    return true;
                }
            }
            return false;
        }

        private boolean podNearby(BlockPos site) {
            for (BlockPos p : this.podNodes) {
                if (!p.equals(site) && LandingResolver.chebyshev(p, site) < PodSiteRules.POD_SPACING
                        && this.level.getBlockState(p).is(ModBlocks.LANDING_POD.get())) {
                    return true;
                }
            }
            return false;
        }
    }
}
