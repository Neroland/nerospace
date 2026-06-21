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

import za.co.neroland.nerospace.pipe.PipeIoMode;
import za.co.neroland.nerospace.pipe.PipeResourceType;
import za.co.neroland.nerospace.pipe.UniversalPipeBlockEntity;
import za.co.neroland.nerospace.registry.ModDataComponents;

/**
 * The Configurator — the pipe network tool. It edits one {@link PipeResourceType} layer at a time (the
 * "selected type", stored on the stack via {@link ModDataComponents#SELECTED_PIPE_TYPE}):
 * <ul>
 *   <li><b>Sneak + right-click</b> (in air or on a non-pipe block): cycle the selected resource type
 *       (energy → fluid → gas → item).</li>
 *   <li><b>Right-click a Universal Pipe face</b>: cycle that face's I/O mode for the selected type
 *       (auto → in → out → off).</li>
 * </ul>
 *
 * <p>Cross-loader port: already loader-agnostic in the standalone mod (vanilla item + data component);
 * copied verbatim. The full per-face × per-type config GUI is the deferred client slice.</p>
 */
public class ConfiguratorItem extends Item {

    public ConfiguratorItem(Properties properties) {
        super(properties);
    }

    private static PipeResourceType selectedType(ItemStack stack) {
        int ordinal = stack.getOrDefault(ModDataComponents.SELECTED_PIPE_TYPE.get(), 0);
        return PipeResourceType.VALUES[Math.floorMod(ordinal, PipeResourceType.VALUES.length)];
    }

    private static PipeResourceType cycleSelectedType(ItemStack stack) {
        int next = Math.floorMod(stack.getOrDefault(ModDataComponents.SELECTED_PIPE_TYPE.get(), 0) + 1,
                PipeResourceType.VALUES.length);
        stack.set(ModDataComponents.SELECTED_PIPE_TYPE.get(), next);
        return PipeResourceType.VALUES[next];
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (player != null && player.isShiftKeyDown()) {
            if (level.getBlockEntity(pos) instanceof UniversalPipeBlockEntity) {
                // Reserved for the config GUI (client slice); only cycle when sneaking on other blocks.
                return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                PipeResourceType type = cycleSelectedType(context.getItemInHand());
                serverPlayer.sendSystemMessage(Component.translatable(
                        "item.nerospace.configurator.selected", type.label()));
            }
            return InteractionResult.SUCCESS;
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof UniversalPipeBlockEntity pipe) {
            Direction face = context.getClickedFace();
            PipeResourceType type = selectedType(context.getItemInHand());
            PipeIoMode mode = pipe.cycleMode(face, type);
            serverPlayer.sendSystemMessage(Component.translatable(
                    "item.nerospace.configurator.face", type.label(), face.getName(),
                    Component.translatable("pipe.nerospace.mode." + mode.getSerializedName())));
            return InteractionResult.SUCCESS;
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                PipeResourceType type = cycleSelectedType(player.getItemInHand(hand));
                serverPlayer.sendSystemMessage(Component.translatable(
                        "item.nerospace.configurator.selected", type.label()));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
