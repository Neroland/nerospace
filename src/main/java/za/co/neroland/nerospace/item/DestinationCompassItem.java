package za.co.neroland.nerospace.item;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.rocket.Destinations;

/**
 * A creative-only travel device that teleports the holder straight to one Nerospace destination
 * (a planet or the station). Intended for testing/creative building — there is no survival recipe;
 * it only appears in the creative tab. Server-authoritative.
 */
public class DestinationCompassItem extends Item {

    private final ResourceKey<Level> destination;

    public DestinationCompassItem(Properties properties, ResourceKey<Level> destination) {
        super(properties);
        this.destination = destination;
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
        ServerLevel dest = current.getServer().getLevel(this.destination);
        if (dest == null) {
            return;
        }

        double x = player.getX();
        double z = player.getZ();
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        dest.getChunk(blockX >> 4, blockZ >> 4);

        double y;
        if (this.destination.equals(ModDimensions.STATION_LEVEL)) {
            // The station is void; drop a small platform so the player doesn't fall.
            int platformY = 64;
            BlockState floor = ModBlocks.STATION_FLOOR.get().defaultBlockState();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    dest.setBlockAndUpdate(new BlockPos(blockX + dx, platformY, blockZ + dz), floor);
                }
            }
            y = platformY + 1.0D;
        } else {
            y = dest.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ) + 1.0D;
        }

        player.teleportTo(dest, x, y, z, Set.of(), player.getYRot(), player.getXRot(), true);
        player.sendSystemMessage(Component.translatable(
                "item.nerospace.destination_compass.travel", Destinations.name(this.destination)));
    }
}
