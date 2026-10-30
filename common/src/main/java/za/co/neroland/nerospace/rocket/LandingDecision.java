package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** The outcome of {@link LandingResolver#resolve}: where the rocket stands, and why. */
public sealed interface LandingDecision permits LandingDecision.PadLanding, LandingDecision.PodLanding,
        LandingDecision.StationDock {

    /** Final rocket spawn pose (the descent starts 48 above it). */
    Vec3 spawn();

    float yaw();

    /** Message flags the arrival text needs. */
    Flags flags();

    /** The node id the rocket is landing on, or −1 for a new pod. */
    int nodeId();

    /**
     * @param padMissing  the explicitly chosen pad (or the target of a broken-pad re-resolve) was gone
     * @param padOccupied a pad the rules would have picked had a rocket on it
     */
    record Flags(boolean padMissing, boolean padOccupied) {
        public static final Flags NONE = new Flags(false, false);
    }

    /** Steps 1–4: {@code step} is the rule that fired. */
    record PadLanding(PadCandidate pad, int step, Vec3 spawn, float yaw, Flags flags) implements LandingDecision {
        @Override
        public int nodeId() {
            return this.pad.id();
        }
    }

    /** Step 5: a Landing Pod at {@code site} (an existing empty one when {@code existingPodReused}). */
    record PodLanding(BlockPos site, boolean existingPodReused, int ringReached, int climbUsed, Vec3 spawn,
            float yaw, Flags flags, int reusedNodeId) implements LandingDecision {
        @Override
        public int nodeId() {
            return this.reusedNodeId;
        }
    }

    /** Station branch: the Docking Ring ({@code viaDockingRing}) or the legacy launch pad. */
    record StationDock(PadCandidate target, boolean viaDockingRing, Vec3 spawn, float yaw, Flags flags)
            implements LandingDecision {
        @Override
        public int nodeId() {
            return this.target.id();
        }
    }
}
