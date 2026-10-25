package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.world.station.StationAudit;

/** The operator audit line names the action and slot only — never a UUID, a player or a station name. */
class StationAuditTest {

    private static final Pattern UUID_LIKE = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-");

    @Test
    void lineCarriesNoIdentity() {
        for (StationAudit.Action a : StationAudit.Action.values()) {
            String line = StationAudit.format(a, 7);
            assertTrue(line.matches("\\[nerospace/station] op action=[a-z_]+ slot=7"), line);
            assertFalse(UUID_LIKE.matcher(line).find(), line);
            assertFalse(line.contains("Station "), line);
        }
    }
}
