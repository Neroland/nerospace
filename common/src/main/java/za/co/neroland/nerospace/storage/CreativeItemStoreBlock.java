package za.co.neroland.nerospace.storage;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/**
 * Creative Item Store block — holds a {@link CreativeItemStoreBlockEntity}. Right-click with an item to
 * set the endless source; sneak-right-click (empty hand) to clear it.
 */
public class CreativeItemStoreBlock extends AbstractStorageBlock {

    public static final @org.jspecify.annotations.NonNull MapCodec<CreativeItemStoreBlock> CODEC = simpleCodec(CreativeItemStoreBlock::new);

    public CreativeItemStoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CreativeItemStoreBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeItemStoreBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CreativeItemStoreBlockEntity store) {
            store.setSource(stack);
            player.sendSystemMessage(Component.translatable(
                    "block.nerospace.creative_item_store.set", stack.getHoverName()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CreativeItemStoreBlockEntity store) {
            store.setSource(ItemStack.EMPTY);
            player.sendSystemMessage(Component.translatable("block.nerospace.creative_item_store.cleared"));
        }
        return InteractionResult.SUCCESS;
    }
}
