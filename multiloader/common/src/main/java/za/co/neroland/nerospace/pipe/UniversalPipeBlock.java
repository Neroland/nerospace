package za.co.neroland.nerospace.pipe;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.registry.ModBlockEntities;

/** Universal Pipe block — ticks its {@link UniversalPipeBlockEntity} energy relay. */
public class UniversalPipeBlock extends BaseEntityBlock {

    public static final MapCodec<UniversalPipeBlock> CODEC = simpleCodec(UniversalPipeBlock::new);

    public UniversalPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<UniversalPipeBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UniversalPipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.UNIVERSAL_PIPE.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }
}
