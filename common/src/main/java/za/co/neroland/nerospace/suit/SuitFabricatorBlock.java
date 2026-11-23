package za.co.neroland.nerospace.suit;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.menu.MenuOpener;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/** The bench that installs modules, applies frame plating, converts a legacy piece and charges a suit. */
public class SuitFabricatorBlock extends BaseEntityBlock {

    public static final MapCodec<SuitFabricatorBlock> CODEC = BlockCodecs.simple(SuitFabricatorBlock::new);

    public SuitFabricatorBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<SuitFabricatorBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SuitFabricatorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SuitFabricatorBlockEntity fabricator) {
            return MenuOpener.openOrConsume(player, fabricator);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.SUIT_FABRICATOR.get(),
                (lvl, p, st, be) -> be.serverTick());
    }
}
