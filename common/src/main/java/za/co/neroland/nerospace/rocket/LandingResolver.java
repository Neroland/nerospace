package za.co.neroland.nerospace.rocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.rocket.LandingDecision.Flags;
import za.co.neroland.nerospace.rocket.LandingDecision.PadLanding;
import za.co.neroland.nerospace.rocket.LandingDecision.PodLanding;
import za.co.neroland.nerospace.rocket.LandingDecision.StationDock;

/**
 * The pure landing resolver (005 §1): explicit pad → return pad → paired pad by name → nearest node
 * (≤ 2048) → Landing Pod; station arrivals prefer a Docking Ring, then the legacy pad, then a pod at the pad
 * centre. No {@code Level}, no server: {@link LandingResolverLevelAdapter} builds the {@link LandingContext}
 * and applies the decision.
 */
public final class LandingResolver {

    /** {@code RocketLaunchPadBlock.SURFACE_HEIGHT} (3/16), duplicated so this class stays loader-free. */
    public static final double SURFACE_HEIGHT = 3.0D / 16.0D;
    /** A Landing Pod is a full cube: the rocket stands on its top face (orchestrator ruling on 005 §1.5). */
    public static final double POD_SURFACE_HEIGHT = 1.0D;
    /** Step 4 never picks a node farther than this (Chebyshev, horizontal) from {@code preferred}. */
    public static final int NEAREST_BOUND = 2048;
    /** Step 3 with duplicate names needs one of them within this distance of {@code preferred} (005 G2). */
    public static final int PAIR_AMBIGUITY_RADIUS = 256;

    private static final Pattern AUTO_LABEL = Pattern.compile("^Pad \\d+$");

    private LandingResolver() {
    }

    public static LandingDecision resolve(LandingContext ctx) {
        if (ctx.station().isPresent()) {
            return resolveStation(ctx, ctx.station().get());
        }
        List<PadCandidate> pads = new ArrayList<>();
        for (PadCandidate c : ctx.padsInDestination()) {
            if (!ctx.excludedNodeIds().contains(c.id())) {
                pads.add(c);
            }
        }
        boolean missing = false;
        boolean occupied = false;

        // Step 1 — explicit pad (indexes the unfiltered list: the Rocket screen's index).
        int idx = ctx.explicitPadIndex();
        if (idx >= 0) {
            PadCandidate chosen = idx < ctx.padsInDestination().size() ? ctx.padsInDestination().get(idx) : null;
            if (chosen != null && !ctx.excludedNodeIds().contains(chosen.id()) && chosen.usable()) {
                return padLanding(chosen, 1, Flags.NONE);
            }
            if (chosen != null && chosen.padBlockStillPresent() && chosen.occupied()) {
                occupied = true;
            } else {
                missing = true;
            }
        }
        if (!ctx.excludedNodeIds().isEmpty()) {
            missing = true; // a re-resolve after the target broke mid-descent
        }

        // Step 2 — return pad if one still stands there.
        Optional<ReturnSite> ret = ctx.returnSite();
        if (ret.isPresent() && ret.get().standing()) {
            ReturnSite r = ret.get();
            boolean excluded = r.registeredNode().map(n -> ctx.excludedNodeIds().contains(n.id())).orElse(false);
            if (!excluded && !r.occupied()) {
                PadCandidate target = r.registeredNode().orElseGet(() -> new PadCandidate(-1, "", PadKind.LAUNCH_PAD,
                        r.pos(), r.center(), r.facing(), true, false));
                return padLanding(target, 2, new Flags(missing, occupied));
            }
            if (r.occupied()) {
                occupied = true;
            }
        }

        BlockPos preferred = preferred(ctx, pads);

        // Step 3 — paired pad by exact (stripped) name.
        PadCandidate paired = paired(ctx, pads, preferred);
        if (paired != null) {
            return padLanding(paired, 3, new Flags(missing, occupied));
        }
        for (PadCandidate c : pads) {
            if (c.padBlockStillPresent() && c.occupied() && c.kind().selectable()) {
                occupied = true;
            }
        }

        // Step 4 — nearest usable node within the bound.
        PadCandidate nearest = nearest(pads, preferred);
        if (nearest != null) {
            return padLanding(nearest, 4, new Flags(missing, occupied));
        }

        // Step 5 — Landing Pod.
        return pod(ctx, preferred, new Flags(missing, occupied));
    }

    private static LandingDecision resolveStation(LandingContext ctx, StationTarget st) {
        Optional<PadCandidate> dock = st.dock().filter(d -> !ctx.excludedNodeIds().contains(d.id()));
        if (dock.isPresent() && dock.get().padBlockStillPresent() && !dock.get().occupied()) {
            PadCandidate d = dock.get();
            return new StationDock(d, true, spawnOf(d.center()), yawOf(d.facing()), Flags.NONE);
        }
        Optional<PadCandidate> pad = st.legacyPad().filter(d -> !ctx.excludedNodeIds().contains(d.id()));
        boolean occupied = dock.map(PadCandidate::occupied).orElse(false);
        if (pad.isPresent() && pad.get().padBlockStillPresent() && !pad.get().occupied()) {
            PadCandidate p = pad.get();
            return new StationDock(p, false, spawnOf(p.center()), yawOf(p.facing()), new Flags(false, occupied));
        }
        occupied |= pad.map(PadCandidate::occupied).orElse(false);
        return pod(ctx, st.padCenter(), new Flags(!ctx.excludedNodeIds().isEmpty(), occupied));
    }

