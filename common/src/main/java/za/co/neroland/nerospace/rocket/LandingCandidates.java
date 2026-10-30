package za.co.neroland.nerospace.rocket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;

import org.jetbrains.annotations.Nullable;

/**
 * Lazy candidate probing for a planet landing, pure. The adapter knows every registered node as data
 * (id, name, kind, position — no chunk needed) and probes a node only when {@link LandingResolver} could pick it:
 * the explicit index, the return-site node, nodes whose stripped name equals the origin pad's, and — walking out
 * from the anchor and from {@code preferred} in distance order — nodes up to the first usable one within
 * {@link LandingResolver#NEAREST_BOUND}. Probes stop at {@link LandingChunkBudget#MAX_PROBE_CHUNKS} distinct chunks.
 * Every node that is not probed is passed to the resolver as not standing, so it is never chosen blind.
 */
public final class LandingCandidates {

    /** A registered node as plain data. {@code centre} is the stored centre, else the node position. */
    public record Node(int id, String name, PadKind kind, BlockPos pos, BlockPos centre) {

        public Node {
            name = name == null ? "" : name;
            kind = kind == null ? PadKind.LAUNCH_PAD : kind;
            centre = centre == null ? pos : centre;
        }
    }

    /** Reads node {@code listIndex} from the world (the only call that may load a chunk). */
    @FunctionalInterface
    public interface Probe {
        PadCandidate probe(int listIndex);
    }

    /**
     * What the resolver needs about the flight, as data.
     *
     * @param returnStandingKnown the return site was read and its pad stands ({@code returnPos} is then not
     *                            {@code preferred})
     * @param returnPos           the return pad position in this dimension, or {@code null}
     * @param known               candidates already read elsewhere (the return-site node), by node id
     */
    public record Flight(int explicitIndex, @Nullable BlockPos returnPos, boolean returnStandingKnown,
            Optional<String> originPadName, boolean originRegistered, BlockPos anchor, Set<Integer> excluded,
            Map<Integer, PadCandidate> known) {

        public Flight {
            originPadName = originPadName == null ? Optional.empty() : originPadName;
            anchor = anchor == null ? BlockPos.ZERO : anchor;
            excluded = excluded == null ? Set.of() : excluded;
            known = known == null ? Map.of() : known;
        }
    }

    private LandingCandidates() {
    }

    /** The candidate list in node order; unprobed nodes read as not standing. */
    public static List<PadCandidate> plan(List<Node> nodes, Flight flight, Probe probe) {
        Planner p = new Planner(nodes, flight, probe);
        return p.run();
    }

    private static final class Planner {
        private final List<Node> nodes;
        private final Flight flight;
        private final Probe probe;
        private final Map<Integer, PadCandidate> read = new HashMap<>();
        private final Set<Long> chunks = new HashSet<>();

        Planner(List<Node> nodes, Flight flight, Probe probe) {
            this.nodes = nodes;
            this.flight = flight;
            this.probe = probe;
        }

        List<PadCandidate> run() {
            // Pre-read candidates (the return-site node) cost nothing more.
            for (int i = 0; i < this.nodes.size(); i++) {
                PadCandidate k = this.flight.known().get(this.nodes.get(i).id());
                if (k != null) {
                    this.read.put(i, k);
                    Node n = this.nodes.get(i);
                    this.chunks.add(LandingChunkBudget.chunkKey(n.pos().getX() >> 4, n.pos().getZ() >> 4));
                }
            }
            // Step 1 — explicit index.
            int idx = this.flight.explicitIndex();
            if (idx >= 0 && idx < this.nodes.size()) {
                get(idx);
            }
            // Step 3 — every node carrying the origin pad's exact name (ambiguity needs them all).
            String origin = this.flight.originRegistered()
                    ? this.flight.originPadName().map(String::strip).orElse("") : "";
            if (!origin.isEmpty() && !LandingResolver.isAutoLabel(origin)) {
                for (int i = 0; i < this.nodes.size(); i++) {
                    Node n = this.nodes.get(i);
                    if (n.kind() != PadKind.POD && n.name().strip().equals(origin) && !excluded(i)) {
                        get(i);
                    }
                }
            }
            // preferred — the same rule the resolver applies, walking out from the anchor.
            BlockPos preferred = preferred();
            // Step 4 — walk out from preferred to the first usable node within the bound.
            walk(preferred, LandingResolver.NEAREST_BOUND);

            List<PadCandidate> out = new ArrayList<>(this.nodes.size());
            for (int i = 0; i < this.nodes.size(); i++) {
                PadCandidate c = this.read.get(i);
                if (c == null) {
                    Node n = this.nodes.get(i);
                    c = new PadCandidate(n.id(), n.name(), n.kind(), n.pos(), n.centre(), null, false, false);
                }
                out.add(c);
            }
            return out;
        }

        private BlockPos preferred() {
            BlockPos rp = this.flight.returnPos();
            if (rp != null && !this.flight.returnStandingKnown()) {
                return rp;
            }
            BlockPos anchor = this.flight.anchor();
            if (this.flight.originRegistered()) {
                PadCandidate best = walk(anchor, Integer.MAX_VALUE);
                if (best != null) {
                    return best.center();
                }
            }
            return anchor;
        }

        /** Probes in distance order from {@code from} until the first usable node; returns it (or null). */
        @Nullable
        private PadCandidate walk(BlockPos from, int bound) {
            List<Ranked> order = new ArrayList<>();
            for (int i = 0; i < this.nodes.size(); i++) {
                Node n = this.nodes.get(i);
                if (!excluded(i) && n.kind().selectable() && LandingResolver.chebyshev(n.centre(), from) <= bound) {
                    order.add(new Ranked(i, horizontalSq(n.centre(), from), n.id()));
                }
            }
            order.sort(Comparator.comparingLong(Ranked::sq).thenComparingInt(Ranked::id));
            for (Ranked r : order) {
                PadCandidate c = get(r.index());
                if (c != null && c.usable()) {
                    return c;
                }
            }
            return null;
        }

        private boolean excluded(int i) {
            return this.flight.excluded().contains(this.nodes.get(i).id());
        }

        /** The probed candidate, or {@code null} when the chunk budget refuses a new chunk. */
        @Nullable
        private PadCandidate get(int i) {
            PadCandidate c = this.read.get(i);
            if (c != null) {
                return c;
            }
            Node n = this.nodes.get(i);
            long key = LandingChunkBudget.chunkKey(n.pos().getX() >> 4, n.pos().getZ() >> 4);
            if (!this.chunks.contains(key)) {
                if (this.chunks.size() >= LandingChunkBudget.MAX_PROBE_CHUNKS) {
                    return null;
                }
                this.chunks.add(key);
            }
            c = this.probe.probe(i);
            this.read.put(i, c);
            return c;
        }
    }

    private record Ranked(int index, long sq, int id) {
    }

    static long horizontalSq(BlockPos a, BlockPos b) {
        long dx = a.getX() - b.getX();
        long dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }
}
