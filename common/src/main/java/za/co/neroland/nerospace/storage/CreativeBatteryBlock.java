package za.co.neroland.nerospace.storage;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/** Creative Battery block — holds a {@link CreativeBatteryBlockEntity}. */
public class CreativeBatteryBlock extends BaseEntityBlock {

    public static final @org.jspecify.annotations.NonNull MapCodec<CreativeBatteryBlock> CODEC = simpleCodec(CreativeBatteryBlock::new);

    public CreativeBatteryBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CreativeBatteryBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeBatteryBlockEntity(pos, state);
    }
}
