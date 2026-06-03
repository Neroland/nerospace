package za.co.neroland.nerospace.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.pipe.PipeConnectionMode;
import za.co.neroland.nerospace.pipe.UniversalPipeBlockEntity;

/**
 * The Configurator — the network tool. Right-click a Universal Pipe face to cycle that face's
 * connection mode (auto → pull → push → disabled). Filters and upgrades will hook in here later.
 */
public class ConfiguratorItem extends Item {

    public ConfiguratorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.isClientSide() && context.getPlayer() instanceof ServerPlayer player
                && level.getBlockEntity(pos) instanceof UniversalPipeBlockEntity pipe) {
            Direction face = context.getClickedFace();
            PipeConnectionMode mode = pipe.cycleFaceMode(face);
            player.sendSystemMessage(Component.translatable(
                    "item.nerospace.configurator.face", face.getName(), mode.getSerializedName()));
            return InteractionResult.SUCCESS;
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
