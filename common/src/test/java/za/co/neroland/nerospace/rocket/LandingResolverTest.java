package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.rocket.LandingDecision.PadLanding;
import za.co.neroland.nerospace.rocket.LandingDecision.PodLanding;
import za.co.neroland.nerospace.rocket.LandingDecision.StationDock;

/** The pure landing resolver, with hand-built contexts (no server). */
class LandingResolverTest {

    /** Every column is a perfect pod site at y 64. */
    static final PodSearch.Sampler CLEAR = new PodSearch.Sampler() {
        @Override
        public SiteSample column(int x, int z) {
            return SiteSample.clear(new BlockPos(x, 64, z));
        }

        @Override
        public SiteSample at(BlockPos site) {
            return SiteSample.clear(site);
        }
    };

    static PadCandidate pad(int id, String name, int x, int z) {
        return new PadCandidate(id, name, PadKind.LAUNCH_PAD, new BlockPos(x, 64, z), new BlockPos(x, 64, z), null,
                true, false);
    }

    static LandingContext ctx(int explicit, List<PadCandidate> pads, Optional<ReturnSite> ret, Optional<String> origin,
            boolean originRegistered) {
        return new LandingContext("nerospace:luna", explicit, pads, ret, origin, originRegistered, BlockPos.ZERO,
                Optional.empty(), CLEAR, Set.of());
    }

    @Test
    @DisplayName("step 1: an explicit pad wins over return, paired and nearest")
    void explicitPadWins() {
        List<PadCandidate> pads = List.of(pad(0, "Home", 10, 10), pad(1, "B", 20, 20), pad(2, "C", 500, 500));
        ReturnSite ret = new ReturnSite(new BlockPos(10, 64, 10), true, false, new BlockPos(10, 64, 10), null,
                Optional.empty());
        LandingDecision d = LandingResolver.resolve(ctx(2, pads, Optional.of(ret), Optional.of("Home"), true));
        PadLanding p = assertInstanceOf(PadLanding.class, d);
        assertEquals(1, p.step());
        assertEquals(2, p.pad().id());
    }

    @Test
    @DisplayName("step 1 falls through when the chosen pad is gone, flagging pad_missing")
    void explicitPadMissingFallsThrough() {
        PadCandidate gone = new PadCandidate(2, "C", PadKind.LAUNCH_PAD, new BlockPos(5, 64, 5), new BlockPos(5, 64, 5),
                null, false, false);
        List<PadCandidate> pads = List.of(pad(0, "A", 10, 10), pad(1, "B", 20, 20), gone);
        ReturnSite ret = new ReturnSite(new BlockPos(30, 64, 30), true, false, new BlockPos(31, 64, 31), null,
                Optional.empty());
        LandingDecision d = LandingResolver.resolve(ctx(2, pads, Optional.of(ret), Optional.empty(), false));
        PadLanding p = assertInstanceOf(PadLanding.class, d);
        assertEquals(2, p.step());
        assertTrue(p.flags().padMissing());
    }

    @Test
    @DisplayName("step 2: an unregistered standing return pad beats paired and nearest; spawn is the cluster centre")
    void returnPadBeatsPairedAndNearest() {
        List<PadCandidate> pads = List.of(pad(0, "Home", 1, 1));
        BlockPos corner = new BlockPos(100, 70, 100);
        BlockPos centre = new BlockPos(101, 70, 101);
        ReturnSite ret = new ReturnSite(corner, true, false, centre, null, Optional.empty());
        LandingDecision d = LandingResolver.resolve(ctx(-1, pads, Optional.of(ret), Optional.of("Home"), true));
        PadLanding p = assertInstanceOf(PadLanding.class, d);
        assertEquals(2, p.step());
        assertEquals(new Vec3(101.5D, 70.0D + 3.0D / 16.0D, 101.5D), p.spawn());
    }

    @Test
    @DisplayName("step 3: the paired pad is matched by exact, case-sensitive name")
    void pairedPadByExactName() {
        List<PadCandidate> pads = List.of(pad(3, "home", 0, 0), pad(7, "Home", 40, 40));
        LandingDecision d = LandingResolver.resolve(ctx(-1, pads, Optional.empty(), Optional.of("Home"), true));
        PadLanding p = assertInstanceOf(PadLanding.class, d);
        assertEquals(3, p.step());
        assertEquals(7, p.pad().id());
    }

