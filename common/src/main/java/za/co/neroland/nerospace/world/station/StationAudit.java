package za.co.neroland.nerospace.world.station;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import za.co.neroland.nerospace.telemetry.NerospaceTelemetry;

/**
 * Operator-override audit log. Called <b>only</b> when an operator acted on a station
 * they neither own nor belong to. The line carries the action and the station's slot number — never the actor, a
 * member, the station's name or a position (POPIA/GDPR).
 */
public final class StationAudit {

    /** The actions an operator override can perform. */
    public enum Action {
        RENAME, REMOVE, CLASS, DOCKING, MEMBER_ADD, MEMBER_REMOVE, MODULE_BUILD, MODULE_REPAIR, MODULE_TOGGLE, CLAIM,
        CLASS_COOLDOWN_BYPASS, VISITOR_SEND_HOME, ABANDON;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("Nerospace");

    private StationAudit() {
    }

    /** The log line body — pure, so a test can assert it carries nothing that identifies anyone. */
    public static String format(Action action, int slot) {
        return "[nerospace/station] op action=" + action.id() + " slot=" + slot;
    }

    /** Writes the INFO line and a telemetry breadcrumb with the same two values. */
    public static void op(Action action, int slot) {
        String line = format(action, slot);
        LOGGER.info(line);
        NerospaceTelemetry.breadcrumb("station", "op " + action.id() + " slot=" + slot);
    }
}
