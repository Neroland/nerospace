package za.co.neroland.nerospace.suit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.mojang.serialization.JsonOps;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SuitLoadoutCodecTest {

    @Test
    @DisplayName("round trip keeps frame, weave, modules and an unknown module id")
    void roundTrip() {
        SuitLoadout in = new SuitLoadout(2, SuitIds.THERMAL_WEAVE, List.of(SuitIds.RAD_LINING),
                List.of(SuitIds.SOLAR_SKIN, "nerospace:from_the_future"), 1, 0x336699, 2);
        SuitLoadout out = SuitLoadout.CODEC.decode(JsonOps.INSTANCE, SuitLoadout.CODEC.encodeStart(JsonOps.INSTANCE, in)
                .getOrThrow()).getOrThrow().getFirst();
        assertEquals(in, out);
    }

    @Test
    @DisplayName("an unknown frame ordinal becomes Mk I; an empty component is Mk I")
    void defaults() {
        SuitLoadout unknown = SuitLoadout.normalized(9, "", List.of(), List.of("nerospace:kept"), 0, -1, 0);
        assertEquals(0, unknown.frame());
        assertEquals(List.of("nerospace:kept"), unknown.modules());
        SuitLoadout empty = SuitLoadout.CODEC.decode(JsonOps.INSTANCE,
                com.google.gson.JsonParser.parseString("{}")).getOrThrow().getFirst();
        assertEquals(SuitLoadout.EMPTY.frame(), empty.frame());
        assertEquals(SuitFrame.MK1, empty.frameEnum());
        assertTrue(empty.modules().isEmpty());
    }
}
