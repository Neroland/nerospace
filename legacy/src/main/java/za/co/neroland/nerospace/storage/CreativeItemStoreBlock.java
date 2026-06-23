package za.co.neroland.nerospace.storage;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Creative Item Store block — right-click holding an item to set the endless source item (kept),
 * sneak-right-click to clear, bare hand reads out.
 */
public class CreativeItemStoreBlock extends AbstractStorageBlock {

    public static final MapCodec<CreativeItemStoreBlock> CODEC = simpleCodec(CreativeItemStoreBlock::new);

    public CreativeItemStoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CreativeItemStoreBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeItemStoreBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.isEmpty()) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                    && level.getBlockEntity(pos) instanceof CreativeItemStoreBlockEntity store) {
                ItemResource resource = ItemResource.of(stack);
                store.setSource(resource);
                serverPlayer.sendSystemMessage(Component.translatable(
                        "block.nerospace.creative_store.set", resource.getHoverName()));
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof CreativeItemStoreBlockEntity store) {
            if (player.isShiftKeyDown()) {
                store.setSource(ItemResource.EMPTY);
                serverPlayer.sendSystemMessage(Component.translatable("block.nerospace.creative_store.cleared"));
            } else if (store.source().isEmpty()) {
                serverPlayer.sendSystemMessage(Component.translatable("block.nerospace.creative_store.unset"));
            } else {
                serverPlayer.sendSystemMessage(Component.translatable(
                        "block.nerospace.creative_store.readout", store.source().getHoverName()));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
