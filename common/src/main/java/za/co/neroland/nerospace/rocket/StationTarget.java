package za.co.neroland.nerospace.rocket;

import java.util.Optional;

import net.minecraft.core.BlockPos;

/**
 * The station branch of a landing (005 §1.8): the slot (−1 = origin platform), its centre, the legacy pad
 * centre (where a pod goes if both the dock and the pad are gone), the nearest {@code DOCK} node and the
 * nearest {@code LAUNCH_PAD} node within the station's 48-block claim box.
 */
public record StationTarget(int slot, BlockPos centre, BlockPos padCenter, Optional<PadCandidate> dock,
        Optional<PadCandidate> legacyPad) {

    /** Nodes farther than this (Chebyshev, horizontal) from the slot centre belong to another slot. */
    public static final int CLAIM_RADIUS = 48;

    public StationTarget {
        dock = dock == null ? Optional.empty() : dock;
        legacyPad = legacyPad == null ? Optional.empty() : legacyPad;
    }
}
