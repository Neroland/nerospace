package za.co.neroland.nerospace.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Validated request to create or advance a reversible regional terraforming overlay.
 *
 * <p><b>Public API — semver-stable.</b> Unlike {@link TerraformRegion}, which clamps because it is built
 * from persisted data, a request <em>throws</em> on out-of-range input: a caller passing a 10,000-block
 * radius has a bug, and silently shrinking it would hide that.</p>
 *
 * <p>The {@code id} is caller-owned and is what {@link NerospaceTerraforming#rollback} addresses. As with
 * {@link NerospaceOxygen}, it must never encode player identity — it is persisted verbatim.</p>
 */
public record TerraformRequest(Identifier id, BlockPos center, int radius, int stage, float progress) {

    /** Largest overlay radius, in blocks. */
    public static final int MAX_RADIUS = 256;

    /** Highest terraforming stage, matching {@code TerraformConversion}. */
    public static final int MAX_STAGE = 3;

    public TerraformRequest {
        if (id == null || center == null) {
            throw new IllegalArgumentException("Terraform request id and center are required");
        }
        if (radius < 1 || radius > MAX_RADIUS || stage < 0 || stage > MAX_STAGE
                || !Float.isFinite(progress) || progress < 0.0F || progress > 1.0F) {
            throw new IllegalArgumentException("Terraform request is outside bounded stage/region limits");
        }
        center = center.immutable();
    }
}
