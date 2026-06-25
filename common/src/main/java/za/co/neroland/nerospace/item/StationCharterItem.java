package za.co.neroland.nerospace.item;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerospace.progression.StarGuideGrants;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.rocket.StationCoreBlockEntity;
import za.co.neroland.nerospace.rocket.StationRegistry;

/**
 * The Station Charter — founds a player station. Right-click to allocate the next station slot in the
 * {@code nerospace:station} void dimension, lay a 7×7 landing pad, anchor a bound {@link
 * StationCoreBlockEntity}, and travel there. Rename the charter in an anvil to name the station;
 * breaking the Station Core unregisters it and pops the charter back (re-foundable elsewhere).
 *
 * <p>Cross-loader note: the standalone mod founds via the rocket's FOUND launch node; the multiloader
 * rocket deferred its station-selection rows, so founding is driven from the charter directly here
 * (the {@code guide/station_charter} advancement is code-granted, routing around the deferred
 * {@code ModCriteria} the same way the terraform advancements are).</p>
 */
public class StationCharterItem extends Item {

    /** 7×7 landing pad (radius 3), matching the standalone mod's station platform. */
    private static final int PLATFORM_RADIUS = 3;

    public StationCharterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        MinecraftServer server = serverPlayer.level().getServer();
        if (server == null) {
            return InteractionResult.PASS;
        }
        ServerLevel station = server.getLevel(ModDimensions.STATION_LEVEL);
        if (station == null) {
            return InteractionResult.PASS;
        }

        StationRegistry registry = StationRegistry.get(server);
        if (registry.isFull()) {
            serverPlayer.sendSystemMessage(Component.translatable("item.nerospace.station_charter.full"));
            return InteractionResult.SUCCESS;
        }

        ItemStack held = player.getItemInHand(hand);
        Component customName = held.get(DataComponents.CUSTOM_NAME);
        StationRegistry.StationEntry entry = registry.found(customName == null ? null : customName.getString());
        if (entry == null) {
            serverPlayer.sendSystemMessage(Component.translatable("item.nerospace.station_charter.full"));
            return InteractionResult.SUCCESS;
        }
        held.shrink(1);

        BlockPos centre = entry.center();
        station.getChunk(centre.getX() >> 4, centre.getZ() >> 4);
        buildStationPlatform(station, centre);
        station.setBlockAndUpdate(centre, ModBlocks.STATION_CORE.get().defaultBlockState());
        if (station.getBlockEntity(centre) instanceof StationCoreBlockEntity core) {
            core.bindStation(entry.slot(), entry.name());
        }

        serverPlayer.teleportTo(station, centre.getX() + 0.5, centre.getY() + 1.0, centre.getZ() + 0.5,
                za.co.neroland.nerospace.NerospaceCommon.requireNonNull(
                        Set.<net.minecraft.world.entity.Relative>of()),
                serverPlayer.getYRot(), serverPlayer.getXRot(), true);
        StarGuideGrants.grant(serverPlayer, "guide/station_charter");
        serverPlayer.sendSystemMessage(Component.translatable(
                "item.nerospace.station_charter.founded", entry.name()));
        return InteractionResult.SUCCESS;
    }

    /** Lay a 7×7 station-floor landing pad so the arriving rider has solid ground in the void. */
    private static void buildStationPlatform(ServerLevel level, BlockPos centre) {
        BlockState floor = ModBlocks.STATION_FLOOR.get().defaultBlockState();
        for (int dx = -PLATFORM_RADIUS; dx <= PLATFORM_RADIUS; dx++) {
            for (int dz = -PLATFORM_RADIUS; dz <= PLATFORM_RADIUS; dz++) {
                level.setBlockAndUpdate(new BlockPos(centre.getX() + dx, centre.getY(), centre.getZ() + dz), floor);
            }
        }
    }
}
