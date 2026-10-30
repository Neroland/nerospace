package za.co.neroland.nerospace.rocket;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.jetbrains.annotations.Nullable;

/**
 * The return-pad check result: {@code pos} is the rocket's saved return position; {@code standing}
 * = a launch pad (any size) is at {@code pos}; {@code center}/{@code facing} are the cluster-derived pose the
 * adapter resolved for an unregistered pad (a registered node's own centre/facing win).
 */
public record ReturnSite(BlockPos pos, boolean standing, boolean occupied, BlockPos center,
        @Nullable Direction facing, Optional<PadCandidate> registeredNode) {

    public ReturnSite {
        center = center == null ? pos : center;
        registeredNode = registeredNode == null ? Optional.empty() : registeredNode;
    }
}
