package za.co.neroland.nerospace.world.planet.europa.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import za.co.neroland.nerospace.registry.ModTags;
import za.co.neroland.nerospace.world.planet.europa.EuropaContent;

/**
 * What counts as heat or liquid for Europa's blocks. A heat source is a block in {@code #nerospace:europa_heat_sources}
 * that is actually hot: a campfire must be lit and a hydrothermal vent must be active. Liquid is water (the
 * {@code minecraft:water} fluid tag) or brine; lava and every other fluid are not.
 */
public final class EuropaHeat {

    private EuropaHeat() {
    }

    public static boolean isHeat(BlockState state) {
        if (!state.is(ModTags.Blocks.EUROPA_HEAT_SOURCES)) {
            return false;
        }
        if (state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)) {
            return false;
        }
        return !state.hasProperty(HydrothermalVentBlock.ACTIVE) || state.getValue(HydrothermalVentBlock.ACTIVE);
    }

    /** Water or brine; lava and every other fluid are not (verifier V05). */
    public static boolean isLiquid(BlockState state) {
        return liquid(state.getFluidState().is(FluidTags.WATER), isBrine(state));
    }

    /** Pure form of {@link #isLiquid}: water or brine. */
    public static boolean liquid(boolean water, boolean brine) {
        return water || brine;
    }

    public static boolean isBrine(BlockState state) {
        return state.is(EuropaContent.BRINE.get());
    }

    /** One flag per direction (indexed by {@link Direction#get3DDataValue()}): is that neighbour a heat source. */
    public static boolean[] heatAround(BlockGetter level, BlockPos pos) {
        boolean[] heat = new boolean[6];
        for (Direction d : Direction.values()) {
            heat[d.get3DDataValue()] = isHeat(level.getBlockState(pos.relative(d)));
        }
        return heat;
    }

    public static boolean liquidAdjacent(BlockGetter level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (isLiquid(level.getBlockState(pos.relative(d)))) {
                return true;
            }
        }
        return false;
    }

    public static boolean brineAdjacent(BlockGetter level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (isBrine(level.getBlockState(pos.relative(d)))) {
                return true;
            }
        }
        return false;
    }
}
