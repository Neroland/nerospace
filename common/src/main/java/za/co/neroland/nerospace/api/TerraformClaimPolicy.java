package za.co.neroland.nerospace.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Claim/territory authorization hook. Implementations inspect the actor but Nerospace stores no owner id. */
@FunctionalInterface
public interface TerraformClaimPolicy {

    boolean mayMutate(ServerPlayer actor, ServerLevel level, TerraformRequest request);
}
