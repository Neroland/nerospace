package za.co.neroland.nerospace.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.rocket.StationRegistry;

/** Core's per-player export reuses {@code /nerospace privacy export}: null when nothing is stored, offline-safe. */
class NerospaceCoreExportTest {

    @Test
    void aPlayerWithNothingStoredExportsNothing() {
        UUID me = UUID.randomUUID();
        StationRegistry stations = new StationRegistry();
        stations.found("Someone else's", UUID.randomUUID().toString());
        JsonObject doc = NerospacePrivacyExport.build(me, Set.of(), stations.all(), "nerospace:station", Set.of(),
                List.of(), List.of(), List.of(), null, null, List.of());
        assertTrue(NerospacePrivacyExport.storesNothing(doc));
    }

    @Test
    void anOfflinePlayersStationStillCounts() {
        UUID me = UUID.randomUUID();
        StationRegistry stations = new StationRegistry();
        stations.found("Mine", me.toString());
        // Offline: no oxygen and no Star Guide (attachments are reachable only through a live player).
        JsonObject doc = NerospacePrivacyExport.build(me, Set.of(), stations.all(), "nerospace:station", Set.of(),
                List.of(), List.of(), List.of(), null, null, List.of());
        assertFalse(NerospacePrivacyExport.storesNothing(doc));
        assertFalse(doc.has("oxygen"));
    }
}
