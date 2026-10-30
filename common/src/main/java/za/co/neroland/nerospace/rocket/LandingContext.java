package za.co.neroland.nerospace.rocket;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;

/**
 * Everything {@link LandingResolver} needs, captured before any world mutation. Immutable.
 *
 * @param destinationDim       dimension id string, as {@link PadRegistry} stores it
 * @param explicitPadIndex     the rocket's {@code DATA_PAD}; −1 = auto
 * @param padsInDestination    {@link PadRegistry#inDimension} order (the index the Rocket screen addresses)
 * @param returnSite           this rocket's return pad, only when it is in the destination dimension
 * @param originPadName        the origin node's name; empty for auto-labels, blank names and unregistered pads
 * @param originRegistered     whether the launch pad was a registered node at all
 * @param arrivalAnchor        the planet's arrival anchor (X/Z; Y ignored), the last-resort search origin
 * @param station              the station branch, present only for station arrivals
 * @param sampler              Landing Pod site sampler over the destination
 * @param excludedNodeIds      nodes to ignore (a pad broken mid-descent)
 */
public record LandingContext(String destinationDim, int explicitPadIndex, List<PadCandidate> padsInDestination,
        Optional<ReturnSite> returnSite, Optional<String> originPadName, boolean originRegistered,
        BlockPos arrivalAnchor, Optional<StationTarget> station, PodSearch.Sampler sampler,
        Set<Integer> excludedNodeIds) {

    public LandingContext {
        padsInDestination = padsInDestination == null ? List.of() : List.copyOf(padsInDestination);
        returnSite = returnSite == null ? Optional.empty() : returnSite;
        originPadName = originPadName == null ? Optional.empty() : originPadName;
        station = station == null ? Optional.empty() : station;
        excludedNodeIds = excludedNodeIds == null ? Set.of() : Set.copyOf(excludedNodeIds);
        arrivalAnchor = arrivalAnchor == null ? BlockPos.ZERO : arrivalAnchor;
    }

    /** Whether this is a station arrival (the destination's archetype is ORBITAL). */
    public boolean destinationIsStation() {
        return this.station.isPresent();
    }

    /** A copy that ignores {@code nodeId} and the explicit pad choice (re-resolve after a broken pad). */
    public LandingContext excluding(int nodeId) {
        java.util.Set<Integer> ex = new java.util.HashSet<>(this.excludedNodeIds);
        ex.add(nodeId);
        return new LandingContext(this.destinationDim, -1, this.padsInDestination, this.returnSite,
                this.originPadName, this.originRegistered, this.arrivalAnchor, this.station, this.sampler, ex);
    }
}
