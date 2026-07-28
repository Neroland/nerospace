package za.co.neroland.nerospace.api;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerospace.platform.Services;
import za.co.neroland.nerospace.world.TerraformOverlayState;

/** Authorized, reversible regional overlay facade. No internal manager or owner identity is exposed. */
public final class NerospaceTerraforming {

    private static volatile TerraformClaimPolicy claimPolicy = (actor, level, request) -> false;

    private NerospaceTerraforming() {
    }

    /** Installs the server's claim integration. The default denies every mutation. */
    public static void setClaimPolicy(TerraformClaimPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Terraform claim policy must not be null");
        }
        claimPolicy = policy;
    }

    public static boolean apply(ServerPlayer actor, ServerLevel level, TerraformRequest request) {
        if (actor == null || level == null || request == null || !level.hasChunkAt(request.center())) {
            return false;
        }
        boolean authorized = claimPolicy.mayMutate(actor, level, request);
        int baseline = Services.PLATFORM.getTerraformStage(level.getChunkAt(request.center()));
        return TerraformOverlayState.get(level).apply(request, baseline, authorized);
    }

    public static boolean rollback(ServerPlayer actor, ServerLevel level, Identifier regionId) {
        if (actor == null || level == null || regionId == null) {
            return false;
        }
        Optional<TerraformRegion> region = TerraformOverlayState.get(level).get(regionId);
        if (region.isEmpty()) {
            return false;
        }
        TerraformRegion value = region.orElseThrow();
        TerraformRequest request = new TerraformRequest(value.id(), value.center(), value.radius(),
                value.stage(), value.progress());
        return TerraformOverlayState.get(level).rollback(regionId,
                claimPolicy.mayMutate(actor, level, request));
    }

    public static Optional<TerraformRegion> region(ServerLevel level, Identifier id) {
        return level == null || id == null ? Optional.empty() : TerraformOverlayState.get(level).get(id);
    }

    public static Optional<TerraformRegion> at(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null || !level.hasChunkAt(pos)) {
            return Optional.empty();
        }
        return TerraformOverlayState.get(level).at(pos);
    }
}
