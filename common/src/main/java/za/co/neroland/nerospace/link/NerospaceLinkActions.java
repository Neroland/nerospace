package za.co.neroland.nerospace.link;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonObject;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerolandcore.link.LinkActionHandler;
import za.co.neroland.nerolandcore.link.LinkActionResult;
import za.co.neroland.nerolandcore.link.LinkAlerts;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.item.StationCharterItem;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.rocket.StationRegistry;

/**
 * Nerospace's write side of the NeroLink SPI — deliberately the smallest surface that is still useful.
 *
 * <p><b>What is NOT here, on purpose.</b> Nothing in this handler launches a rocket, teleports a player,
 * changes a destination, spends fuel, founds or deletes a station, or edits a block. A companion app is an
 * out-of-band channel with no line of sight to the world: an action that moved a player or committed a
 * resource from a phone would be a griefing primitive, not a convenience. Travel stays behind the rocket
 * menu, where the player is physically present.</p>
 *
 * <p>The two actions that remain are a rename (a label on something the requester already owns) and an
 * alert acknowledgement (dismissing a notification the bridge itself raised).</p>
 */
public final class NerospaceLinkActions implements LinkActionHandler {

    private static final List<String> ACTIONS = List.of(
            NerospaceLinkModule.ACTION_RENAME_STATION,
            NerospaceLinkModule.ACTION_ACKNOWLEDGE_ALERT);

    /** Station names are a UI label; keep them short enough that the naming console can render one. */
    private static final int MAX_STATION_NAME = 32;

    @Override
    public String moduleId() {
        return NerospaceLinkModule.MODULE_ID;
    }

    @Override
    public List<String> actionIds() {
        return ACTIONS;
    }

    @Override
    public boolean allowOffline(String actionId) {
        // Dismissing your own notification needs no world state. A rename touches the station's block
        // entity and pad registry, so it requires the player to actually be in the game.
        return NerospaceLinkModule.ACTION_ACKNOWLEDGE_ALERT.equals(actionId);
    }

    @Override
    public LinkActionResult execute(UUID playerId, String actionId, JsonObject params) {
        if (playerId == null) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION, "No player was supplied.");
        }
        if (!NerospaceLinkAccess.enabled()) {
            return LinkActionResult.error(LinkActionResult.Error.ACTION_DISABLED,
                    "The Nerospace link module is disabled on this server.");
        }
        MinecraftServer server = NerospaceLinkAccess.server();
        if (server == null) {
            return LinkActionResult.error(LinkActionResult.Error.INTERNAL,
                    "The server is not running a world yet.");
        }
        try {
            if (NerospaceLinkModule.ACTION_RENAME_STATION.equals(actionId)) {
                return renameStation(server, playerId, params);
            }
            if (NerospaceLinkModule.ACTION_ACKNOWLEDGE_ALERT.equals(actionId)) {
                return acknowledgeAlert(server, playerId, params);
            }
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION,
                    "Nerospace does not know the action '" + actionId + "'.");
        } catch (RuntimeException e) {
            // Action id only — never who asked (POPIA/GDPR).
            NerospaceCommon.LOGGER.warn("[Nerospace] NeroLink action '{}' failed.", actionId, e);
            return LinkActionResult.error(LinkActionResult.Error.INTERNAL,
                    "The action could not be processed.");
        }
    }

    /**
     * Renames one of the requester's own stations. The gating ladder, in order: online → the slot resolves
     * inside the requester's OWN stations (so "not yours" and "does not exist" answer identically) →
     * Nerospace's own {@code canManage} check → the name parses → the name actually changes.
     *
     * <p>The last check is not cosmetic. The in-game rename path re-registers the station's landing pad in
     * the global {@link za.co.neroland.nerospace.rocket.PadRegistry}, which is capped at 256 entries, so a
     * remote no-op rename repeated in a loop could crowd that registry out. Refusing an unchanged name
     * removes the loop. A determined owner can still churn distinct names; that is the same exposure the
     * physical naming console already carries, and it is bounded by having to be online.</p>
     */
    private static LinkActionResult renameStation(MinecraftServer server, UUID playerId, JsonObject params) {
        ServerPlayer player = NerospaceLinkAccess.online(server, playerId);
        if (player == null) {
            return LinkActionResult.error(LinkActionResult.Error.PLAYER_OFFLINE_REQUIRED,
                    "A station can only be renamed while you are online.");
        }
        StationRegistry.StationEntry entry = NerospaceLinkAccess.stationParam(server, playerId, params);
        if (entry == null) {
            return LinkActionResult.error(LinkActionResult.Error.NOT_OWNER,
                    "You do not own a station with that slot. Pass 'station' as one of your own slot numbers.");
        }
        // Re-check through the mod's own rule even though the slot came from the owned set — the bridge
        // authenticated a token, not a permission.
        if (!StationRegistry.canManage(entry, player)) {
            return LinkActionResult.error(LinkActionResult.Error.NOT_OWNER, "You cannot manage that station.");
        }

        String raw = NerospaceLinkAccess.string(params, "name");
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION,
                    "The 'name' parameter must be a non-blank station name.");
        }
        if (name.length() > MAX_STATION_NAME) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION,
                    "A station name may be at most " + MAX_STATION_NAME + " characters.");
        }
        if (name.equals(entry.name())) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION,
                    "That station is already called that.");
        }

        // The same server-side path the physical naming console uses: registry + Station Core binding +
        // landing-pad label, so a remote rename and an in-game rename cannot drift apart. That path also
        // publishes the station_state broadcast, so this action does not publish one itself.
        StationCharterItem.renameStation(player, entry.slot(), name);

        StationRegistry.StationEntry updated = StationRegistry.get(server).get(entry.slot());
        if (updated == null || !name.equals(updated.name())) {
            return LinkActionResult.error(LinkActionResult.Error.INTERNAL, "The station could not be renamed.");
        }

        JsonObject state = new JsonObject();
        state.addProperty("schema_version", NerospaceLinkModule.SCHEMA_VERSION);
        state.addProperty("station", updated.slot());
        state.addProperty("name", updated.name());
        state.addProperty("dimension", ModDimensions.STATION_LEVEL.identifier().toString());
        return LinkActionResult.ok(state);
    }

    /** Acknowledges one of the requester's own Core alerts. Scoped by Core to that player's alert list. */
    private static LinkActionResult acknowledgeAlert(MinecraftServer server, UUID playerId, JsonObject params) {
        String alertId = NerospaceLinkAccess.string(params, "alert");
        if (alertId == null) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION,
                    "The 'alert' parameter must be an alert id.");
        }
        if (!LinkAlerts.get(server).ack(server, playerId, alertId)) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION,
                    "You have no unacknowledged alert with that id.");
        }
        JsonObject state = new JsonObject();
        state.addProperty("schema_version", NerospaceLinkModule.SCHEMA_VERSION);
        state.addProperty("alert", alertId);
        state.addProperty("acked", true);
        return LinkActionResult.ok(state);
    }
}
