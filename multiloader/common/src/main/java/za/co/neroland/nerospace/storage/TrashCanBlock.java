package za.co.neroland.nerospace.storage;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/** Trash Can block — holds a {@link TrashCanBlockEntity} void sink. */
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrashCanBlockEntity(pos, state);
    }
}
