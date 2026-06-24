package za.co.neroland.nerospace.storage;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Trash Can: a void sink for items, fluids and gas. Pipe or hopper anything into it and it is
 * destroyed — handy for dumping the cobble/dirt a quarry digs up until item filters arrive. Backed
 * by {@link TrashCanBlockEntity}.
 */
public class TrashCanBlock extends BaseEntityBlock {

    public static final MapCodec<TrashCanBlock> CODEC = simpleCodec(TrashCanBlock::new);

    public TrashCanBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<TrashCanBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrashCanBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.TRASH_CAN.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }
}
