package za.co.neroland.nerospace.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.item.ItemResource;

import za.co.neroland.nerospace.pipe.UniversalPipeBlockEntity;
import za.co.neroland.nerospace.registry.ModDataComponents;

/**
 * Pipe Filter — restricts a pipe face's <b>item</b> layer to one item:
 * <ul>
 *   <li><b>Right-click (air)</b> holding the filter in one hand and the item to filter in the other:
 *       sets the filter (empty other hand clears it).</li>
 *   <li><b>Right-click a Universal Pipe face</b>: applies the filter to that face — only the
 *       configured item is pulled or pushed through it. Apply an empty filter to remove.</li>
 * </ul>
 */
public class PipeFilterItem extends Item {

    public PipeFilterItem(Properties properties) {
        super(properties);
    }

    public static ItemResource configured(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FILTER_ITEM.get(), ItemResource.EMPTY);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.isClientSide() && context.getPlayer() instanceof ServerPlayer player
                && level.getBlockEntity(pos) instanceof UniversalPipeBlockEntity pipe) {
            Direction face = context.getClickedFace();
            ItemResource filter = configured(context.getItemInHand());
            pipe.setFilter(face, filter);
            player.sendSystemMessage(filter.isEmpty()
                    ? Component.translatable("item.nerospace.pipe_filter.cleared_face", face.getName())
                    : Component.translatable("item.nerospace.pipe_filter.applied",
                            filter.getHoverName(), face.getName()));
            return InteractionResult.SUCCESS;
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack self = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (other.isEmpty()) {
                self.remove(ModDataComponents.FILTER_ITEM.get());
                serverPlayer.sendSystemMessage(Component.translatable("item.nerospace.pipe_filter.cleared"));
            } else {
                ItemResource resource = ItemResource.of(other);
                self.set(ModDataComponents.FILTER_ITEM.get(), resource);
                serverPlayer.sendSystemMessage(Component.translatable(
                        "item.nerospace.pipe_filter.set", resource.getHoverName()));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
