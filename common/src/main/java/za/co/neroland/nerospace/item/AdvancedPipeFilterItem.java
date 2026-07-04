package za.co.neroland.nerospace.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.menu.AdvancedFilterMenu;
import za.co.neroland.nerospace.pipe.FaceFilter;
import za.co.neroland.nerospace.pipe.UniversalPipeBlockEntity;
import za.co.neroland.nerospace.registry.ModDataComponents;

/**
 * Advanced Pipe Filter — the higher-tier Pipe Filter (issue #25: multiple items per pipe filter).
 * Holds a full {@link FaceFilter} (up to 9 item/tag entries, whitelist or blacklist, exact or
 * item-only matching) in the {@link ModDataComponents#ADVANCED_FILTER} component, configured
 * through a GUI rather than the basic filter's off-hand trick:
 * <ul>
 *   <li><b>Right-click (air / non-pipe)</b>: open the filter configuration GUI
 *       ({@link AdvancedFilterMenu}) editing this stack's stored filter.</li>
 *   <li><b>Right-click a Universal Pipe face</b>: apply the stored filter to that face
 *       (an unconfigured filter clears the face, mirroring the basic filter).</li>
 *   <li><b>Sneak + right-click a Universal Pipe face</b>: copy that face's current filter into
 *       this item — replicate a sorting wall without reconfiguring by hand.</li>
 * </ul>
 *
 * <p>The stored filter is a shareable template: the component travels with the stack, so a
 * configured filter can be crafted-around, stored, or handed to another player.</p>
 */
public class AdvancedPipeFilterItem extends Item {

    public AdvancedPipeFilterItem(Properties properties) {
        super(properties);
    }

    /** The filter configured on this stack ({@link FaceFilter#EMPTY} = unset). */
    public static FaceFilter configured(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ADVANCED_FILTER.get(), FaceFilter.EMPTY);
    }

    /** Store {@code filter} on the stack (removes the component when unconfigured-default). */
    public static void store(ItemStack stack, FaceFilter filter) {
        if (filter.isEmpty() && !filter.blacklist() && filter.matchComponents()) {
            stack.remove(ModDataComponents.ADVANCED_FILTER.get());
        } else {
            stack.set(ModDataComponents.ADVANCED_FILTER.get(), filter);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockEntity(pos) instanceof UniversalPipeBlockEntity pipe) {
            if (!level.isClientSide() && context.getPlayer() instanceof ServerPlayer player) {
                Direction face = context.getClickedFace();
                if (player.isShiftKeyDown()) {
                    // Copy the face's filter into the item (template pickup).
                    FaceFilter copied = pipe.filter(face);
                    store(context.getItemInHand(), copied);
                    player.sendSystemMessage(Component.translatable(
                            "item.nerospace.advanced_pipe_filter.copied",
                            face.getName(), copied.activeEntryCount()));
                } else {
                    FaceFilter filter = configured(context.getItemInHand());
                    pipe.setFaceFilter(face, filter);
                    player.sendSystemMessage(filter.isEmpty()
                            ? Component.translatable("item.nerospace.advanced_pipe_filter.cleared_face",
                                    face.getName())
                            : Component.translatable("item.nerospace.advanced_pipe_filter.applied",
                                    filter.activeEntryCount(), face.getName()));
                }
            }
            return InteractionResult.SUCCESS;
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new AdvancedFilterMenu(id, inventory, hand),
                    Component.translatable("item.nerospace.advanced_pipe_filter")));
        }
        return InteractionResult.SUCCESS;
    }
}
