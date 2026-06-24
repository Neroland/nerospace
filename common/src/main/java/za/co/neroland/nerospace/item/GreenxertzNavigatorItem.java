package za.co.neroland.nerospace.item;

import java.util.Set;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import za.co.neroland.nerospace.registry.ModDimensions;

/**
 * Creative travel device: right-click toggles the holder between the overworld and Greenxertz. All
 * teleport logic runs server-side so it behaves correctly on a dedicated server.
 */
public class GreenxertzNavigatorItem extends Item {

    public GreenxertzNavigatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            teleport(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    private void teleport(ServerPlayer player) {
        ServerLevel current = player.level();
        MinecraftServer server = current.getServer();

        boolean onPlanet = current.dimension().equals(ModDimensions.GREENXERTZ_LEVEL);
        ServerLevel destination = server.getLevel(onPlanet ? Level.OVERWORLD : ModDimensions.GREENXERTZ_LEVEL);
        if (destination == null) {
            return;
        }

        double x = player.getX();
        double z = player.getZ();
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        destination.getChunk(blockX >> 4, blockZ >> 4);
        int y = destination.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);

        player.teleportTo(destination, x, y + 1.0D, z, Set.of(), player.getYRot(), player.getXRot(), true);
        player.sendSystemMessage(Component.translatable(onPlanet
                ? "item.nerospace.greenxertz_navigator.return"
                : "item.nerospace.greenxertz_navigator.travel"));
    }
}
