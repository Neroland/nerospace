package za.co.neroland.nerospace.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.FrostRules;
import za.co.neroland.nerospace.registry.ModDimensions;

/**
 * Transparent airtight hull (Hull Window): vanilla-glass light, sealing via the {@code nerospace:airtight}
 * tag, and a {@code FROSTED} edge when it faces the void in the station (recomputed on neighbour change only).
 */
public class AirtightWindowBlock extends TransparentBlock {

    public static final MapCodec<AirtightWindowBlock> CODEC = BlockCodecs.simple(AirtightWindowBlock::new);

    public static final BooleanProperty FROSTED = BooleanProperty.create("frosted");

    @SuppressWarnings("this-escape")
    public AirtightWindowBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FROSTED, Boolean.FALSE));
    }

    protected MapCodec<? extends AirtightWindowBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FROSTED);
    }

    private static boolean frosted(LevelReader level, BlockPos pos) {
        boolean inStation = level instanceof net.minecraft.world.level.Level l
                && l.dimension().equals(ModDimensions.STATION_LEVEL);
        boolean[] air = new boolean[4];
        int i = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            air[i++] = level.getBlockState(pos.relative(d)).isAir();
        }
        return FrostRules.frosted(inStation, air);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FROSTED, frosted(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        BlockState updated = super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
        if (direction.getAxis().isHorizontal() && updated.is(this)) {
            return updated.setValue(FROSTED, frosted(level, pos));
        }
        return updated;
    }
}
