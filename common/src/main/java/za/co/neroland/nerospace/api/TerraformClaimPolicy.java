package za.co.neroland.nerospace.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Claim/territory authorization hook for {@link NerospaceTerraforming}.
 *
 * <p><b>Public API — semver-stable.</b> Nerospace has no claim system of its own, so it refuses every
 * overlay mutation until a server installs a policy (see
 * {@link NerospaceTerraforming#setClaimPolicy}). Implementations inspect the actor to make the decision;
 * Nerospace never stores the resulting owner id.</p>
 */
@FunctionalInterface
public interface TerraformClaimPolicy {

    /** Whether {@code actor} may create, advance or roll back this overlay region. */
    boolean mayMutate(ServerPlayer actor, ServerLevel level, TerraformRequest request);
}
