package za.co.neroland.nerospace.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.rocket.PadKind;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.planet.WeatherState;
import za.co.neroland.nerospace.world.station.DockingPolicy;
import za.co.neroland.nerospace.world.station.StationClass;

/** Unknown saved names still decode to the old default. The warning is a side effect, not a behaviour change. */
class LenientDecodeTest {

    @Test
    void token() {
        assertFalse(LenientDecode.unknownSaved(null, false));
        assertFalse(LenientDecode.unknownSaved("  ", false));
        assertFalse(LenientDecode.unknownSaved("WAYSTATION", true));
        assertTrue(LenientDecode.unknownSaved("OUTPOST", false));
    }

    @Test
    @DisplayName("lenient enums keep their defaults")
    void defaults() {
        assertEquals(StationClass.WAYSTATION, StationClass.parse(null));
        assertEquals(StationClass.WAYSTATION, StationClass.parse("  "));
        assertEquals(StationClass.RESEARCH, StationClass.parse("research"));
        assertEquals(StationClass.WAYSTATION, StationClass.parse("OUTPOST"));
        assertEquals(DockingPolicy.OPEN, DockingPolicy.parse("OPEN"));
        assertEquals(DockingPolicy.MEMBERS, DockingPolicy.parse("members"));
        assertEquals(DockingPolicy.OPEN, DockingPolicy.parse("friends"));
        assertEquals(PadKind.LAUNCH_PAD, PadKind.byName(null));
        assertEquals(PadKind.DOCK, PadKind.byName("dock"));
        assertEquals(PadKind.LAUNCH_PAD, PadKind.byName("beacon"));
        assertEquals(WeatherEvent.NONE, WeatherEvent.byName("TORNADO"));
        assertEquals(WeatherEvent.FLARE, WeatherEvent.byName("FLARE"));
        assertEquals(WeatherState.CLEAR, WeatherState.byName(null));
        assertEquals(WeatherState.CLEAR, WeatherState.byName("STORM"));
    }
}
