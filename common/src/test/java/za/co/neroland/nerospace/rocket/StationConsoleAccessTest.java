package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.menu.StationConsoleMenu;

/** A visitor's console payload carries no owner and no member ids; a crew viewer gets both, ids only. */
class StationConsoleAccessTest {

    static final String OWNER = "11111111-1111-1111-1111-111111111111";
    static final String MEMBER = "22222222-2222-2222-2222-222222222222";

    @Test
    void visitorGetsNoIdentities() {
        StationConsoleMenu.Identities ids = StationConsoleMenu.identities(false, OWNER, List.of(MEMBER));
        assertTrue(ids.owner().isEmpty());
        assertTrue(ids.members().isEmpty());
    }

    @Test
    void addMemberIsBoundToTheOpenConsole() {
        assertTrue(StationConsoleMenu.boundToOpenConsole(true, 3, true, 3));
        assertFalse(StationConsoleMenu.boundToOpenConsole(false, -1, false, 3), "no console open (crafted packet)");
        assertFalse(StationConsoleMenu.boundToOpenConsole(true, 2, true, 3), "another station's console");
        assertFalse(StationConsoleMenu.boundToOpenConsole(true, 3, false, 3), "walked out of reach");
    }

    @Test
    void crewGetsIds() {
        StationConsoleMenu.Identities ids = StationConsoleMenu.identities(true, OWNER, List.of(MEMBER, "not-a-uuid"));
        assertEquals(UUID.fromString(OWNER), ids.owner().orElseThrow());
        assertEquals(List.of(UUID.fromString(MEMBER)), ids.members(), "malformed ids are dropped, never echoed");
        assertTrue(StationConsoleMenu.identities(true, "", List.of()).owner().isEmpty(), "unowned");
    }
}
