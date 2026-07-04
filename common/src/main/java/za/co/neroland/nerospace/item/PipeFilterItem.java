package za.co.neroland.nerospace.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

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
 *
 * <p>Cross-loader port: the standalone mod stored the filter as a NeoForge-transfer {@code ItemResource};
 * the multiloader stores a vanilla {@link ItemStack} (the {@link ModDataComponents#FILTER_ITEM} component
 * and {@link UniversalPipeBlockEntity#setFilter} are both ItemStack-based here).</p>
 */
public class PipeFilterItem extends Item {

    public PipeFilterItem(Properties properties) {
        super(properties);
    }

    /** The filter set on this stack (EMPTY = unset). */
    public static ItemStack configured(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FILTER_ITEM.get(), ItemStack.EMPTY);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        ItemStack filter = configured(stack);
        tooltip.accept(filter.isEmpty()
                ? Component.translatable("item.nerospace.pipe_filter.tooltip.empty")
                        .withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("item.nerospace.pipe_filter.tooltip.filters",
                        filter.getHoverName()).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.isClientSide() && context.getPlayer() instanceof ServerPlayer player
                && level.getBlockEntity(pos) instanceof UniversalPipeBlockEntity pipe) {
            ItemStack held = context.getItemInHand();
            if (configured(held).isEmpty()) {
                player.sendSystemMessage(Component.translatable("item.nerospace.pipe_filter.unconfigured"));
                return InteractionResult.SUCCESS;
            }
            // Install the physical filter into the face's slot (like upgrades); any previously
            // installed filter pops back to the player. Remove filters via the Configurator GUI.
            Direction face = context.getClickedFace();
            ItemStack previous = pipe.installFilter(face, held);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            if (!previous.isEmpty()) {
                player.getInventory().placeItemBackInInventory(previous);
            }
            player.sendSystemMessage(Component.translatable("item.nerospace.pipe_filter.applied",
                    configured(pipe.filterItem(face)).getHoverName(), face.getName()));
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
                ItemStack resource = other.copyWithCount(1);
                self.set(ModDataComponents.FILTER_ITEM.get(), resource);
                serverPlayer.sendSystemMessage(Component.translatable(
                        "item.nerospace.pipe_filter.set", resource.getHoverName()));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
