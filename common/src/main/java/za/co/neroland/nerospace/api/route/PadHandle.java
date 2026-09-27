package za.co.neroland.nerospace.api.route;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * An immutable snapshot of one cargo endpoint: a Cargo Pad, or a founded orbital station (which counts
 * as a pad for routing and carries a negative id). <b>Public API — semver-stable.</b>
 *
 * <p>Snapshots do not update; re-query {@link RouteApi} for live data. No owner UUID is exposed —
 * ownership is answered only for a UUID the caller already holds ({@link #ownedBy}).</p>
 */
public interface PadHandle {

    /** Stable id, never reused. Negative for a station endpoint ({@code -(stationSlot + 1)}). */
    int id();

    /** Player-facing label (the pad's name tag, or "Cargo Pad N" / the station's name). */
    String name();

    /** The dimension the pad stands in. */
    ResourceKey<Level> dimension();

    /** The pad's block position (for a station: its landing-pad centre). */
    BlockPos position();

    /** Whether this endpoint is a founded station rather than a placed Cargo Pad. */
    boolean isStation();

    /** Whether the owner marked the pad public (anyone may route to it). */
    boolean isPublic();

    /**
     * Whether {@code player} owns this pad. {@code false} for an unowned (legacy or erased) pad, so an
     * erased player never reads as an owner. A station endpoint answers with the station founder rule.
     */
    boolean ownedBy(UUID player);

    /** Whether {@code player} may route cargo to this pad: owner, on the access list, or the pad is public. */
    boolean accessibleBy(UUID player);
}