    /** 005 §1.4: where steps 4 and 5 search from. Never the rider's origin-dimension X/Z. */
    static BlockPos preferred(LandingContext ctx, List<PadCandidate> pads) {
        Optional<ReturnSite> ret = ctx.returnSite();
        if (ret.isPresent() && !ret.get().standing()) {
            return ret.get().pos();
        }
        BlockPos anchor = ctx.arrivalAnchor();
        if (ctx.originRegistered()) {
            PadCandidate best = null;
            long bestSq = Long.MAX_VALUE;
            for (PadCandidate c : pads) {
                if (!c.usable()) {
                    continue;
                }
                long sq = horizontalSq(c.center(), anchor);
                if (sq < bestSq || (sq == bestSq && best != null && c.id() < best.id())) {
                    bestSq = sq;
                    best = c;
                }
            }
            if (best != null) {
                return best.center();
            }
        }
        return anchor;
    }

    @Nullable
    private static PadCandidate paired(LandingContext ctx, List<PadCandidate> pads, BlockPos preferred) {
        if (!ctx.originRegistered() || ctx.originPadName().isEmpty()) {
            return null;
        }
        String origin = ctx.originPadName().get().strip();
        if (origin.isEmpty() || AUTO_LABEL.matcher(origin).matches()) {
            return null;
        }
        List<PadCandidate> named = new ArrayList<>();
        for (PadCandidate c : pads) {
            if (c.kind() != PadKind.POD && c.name().strip().equals(origin)) {
                named.add(c);
            }
        }
        if (named.size() > 1) {
            boolean anyClose = false;
            for (PadCandidate c : named) {
                if (chebyshev(c.center(), preferred) <= PAIR_AMBIGUITY_RADIUS) {
                    anyClose = true;
                }
            }
            if (!anyClose) {
                return null; // ambiguous and far: fall to nearest (005 G2)
            }
        }
        PadCandidate best = null;
        for (PadCandidate c : named) {
            if (c.usable() && (best == null || c.id() < best.id())) {
                best = c;
            }
        }
        return best;
    }

    @Nullable
    private static PadCandidate nearest(List<PadCandidate> pads, BlockPos preferred) {
        PadCandidate best = null;
        long bestSq = Long.MAX_VALUE;
        for (PadCandidate c : pads) {
            if (!c.usable() || chebyshev(c.center(), preferred) > NEAREST_BOUND) {
                continue;
            }
            long sq = horizontalSq(c.center(), preferred);
            if (sq < bestSq || (sq == bestSq && best != null && c.id() < best.id())) {
                bestSq = sq;
                best = c;
            }
        }
        return best;
    }

    private static PodLanding pod(LandingContext ctx, BlockPos preferred, Flags flags) {
        PodSearch.Found found = PodSearch.search(ctx.sampler(), preferred);
        int reusedId = -1;
        if (found.reused()) {
            for (PadCandidate c : ctx.padsInDestination()) {
                if (c.pos().equals(found.site())) {
                    reusedId = c.id();
                }
            }
        }
        return new PodLanding(found.site(), found.reused(), found.ringReached(), found.climbUsed(),
                podSpawnOf(found.site()), yawOf(null), flags, reusedId);
    }

    private static PadLanding padLanding(PadCandidate pad, int step, Flags flags) {
        return new PadLanding(pad, step, spawnOf(pad.center()), yawOf(pad.facing()), flags);
    }

    /** 005 §1.5: {@code center + (0.5, SURFACE_HEIGHT, 0.5)}. */
    public static Vec3 spawnOf(BlockPos center) {
        return new Vec3(center.getX() + 0.5D, center.getY() + SURFACE_HEIGHT, center.getZ() + 0.5D);
    }

    /** A pod landing: standing on the pod's top face, centred. */
    public static Vec3 podSpawnOf(BlockPos pod) {
        return new Vec3(pod.getX() + 0.5D, pod.getY() + POD_SURFACE_HEIGHT, pod.getZ() + 0.5D);
    }

    /** 005 §1.5: SOUTH 0, WEST 90, NORTH 180, EAST −90; no facing → 0. */
    public static float yawOf(@Nullable Direction facing) {
        if (facing == null) {
            return 0.0F;
        }
        return switch (facing) {
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
    }

    /** Whether a node name is an auto-label ("Pad N") that never pairs. */
    public static boolean isAutoLabel(String name) {
        return name == null || name.isBlank() || AUTO_LABEL.matcher(name.strip()).matches();
    }

    static int chebyshev(BlockPos a, BlockPos b) {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getZ() - b.getZ()));
    }

    private static long horizontalSq(BlockPos a, BlockPos b) {
        long dx = a.getX() - b.getX();
        long dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }
}
