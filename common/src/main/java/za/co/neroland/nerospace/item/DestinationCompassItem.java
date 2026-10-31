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
import za.co.neroland.nerospace.telemetry.NerospaceTelemetry;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.rocket.Destinations;
import za.co.neroland.nerospace.world.planet.nyxara.worldgen.NyxaraBand;

/**
 * A creative-only travel device that teleports the holder straight to one Nerospace destination (a
 * planet or the station). Intended for testing/creative building — no survival recipe. Server-authoritative.
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
        boolean returning = current.dimension().equals(this.destination);
        ServerLevel dest = current.getServer().getLevel(returning ? Level.OVERWORLD : this.destination);
        if (dest == null) {
            return;
        }

        double x = player.getX();
        double z = player.getZ();
        if (!returning && this.destination.equals(ModDimensions.NYXARA_LEVEL)) {
            // Nyxara: set down in the habitable twilight band, never on the scorched dayside or the frozen nightside.
            int bandX = CompassLanding.intoBand(Mth.floor(x), NyxaraBand.width(dest), NyxaraBand.originX(dest),
                    NyxaraBand.edgeNoise(dest));
            if (bandX != Mth.floor(x)) {
                x = bandX + 0.5D;
            }
        }
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        dest.getChunk(blockX >> 4, blockZ >> 4);

        double y;
        int feet = !returning && this.destination.equals(ModDimensions.STATION_LEVEL) ? -1
                : CompassLanding.feetY(dest.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ),
                        dest.getMinY());
        if (feet < 0) {
            // The station is void (and so is a groundless column): drop a small platform so the player doesn't fall.
            int platformY = CompassLanding.PLATFORM_Y;
            BlockState floor = ModBlocks.STATION_FLOOR.get().defaultBlockState();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    dest.setBlockAndUpdate(new BlockPos(blockX + dx, platformY, blockZ + dz), floor);
                }
            }
            y = platformY + 1.0D;
        } else {
            y = feet;
        }

        player.teleportTo(dest, x, y, z, Set.of(), player.getYRot(), player.getXRot(), true);
        NerospaceTelemetry.breadcrumb("travel",
                "compass dim=" + (returning ? "overworld" : Destinations.name(this.destination)));
        player.sendSystemMessage(returning
                ? Component.translatable("item.nerospace.destination_compass.return")
                : Component.translatable("item.nerospace.destination_compass.travel",
                        Destinations.displayName(this.destination)));
    }
}
