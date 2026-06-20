package za.co.neroland.nerospace.storage;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/** Creative Fluid Tank block — holds a {@link CreativeFluidTankBlockEntity}. */
public class CreativeFluidTankBlock extends AbstractStorageBlock {

    public static final MapCodec<CreativeFluidTankBlock> CODEC = simpleCodec(CreativeFluidTankBlock::new);

    public CreativeFluidTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CreativeFluidTankBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeFluidTankBlockEntity(pos, state);
    }
}
