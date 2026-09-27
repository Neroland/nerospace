package za.co.neroland.nerospace.route;

import java.util.List;

import za.co.neroland.nerospace.api.route.PadHandle;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.rocket.StationRegistry;
import za.co.neroland.nerospace.rocket.StationStructure;

/**
 * A founded orbital station presented as a routing endpoint: id {@code -(slot + 1)}, position = the
 * station's Tier-2 landing-pad centre, ownership = the station founder rule. Synthesised on demand from
 * {@link StationRegistry}, never stored, so an erased founder is unowned immediately.
 */
final class StationPad {

    private StationPad() {
    }

    static PadHandle of(StationRegistry.StationEntry entry) {
        return new PadView(RouteRegistry.stationPadId(entry.slot()), entry.name(), ModDimensions.STATION_LEVEL,
                StationStructure.padCenter(entry.center()), true, false, entry.owner(), List.of());
    }
}
