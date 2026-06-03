package za.co.neroland.nerospace.storage;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Item Store block — right-click opens the 27-slot chest GUI; drops contents when broken. */
public class ItemStoreBlock extends AbstractStorageBlock {

    public static final MapCodec<ItemStoreBlock> CODEC = simpleCodec(ItemStoreBlock::new);

    public ItemStoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ItemStoreBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemStoreBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ItemStoreBlockEntity store) {
            player.openMenu(store);
        }
        return InteractionResult.SUCCESS;
    }
}
