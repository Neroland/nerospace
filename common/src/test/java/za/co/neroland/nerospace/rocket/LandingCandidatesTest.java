package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

/**
 * A landing probes registered nodes lazily and stays inside its chunk budget. The probe is a fake
 * level adapter that counts every node read and every distinct chunk it would load.
 */
class LandingCandidatesTest {

    /** The fake level: the truth for every node, plus a tally of what the planner read. */
    static final class FakeLevel implements LandingCandidates.Probe {
        final List<PadCandidate> truth;
        final Set<Long> chunks = new HashSet<>();
        int probes;

        FakeLevel(List<PadCandidate> truth) {
            this.truth = truth;
        }

        @Override
        public PadCandidate probe(int listIndex) {
            PadCandidate c = this.truth.get(listIndex);
            this.probes++;
            this.chunks.add(LandingChunkBudget.chunkKey(c.pos().getX() >> 4, c.pos().getZ() >> 4));
            return c;
        }
    }

    static PadCandidate node(int id, String name, int x, int z, boolean standing, boolean occupied) {
        BlockPos p = new BlockPos(x, 64, z);
        return new PadCandidate(id, name, PadKind.LAUNCH_PAD, p, p, null, standing, occupied);
    }

    static List<LandingCandidates.Node> nodes(List<PadCandidate> truth) {
        List<LandingCandidates.Node> out = new ArrayList<>();
        for (PadCandidate c : truth) {
            out.add(new LandingCandidates.Node(c.id(), c.name(), c.kind(), c.pos(), c.center()));
        }
        return out;
    }

    static LandingCandidates.Flight flight(int explicit, Optional<String> origin, boolean registered) {
        return new LandingCandidates.Flight(explicit, null, false, origin, registered, BlockPos.ZERO, Set.of(), Map.of());
    }

