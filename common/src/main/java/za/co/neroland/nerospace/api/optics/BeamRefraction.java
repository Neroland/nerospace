package za.co.neroland.nerospace.api.optics;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import za.co.neroland.nerospace.block.rules.RefractRules;
import za.co.neroland.nerospace.registry.ModTags;

/**
 * Additive public API (decision 002 §11.2): how a beam leaves a {@code nerospace:beam_refractors} block. Nerospace
 * never calls a sibling mod; a beam mod may call this. A refractor with a {@code facing} property turns a beam 90°
 * toward that facing (a beam along the facing axis passes straight through); any other block returns {@code in}.
 */
public final class BeamRefraction {

    private BeamRefraction() {
    }

    public static Direction deflect(Direction in, BlockState state) {
        if (in == null || state == null || !state.is(ModTags.Blocks.BEAM_REFRACTORS)
                || !state.hasProperty(BlockStateProperties.FACING)) {
            return in;
        }
        Direction facing = state.getValue(BlockStateProperties.FACING);
        return Direction.from3DDataValue(RefractRules.outDirection(in.get3DDataValue(), facing.get3DDataValue()));
    }
}
