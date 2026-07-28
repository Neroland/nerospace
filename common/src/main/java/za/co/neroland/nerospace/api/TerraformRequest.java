package za.co.neroland.nerospace.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Validated request to create or advance a reversible regional terraforming overlay. */
public record TerraformRequest(Identifier id, BlockPos center, int radius, int stage, float progress) {

    public TerraformRequest {
        if (id == null || center == null) {
            throw new IllegalArgumentException("Terraform request id and center are required");
        }
        if (radius < 1 || radius > 256 || stage < 0 || stage > 3 || !Float.isFinite(progress)
                || progress < 0.0F || progress > 1.0F) {
            throw new IllegalArgumentException("Terraform request is outside bounded stage/region limits");
        }
        center = center.immutable();
    }
}