    @Test
    @DisplayName("step 3: among duplicate names within 256 the oldest id wins")
    void pairedPadOldestIdWins() {
        List<PadCandidate> pads = List.of(pad(9, "Home", 10, 10), pad(4, "Home", 100, 100));
        LandingDecision d = LandingResolver.resolve(ctx(-1, pads, Optional.empty(), Optional.of("Home"), true));
        PadLanding p = assertInstanceOf(PadLanding.class, d);
        assertEquals(3, p.step());
        assertEquals(4, p.pad().id());
    }

    @Test
    @DisplayName("step 3 is skipped when duplicate names are all far from preferred (ambiguous) → step 4")
    void pairedPadAmbiguousFar() {
        // originRegistered → preferred is the usable node nearest the anchor: the unnamed one at the origin.
        List<PadCandidate> pads = List.of(pad(1, "Home", 1000, 0), pad(2, "Home", -1000, 0), pad(3, "Base", 5, 5));
        LandingDecision d = LandingResolver.resolve(ctx(-1, pads, Optional.empty(), Optional.of("Home"), true));
        PadLanding p = assertInstanceOf(PadLanding.class, d);
        assertEquals(4, p.step());
        assertEquals(3, p.pad().id());
    }

    @Test
    @DisplayName("auto-labels never pair")
    void autoLabelNeverPairs() {
        List<PadCandidate> pads = List.of(pad(0, "Base", 3, 3), pad(5, "Pad 3", 900, 900));
        LandingDecision d = LandingResolver.resolve(ctx(-1, pads, Optional.empty(), Optional.of("Pad 3"), true));
        PadLanding p = assertInstanceOf(PadLanding.class, d);
        assertEquals(4, p.step());
        assertEquals(0, p.pad().id());
        assertTrue(LandingResolver.isAutoLabel("Pad 12"));
        assertFalse(LandingResolver.isAutoLabel("Pad Twelve"));
    }

    @Test
    @DisplayName("step 4 is bounded at 2048 blocks: a lone far pad yields a Landing Pod")
    void nearestBounded2048() {
        LandingDecision d = LandingResolver.resolve(ctx(-1, List.of(pad(0, "Far", 3000, 0)), Optional.empty(),
                Optional.empty(), false));
        assertInstanceOf(PodLanding.class, d);
    }

    @Test
    @DisplayName("step 4 tie: equidistant nodes → lowest id")
    void nearestTieLowestId() {
        List<PadCandidate> pads = List.of(pad(8, "A", 10, 0), pad(2, "B", -10, 0));
        PadLanding p = assertInstanceOf(PadLanding.class,
                LandingResolver.resolve(ctx(-1, pads, Optional.empty(), Optional.empty(), false)));
        assertEquals(4, p.step());
        assertEquals(2, p.pad().id());
    }

    @Test
    @DisplayName("an occupied pad is skipped; all occupied → pod")
    void occupiedPadSkipped() {
        PadCandidate busy = new PadCandidate(1, "A", PadKind.LAUNCH_PAD, new BlockPos(1, 64, 1), new BlockPos(1, 64, 1),
                null, true, true);
        List<PadCandidate> pads = List.of(busy, pad(2, "B", 50, 50));
        PadLanding p = assertInstanceOf(PadLanding.class,
                LandingResolver.resolve(ctx(-1, pads, Optional.empty(), Optional.empty(), false)));
        assertEquals(2, p.pad().id());
        assertTrue(p.flags().padOccupied());
        LandingDecision all = LandingResolver.resolve(ctx(-1, List.of(busy), Optional.empty(), Optional.empty(), false));
        assertInstanceOf(PodLanding.class, all);
    }

    @Test
    @DisplayName("POD nodes are never picked by steps 1–4")
    void podNodesIgnoredBySteps1to4() {
        PadCandidate pod = new PadCandidate(1, "Landing Pod 2", PadKind.POD, new BlockPos(0, 64, 0),
                new BlockPos(0, 64, 0), null, true, false);
        LandingDecision d = LandingResolver.resolve(ctx(0, List.of(pod), Optional.empty(), Optional.empty(), false));
        assertInstanceOf(PodLanding.class, d);
    }

    @Test
    @DisplayName("station: a Docking Ring beats the legacy pad")
    void stationPrefersDock() {
        PadCandidate dock = new PadCandidate(4, "S Dock", PadKind.DOCK, new BlockPos(9, 64, 0), new BlockPos(9, 64, 0),
                Direction.EAST, true, false);
        PadCandidate legacy = pad(1, "S Landing", 9, 0);
        StationTarget st = new StationTarget(0, new BlockPos(0, 64, 0), new BlockPos(9, 64, 0), Optional.of(dock),
                Optional.of(legacy));
        LandingContext c = new LandingContext("nerospace:station", -1, List.of(), Optional.empty(), Optional.empty(),
                false, BlockPos.ZERO, Optional.of(st), CLEAR, Set.of());
        StationDock d = assertInstanceOf(StationDock.class, LandingResolver.resolve(c));
        assertTrue(d.viaDockingRing());
        assertEquals(-90.0F, d.yaw());
    }

