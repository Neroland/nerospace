package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The origin platform is built once: a core, a pad node or a station floor at the centre means "leave it". */
class StationStructureGuardTest {

    @Test
    void guard() {
        assertFalse(StationStructure.alreadyBuilt(false, false, false));
        assertTrue(StationStructure.alreadyBuilt(true, false, false));
        assertTrue(StationStructure.alreadyBuilt(false, true, false));
        assertTrue(StationStructure.alreadyBuilt(false, false, true));
    }
}
