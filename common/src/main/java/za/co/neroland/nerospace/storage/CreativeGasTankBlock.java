package za.co.neroland.nerospace.storage;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/** Creative Gas Tank block — holds a {@link CreativeGasTankBlockEntity}. */
public class CreativeGasTankBlock extends AbstractStorageBlock {

    public static final @org.jspecify.annotations.NonNull MapCodec<CreativeGasTankBlock> CODEC = simpleCodec(CreativeGasTankBlock::new);

    public CreativeGasTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CreativeGasTankBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeGasTankBlockEntity(pos, state);
    }
}
