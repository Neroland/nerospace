package za.co.neroland.nerospace.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.menu.StationCharterMenu;
import za.co.neroland.nerospace.progression.StarGuideGrants;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.rocket.PadRegistry;
import za.co.neroland.nerospace.rocket.StationCoreBlockEntity;
import za.co.neroland.nerospace.rocket.StationRegistry;
import za.co.neroland.nerospace.rocket.StationStructure;
import za.co.neroland.nerospace.telemetry.NerospaceTelemetry;

/**
 * The Station Charter — founds a player station. Right-click opens a naming console; type a name and
 * confirm to allocate the next station slot in the {@code nerospace:station} void dimension and build the
 * station there (enclosed room, airlock, Tier-2 landing pad, and an anchored unbreakable Station Core
 * bound to the name). It does <b>not</b> teleport you — fly a rocket to the Orbital Station to visit, so
 * the rocket stays the means of travel. The name persists with the Station Core.
 *
 * <p>The naming screen sends the name back as a {@code FoundStationPayload}; {@link #foundFromUi} does the
 * server-side founding (consuming one charter).</p>
 */
public class StationCharterItem extends Item {

    public StationCharterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new StationCharterMenu(id, inv),
                    Component.translatable("gui.nerospace.station_charter.title")));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Server-side founding from the naming screen: allocate the station slot, build it, consume one
     * charter from the player's hands. No teleport.
     */
    public static void foundFromUi(ServerPlayer player, String name) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        ServerLevel station = server.getLevel(ModDimensions.STATION_LEVEL);
        if (station == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!held.is(ModItems.STATION_CHARTER.get())) {
            held = player.getOffhandItem();
            if (!held.is(ModItems.STATION_CHARTER.get())) {
                return; // no charter to spend
            }
        }
        StationRegistry registry = StationRegistry.get(server);
        if (registry.isFull()) {
            player.sendSystemMessage(Component.translatable("item.nerospace.station_charter.full"));
            return;
        }
        String trimmed = name == null ? "" : name.trim();
        StationRegistry.StationEntry entry =
                registry.found(trimmed.isBlank() ? null : trimmed, player.getUUID().toString());
        if (entry == null) {
            player.sendSystemMessage(Component.translatable("item.nerospace.station_charter.full"));
            return;
        }
        held.shrink(1);
        StationStructure.build(station, entry.center());
        NerospaceTelemetry.breadcrumb("station", "founded slot=" + entry.slot());
        StarGuideGrants.grant(player, "guide/station_charter");
        // Founding an off-world base opens Neroland Core's shared FIRST_COLONY gate (one-directional).
        StarGuideGrants.driveFirstColony(player);
        player.sendSystemMessage(Component.translatable("item.nerospace.station_charter.founded", entry.name()));
    }

    /** Server-side rename (from a Station Core's rename screen): updates registry + Core + landing pad. */
    public static void renameStation(ServerPlayer player, int slot, String name) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isBlank()) {
            return;
        }
        StationRegistry registry = StationRegistry.get(server);
        if (!StationRegistry.canManage(registry.get(slot), player)) {
            player.sendSystemMessage(Component.translatable("item.nerospace.station_charter.not_owner"));
            return;
        }
        StationRegistry.StationEntry entry = registry.rename(slot, trimmed);
        if (entry == null) {
            return;
        }
        ServerLevel station = server.getLevel(ModDimensions.STATION_LEVEL);
        if (station != null) {
            if (station.getBlockEntity(entry.center()) instanceof StationCoreBlockEntity core) {
                core.bindStation(slot, trimmed);
            }
            PadRegistry.get(server).register(trimmed + " Landing", ModDimensions.STATION_LEVEL,
                    StationStructure.padCenter(entry.center()));
        }
        player.sendSystemMessage(Component.translatable("item.nerospace.station_charter.renamed", trimmed));
    }
}
