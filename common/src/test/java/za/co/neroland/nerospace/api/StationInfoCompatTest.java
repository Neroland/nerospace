package za.co.neroland.nerospace.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

/**
 * NeroSecurity and NeroEvents compile against the 1.6.0 {@code StationInfo}: the five-argument constructor and the
 * five accessors must stay, by reflection exactly as a sibling sees them; the sixth component is additive.
 */
class StationInfoCompatTest {

    @Test
    void shape160IsKept() throws Exception {
        assertNotNull(StationInfo.class.getConstructor(int.class, String.class, PlanetId.class, BlockPos.class,
                int.class));
        Object[][] accessors = { { "id", int.class }, { "name", String.class }, { "planet", PlanetId.class },
                { "position", BlockPos.class }, { "routeCapacity", int.class } };
        for (Object[] a : accessors) {
            Method m = StationInfo.class.getMethod((String) a[0]);
            assertEquals(a[1], m.getReturnType(), (String) a[0]);
        }
        assertEquals(String.class, StationInfo.class.getMethod("stationClass").getReturnType());
    }

    @Test
    void fiveArgDefaultsToWaystation() {
        StationInfo info = new StationInfo(0, "Home", NerospacePlanets.STATION, BlockPos.ZERO, 8);
        assertEquals("WAYSTATION", info.stationClass());
        assertEquals(8, info.routeCapacity());
        assertEquals(NerospaceStations.DEFAULT_ROUTE_CAPACITY, 8);
    }
}
