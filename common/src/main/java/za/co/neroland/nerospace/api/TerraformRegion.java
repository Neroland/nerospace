package za.co.neroland.nerospace.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Immutable public snapshot of a regional overlay; deliberately contains no owner identity. */
public record TerraformRegion(Identifier id, BlockPos center, int radius, int stage, float progress) {
}