    @Test
    @DisplayName("station: no dock → legacy pad; neither → a pod at the pad centre")
    void stationLegacyPadFallback() {
        PadCandidate legacy = pad(1, "S Landing", 9, 0);
        StationTarget st = new StationTarget(0, new BlockPos(0, 64, 0), new BlockPos(9, 64, 0), Optional.empty(),
                Optional.of(legacy));
        LandingContext c = new LandingContext("nerospace:station", -1, List.of(), Optional.empty(), Optional.empty(),
                false, BlockPos.ZERO, Optional.of(st), CLEAR, Set.of());
        StationDock d = assertInstanceOf(StationDock.class, LandingResolver.resolve(c));
        assertFalse(d.viaDockingRing());
        StationTarget none = new StationTarget(0, new BlockPos(0, 64, 0), new BlockPos(9, 64, 0), Optional.empty(),
                Optional.empty());
        LandingContext c2 = new LandingContext("nerospace:station", -1, List.of(), Optional.empty(), Optional.empty(),
                false, BlockPos.ZERO, Optional.of(none), CLEAR, Set.of());
        PodLanding pod = assertInstanceOf(PodLanding.class, LandingResolver.resolve(c2));
        assertEquals(9, pod.site().getX());
    }

    @Test
    @DisplayName("yaw from facing: EAST −90, WEST 90, NORTH 180, none 0")
    void yawFromFacing() {
        assertEquals(-90.0F, LandingResolver.yawOf(Direction.EAST));
        assertEquals(90.0F, LandingResolver.yawOf(Direction.WEST));
        assertEquals(180.0F, LandingResolver.yawOf(Direction.NORTH));
        assertEquals(0.0F, LandingResolver.yawOf(Direction.SOUTH));
        assertEquals(0.0F, LandingResolver.yawOf(null));
    }

    @Test
    @DisplayName("a 1.6.0 rocket saved mid-flight (no return, no origin, no nodes) → pod at the arrival anchor")
    void savedMidFlight16Rocket() {
        LandingContext c = new LandingContext("nerospace:cindara", -1, List.of(), Optional.empty(), Optional.empty(),
                false, new BlockPos(120, 0, -40), Optional.empty(), CLEAR, Set.of());
        PodLanding pod = assertInstanceOf(PodLanding.class, LandingResolver.resolve(c));
        assertEquals(new BlockPos(120, 64, -40), pod.site());
        assertEquals(0, pod.ringReached());
        // The pod is a full cube: the rocket stands on its top face, centred.
        assertEquals(new Vec3(120.5D, 65.0D, -39.5D), pod.spawn());
        assertEquals(0.0F, pod.yaw());
    }

    @Test
    @DisplayName("pod spawn stands on the pod's top face; pads and docks use the 3/16 plate")
    void podSpawnOnTopFace() {
        BlockPos at = new BlockPos(-7, 80, 12);
        assertEquals(new Vec3(-6.5D, 81.0D, 12.5D), LandingResolver.podSpawnOf(at));
        assertEquals(new Vec3(-6.5D, 80.0D + 3.0D / 16.0D, 12.5D), LandingResolver.spawnOf(at));
        StationTarget none = new StationTarget(0, new BlockPos(0, 64, 0), new BlockPos(9, 64, 0), Optional.empty(),
                Optional.empty());
        LandingContext c = new LandingContext("nerospace:station", -1, List.of(), Optional.empty(), Optional.empty(),
                false, BlockPos.ZERO, Optional.of(none), CLEAR, Set.of());
        PodLanding pod = assertInstanceOf(PodLanding.class, LandingResolver.resolve(c));
        assertEquals(new Vec3(9.5D, 65.0D, 0.5D), pod.spawn());
    }

    @Test
    @DisplayName("a broken target is excluded on re-resolve and flagged pad_missing")
    void excludedNodeReResolves() {
        List<PadCandidate> pads = List.of(pad(1, "A", 0, 0), pad(2, "B", 30, 30));
        LandingContext c = ctx(0, pads, Optional.empty(), Optional.empty(), false).excluding(1);
        PadLanding p = assertInstanceOf(PadLanding.class, LandingResolver.resolve(c));
        assertEquals(2, p.pad().id());
        assertTrue(p.flags().padMissing());
    }
}
