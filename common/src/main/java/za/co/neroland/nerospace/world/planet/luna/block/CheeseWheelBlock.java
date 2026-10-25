package za.co.neroland.nerospace.world.planet.luna.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.CheeseRules;
import za.co.neroland.nerospace.world.planet.luna.LunaContent;
import za.co.neroland.nerospace.world.planet.luna.LunaNumbers;

/**
 * The wheel: a 12/16 round, 8/16 tall. A sword or an axe slices one piece off ({@code BITES} 0–8, the ninth slice
 * takes the last of it); breaking it gives back what is left. Nine in, nine out.
 */
public class CheeseWheelBlock extends Block {

    public static final MapCodec<CheeseWheelBlock> CODEC = BlockCodecs.simple(CheeseWheelBlock::new);

    public static final IntegerProperty BITES = IntegerProperty.create("bites", 0, CheeseRules.MAX_BITES);

    private static final VoxelShape[] SHAPES = new VoxelShape[CheeseRules.MAX_BITES + 1];

    static {
        for (int b = 0; b <= CheeseRules.MAX_BITES; b++) {
            SHAPES[b] = Block.box(LunaNumbers.wheelMinX(b), 0.0, 2.0, 14.0, 8.0, 14.0);
        }
    }

    @SuppressWarnings("this-escape")
    public CheeseWheelBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(BITES, 0));
    }

    protected MapCodec<? extends CheeseWheelBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BITES);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(BITES)];
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ItemTags.SWORDS) && !stack.is(ItemTags.AXES)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            int next = CheeseRules.afterSlice(state.getValue(BITES));
            popResourceFromFace(level, pos, hit.getDirection(), new ItemStack(LunaContent.MOON_CHEESE.get()));
            if (next < 0) {
                level.removeBlock(pos, false);
            } else {
                level.setBlock(pos, state.setValue(BITES, next), Block.UPDATE_ALL);
            }
            level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.8F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }
}
