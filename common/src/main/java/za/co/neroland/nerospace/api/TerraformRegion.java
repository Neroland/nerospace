package za.co.neroland.nerospace.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Immutable public snapshot of a reversible terraforming overlay region.
 *
 * <p><b>Public API — semver-stable.</b> It deliberately contains no owner identity: who created a region is
 * the claim system's business, not a fact Nerospace persists or republishes.</p>
 *
 * <p>Values are clamped on construction to the same bounds {@link TerraformRequest} enforces, so a
 * hand-edited or corrupt saved-data row can never hand a consumer an out-of-range stage or radius.</p>
 *
 * @param stage    terraforming stage, 0-3
 * @param progress progress within the stage, 0.0-1.0
 */
public record TerraformRegion(Identifier id, BlockPos center, int radius, int stage, float progress) {

    public TerraformRegion {
        if (id == null || center == null) {
            throw new IllegalArgumentException("Terraform region id and center are required");
        }
        center = center.immutable();
        radius = Math.clamp(radius, 1, TerraformRequest.MAX_RADIUS);
        stage = Math.clamp(stage, 0, TerraformRequest.MAX_STAGE);
        progress = !Float.isFinite(progress) ? 0.0F : Math.clamp(progress, 0.0F, 1.0F);
    }
}
