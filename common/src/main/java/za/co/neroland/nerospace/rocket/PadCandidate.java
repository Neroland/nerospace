package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.jetbrains.annotations.Nullable;

/**
 * One registered landing node as the pure {@link LandingResolver} sees it (005 §1.2): identity, resolved
 * centre/facing and the two world facts the adapter read — whether the pad/dock/pod block still stands and
 * whether a rocket already sits on it (005 G1).
 *
 * @param facing the node's front, or {@code null} when unknown (yaw 0)
 */
public record PadCandidate(int id, String name, PadKind kind, BlockPos pos, BlockPos center,
        @Nullable Direction facing, boolean padBlockStillPresent, boolean occupied) {

    public PadCandidate {
        name = name == null ? "" : name;
        kind = kind == null ? PadKind.LAUNCH_PAD : kind;
        center = center == null ? pos : center;
    }

    /** Standing, free and of a kind steps 1–4 may pick. */
    public boolean usable() {
        return this.padBlockStillPresent && !this.occupied && this.kind.selectable();
    }
}