    static List<PadCandidate> scattered(long seed, int count, double occupiedShare) {
        Random r = new Random(seed);
        List<PadCandidate> truth = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int x = r.nextInt(60_000) - 30_000;
            int z = r.nextInt(60_000) - 30_000;
            truth.add(node(i, "Pad " + (i + 1), x, z, r.nextDouble() > 0.05, r.nextDouble() < occupiedShare));
        }
        return truth;
    }

    static LandingContext context(List<PadCandidate> pads, LandingCandidates.Flight f) {
        return new LandingContext("nerospace:luna", f.explicitIndex(), pads, Optional.empty(), f.originPadName(),
                f.originRegistered(), f.anchor(), Optional.empty(), LandingResolverTest.CLEAR, f.excluded());
    }

    @Test
    void fullRegistryLandingReadsOneNode() {
        List<PadCandidate> truth = scattered(7L, 255, 0.0);
        truth.add(node(255, "Base", 120, -40, true, false)); // the one near the arrival anchor
        FakeLevel level = new FakeLevel(truth);
        List<PadCandidate> plan = LandingCandidates.plan(nodes(truth), flight(-1, Optional.empty(), false), level);
        assertEquals(1, level.probes, "only the nearest node is read");
        LandingDecision d = LandingResolver.resolve(context(plan, flight(-1, Optional.empty(), false)));
        assertEquals(255, assertInstanceOf(LandingDecision.PadLanding.class, d).pad().id());
    }

    @Test
    void everyNodeOccupiedStaysInsideTheChunkBudget() {
        List<PadCandidate> truth = scattered(11L, 256, 1.0); // nothing usable: the worst case for the walks
        FakeLevel level = new FakeLevel(truth);
        LandingCandidates.Flight f = flight(3, Optional.of("Home"), true);
        List<PadCandidate> plan = LandingCandidates.plan(nodes(truth), f, level);
        assertTrue(level.chunks.size() <= LandingChunkBudget.MAX_PROBE_CHUNKS,
                "probed chunks " + level.chunks.size());
        assertInstanceOf(LandingDecision.PodLanding.class, LandingResolver.resolve(context(plan, f)));
    }

    @Test
    void namedPadsAreAllReadAndPair() {
        List<PadCandidate> truth = scattered(13L, 200, 0.0);
        truth.add(node(200, "Home", 9_000, 9_000, true, false));
        truth.add(node(201, "Home", 9_050, 9_000, true, true));
        FakeLevel level = new FakeLevel(truth);
        LandingCandidates.Flight f = flight(-1, Optional.of("Home"), true);
        List<PadCandidate> plan = LandingCandidates.plan(nodes(truth), f, level);
        assertTrue(level.probes <= 4, "probes " + level.probes); // 2 named + the preferred / nearest walks
        LandingDecision d = LandingResolver.resolve(context(plan, f));
        LandingDecision.PadLanding p = assertInstanceOf(LandingDecision.PadLanding.class, d);
        assertEquals(3, p.step());
        assertEquals(200, p.pad().id());
    }

    @Test
    void lazyPlanDecidesLikeTheFullSnapshot() {
        for (long seed = 1; seed <= 200; seed++) {
            Random r = new Random(seed * 31L);
            List<PadCandidate> truth = new ArrayList<>();
            int count = 1 + r.nextInt(120);
            for (int i = 0; i < count; i++) {
                int spread = r.nextBoolean() ? 3_000 : 40_000;
                truth.add(node(i, r.nextInt(8) == 0 ? "Home" : "Pad " + (i + 1), r.nextInt(spread) - spread / 2,
                        r.nextInt(spread) - spread / 2, r.nextDouble() > 0.1, r.nextDouble() < 0.15));
            }
            int explicit = r.nextInt(4) == 0 ? r.nextInt(count) : -1;
            boolean registered = r.nextBoolean();
            LandingCandidates.Flight f = flight(explicit, registered ? Optional.of("Home") : Optional.empty(),
                    registered);
            FakeLevel level = new FakeLevel(truth);
            List<PadCandidate> plan = LandingCandidates.plan(nodes(truth), f, level);
            if (level.chunks.size() >= LandingChunkBudget.MAX_PROBE_CHUNKS) {
                continue; // the budget cut the walk short: allowed to differ (falls to a pod)
            }
            LandingDecision lazy = LandingResolver.resolve(context(plan, f));
            LandingDecision full = LandingResolver.resolve(context(truth, f));
            assertEquals(full.getClass(), lazy.getClass(), "seed " + seed);
            assertEquals(full.nodeId(), lazy.nodeId(), "seed " + seed);
        }
    }

    @Test
    void podSquareCoversTheWholeSpiralAndIsFourByFour() {
        Random r = new Random(5L);
        for (int i = 0; i < 2_000; i++) {
            BlockPos p = new BlockPos(r.nextInt(200_000) - 100_000, 64, r.nextInt(200_000) - 100_000);
            LandingChunkBudget.Square sq = LandingChunkBudget.podSquare(p);
            assertEquals(4, sq.size());
            for (int d = -PodSearch.OUTER_RADIUS; d <= PodSearch.OUTER_RADIUS; d += 4) {
                assertTrue(sq.containsBlock(p.getX() + d, p.getZ() + PodSearch.OUTER_RADIUS), p + " " + d);
                assertTrue(sq.containsBlock(p.getX() + d, p.getZ() - PodSearch.OUTER_RADIUS), p + " " + d);
                assertTrue(sq.containsBlock(p.getX() - PodSearch.OUTER_RADIUS, p.getZ() + d), p + " " + d);
                assertTrue(sq.containsBlock(p.getX() + PodSearch.OUTER_RADIUS, p.getZ() + d), p + " " + d);
            }
        }
    }

    @Test
    void podSearchReadsOnlyTheSquare() {
        // A sampler that counts the chunks of every column / site the search reads, refusing nothing.
        Set<Long> read = new HashSet<>();
        BlockPos preferred = new BlockPos(1_237, 64, -5_003);
        LandingChunkBudget.Square sq = LandingChunkBudget.podSquare(preferred);
        PodSearch.Sampler counting = new PodSearch.Sampler() {
            @Override
            public SiteSample column(int x, int z) {
                read.add(LandingChunkBudget.chunkKey(x >> 4, z >> 4));
                return PodSearchTest.bad(new BlockPos(x, 64, z));
            }

            @Override
            public SiteSample at(BlockPos site) {
                read.add(LandingChunkBudget.chunkKey(site.getX() >> 4, site.getZ() >> 4));
                return SiteSample.clear(site);
            }
        };
        PodSearch.search(counting, preferred);
        assertTrue(read.size() <= 16, "chunks " + read.size());
        for (long key : read) {
            int cx = (int) (key >> 32);
            int cz = (int) key;
            assertTrue(sq.containsChunk(cx, cz), "chunk " + cx + "," + cz + " outside the 4x4 square");
        }
    }
}
