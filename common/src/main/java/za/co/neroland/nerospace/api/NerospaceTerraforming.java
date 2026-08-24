package za.co.neroland.nerospace.api;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerospace.machine.TerraformConversion;
import za.co.neroland.nerospace.world.TerraformOverlayState;

/**
 * Authorized, reversible regional terraforming overlay facade.
 *
 * <p><b>Public API — semver-stable.</b> An overlay changes what Nerospace <em>reports</em> about a region
 * (see {@link NerospaceEnvironment}) without rewriting a single block or chunk flag, which is what makes
 * {@link #rollback} instant and lossless. The physical terraforming chunk stage is recorded as a baseline
 * when the region is created, so removing an overlay can never erase real progress underneath it.</p>
 *
 * <h2>Authorization</h2>
 * Nerospace ships no claim system, so {@link #setClaimPolicy} defaults to <b>deny everything</b>. A server
 * that wants overlay terraforming installs a policy backed by whatever claim mod it runs. This is
 * fail-closed on purpose: an unauthenticated regional environment override is a griefing tool.
 *
 * <h2>Privacy (POPIA/GDPR)</h2>
 * No actor is stored. The policy is handed the acting player to make its decision, and the decision — not
 * the identity — is what reaches the saved data.
 */
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

    /**
     * Creates or advances the overlay region named by {@code request}.
     *
     * @return true when the stored region actually changed; false when unauthorized, when the centre's
     *         chunk is not loaded, or when the identical region was already stored
     */
    public static boolean apply(ServerPlayer actor, ServerLevel level, TerraformRequest request) {
        if (actor == null || level == null || request == null || !level.hasChunkAt(request.center())) {
            return false;
        }
        if (!claimPolicy.mayMutate(actor, level, request)) {
            return false;
        }
        // effectiveStage, not the raw attachment: a legacy chunk carries only the TERRAFORMED boolean,
        // which is stage 1. Reading the raw value would record a baseline of 0 for ground that is
        // already terraformed, and every read path uses effectiveStage.
        int baseline = TerraformConversion.effectiveStage(level.getChunkAt(request.center()));
        return TerraformOverlayState.get(level).apply(request, baseline);
    }

    /** Removes an overlay region, restoring whatever the physical terraforming stage underneath it is. */
    public static boolean rollback(ServerPlayer actor, ServerLevel level, Identifier regionId) {
        if (actor == null || level == null || regionId == null) {
            return false;
        }
        TerraformOverlayState state = TerraformOverlayState.get(level);
        Optional<TerraformRegion> region = state.get(regionId);
        if (region.isEmpty()) {
            return false;
        }
        TerraformRegion value = region.orElseThrow();
        TerraformRequest request = new TerraformRequest(value.id(), value.center(), value.radius(),
                value.stage(), value.progress());
        if (!claimPolicy.mayMutate(actor, level, request)) {
            return false;
        }
        return state.rollback(regionId);
    }

    /** The region with this id, if one exists. */
    public static Optional<TerraformRegion> region(ServerLevel level, Identifier id) {
        return level == null || id == null ? Optional.empty() : TerraformOverlayState.get(level).get(id);
    }

    /** The most advanced region covering this position, if any. Empty when the chunk is not loaded. */
    public static Optional<TerraformRegion> at(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null || !level.hasChunkAt(pos)) {
            return Optional.empty();
        }
        return TerraformOverlayState.get(level).at(pos);
    }
}
