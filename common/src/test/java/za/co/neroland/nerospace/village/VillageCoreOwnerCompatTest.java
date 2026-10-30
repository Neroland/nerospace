package za.co.neroland.nerospace.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** A 1.6.0 Village Core keeps its owner UUID, loses {@code OwnerName}, and erasure clears the owner. */
class VillageCoreOwnerCompatTest {

    private static final UUID OWNER = UUID.fromString("0f3c2a8e-5a8d-4d3b-9a43-6c1e2f7b9d10");
    private static final UUID OTHER = UUID.fromString("7b1e9c44-2f0a-4b6e-8d55-1a2b3c4d5e6f");

    /** The two owner keys exactly as a 1.6.0 core wrote them. */
    private static Map<String, String> legacyTag() {
        Map<String, String> tag = new LinkedHashMap<>();
        tag.put("Owner", OWNER.toString());
        tag.put("OwnerName", "SomePlayer");
        return tag;
    }

    @Test
    void loadKeepsTheUuidAndIgnoresTheName() {
        VillageCoreOwnership o = new VillageCoreOwnership();
        Map<String, String> tag = legacyTag();
        o.read(k -> tag.getOrDefault(k, ""), id -> false);
        assertEquals(OWNER, o.owner());
        assertTrue(o.isOwner(OWNER));
    }

    @Test
    void saveNeverWritesOwnerName() {
        VillageCoreOwnership o = new VillageCoreOwnership();
        Map<String, String> tag = legacyTag();
        o.read(k -> tag.getOrDefault(k, ""), id -> false);
        Map<String, String> out = new LinkedHashMap<>();
        o.write(out::put);
        assertEquals(Map.of("Owner", OWNER.toString()), out);
        assertFalse(out.containsKey(VillageCoreOwnership.LEGACY_NAME_KEY));
    }

    @Test
    void forgetClearsOnlyTheMatchingOwner() {
        VillageCoreOwnership o = new VillageCoreOwnership();
        o.claim(OWNER);
        assertFalse(o.forget(OTHER));
        assertEquals(OWNER, o.owner());
        assertTrue(o.forget(OWNER));
        assertNull(o.owner());
        assertFalse(o.isClaimed());
        Map<String, String> out = new LinkedHashMap<>();
        o.write(out::put);
        assertEquals(Map.of("Owner", ""), out);
    }

    @Test
    void ownerErasedThisSessionIsDroppedOnLoad() {
        VillageCoreOwnership o = new VillageCoreOwnership();
        Map<String, String> tag = legacyTag();
        o.read(k -> tag.getOrDefault(k, ""), OWNER::equals);
        assertNull(o.owner());
    }

    @Test
    void unclaimedAndMalformedOwnersLoadAsUnclaimed() {
        VillageCoreOwnership o = new VillageCoreOwnership();
        o.read(k -> "", id -> false);
        assertFalse(o.isClaimed());
        o.read(k -> k.equals("Owner") ? "not-a-uuid" : "", id -> false);
        assertFalse(o.isClaimed());
    }
}
