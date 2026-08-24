package za.co.neroland.nerospace.progression;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.api.PlanetId;
import za.co.neroland.nerospace.world.SavedDataRecovery;

/**
 * UUID-keyed historical planet visits, backing {@link za.co.neroland.nerospace.api.NerospaceVisits}.
 *
 * <p><b>Data minimisation (POPIA/GDPR).</b> A row is a UUID and the set of planet ids that player has
 * reached — nothing else. Deliberately absent: timestamps (which would make it a movement history),
 * coordinates, dimension entry counts and player names. The store is registered with Core's shared
 * {@code PlayerDataErasure} hook by {@code NerospaceErasure}, which also forces an immediate backup
 * rewrite so an erased UUID does not linger in the last-known-good file.</p>
 *
 * <p>Fetched through {@link SavedDataRecovery} like every other Nerospace saved-data manager, so a corrupt
 * {@code .dat} degrades to the backup (or to empty) instead of crashing the tick loop — this store is read
 * from the per-player tick, which is exactly the path MC-NEROSPACE-H turned into a repeating hard crash.</p>
 */
@org.jetbrains.annotations.ApiStatus.Internal
public final class PlanetVisitState extends SavedData {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "planet_visits");

    public static final SavedDataType<PlanetVisitState> TYPE =
            new SavedDataType<>(ID, PlanetVisitState::new, codec(), null);

    /** Stable non-identifying label for the recovery helper's logs, telemetry and backup file name. */
    public static final String RECOVERY_NAME = "nerospace:planet_visits";

    private final Map<UUID, Set<String>> visits = new LinkedHashMap<>();

    /** Overworld-scoped: visits are a server-wide fact about a player, not a per-dimension one. */
    public static PlanetVisitState get(MinecraftServer server) {
        return SavedDataRecovery.get(server.overworld(), TYPE, PlanetVisitState::new, RECOVERY_NAME);
    }

    /** Records a visit. Returns true only on a genuine first visit, which is what fires the event. */
    public boolean record(UUID player, PlanetId planet) {
        boolean changed = this.visits.computeIfAbsent(player, ignored -> new LinkedHashSet<>())
                .add(planet.asString());
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean hasVisited(UUID player, PlanetId planet) {
        return this.visits.getOrDefault(player, Set.of()).contains(planet.asString());
    }

    /** This player's planet ids, as an immutable copy — the caller never sees the live set. */
    public Set<String> export(UUID player) {
        return Set.copyOf(this.visits.getOrDefault(player, Set.of()));
    }

    /** Core erasure: drop the player's row entirely. */
    public void forget(UUID player) {
        if (this.visits.remove(player) != null) {
            setDirty();
        }
    }

    private record Entry(String player, List<String> planets) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("player").forGetter(Entry::player),
                Codec.STRING.listOf().fieldOf("planets").forGetter(Entry::planets)
        ).apply(instance, Entry::new));
    }

    public static Codec<PlanetVisitState> codec() {
        return Entry.CODEC.listOf().optionalFieldOf("visits", List.of()).codec()
                .xmap(PlanetVisitState::decode, PlanetVisitState::encode);
    }

    private static PlanetVisitState decode(List<Entry> entries) {
        PlanetVisitState state = new PlanetVisitState();
        for (Entry entry : entries) {
            try {
                state.visits.put(UUID.fromString(entry.player()), new LinkedHashSet<>(entry.planets()));
            } catch (IllegalArgumentException ignored) {
                // Malformed UUID row (hand-edited or truncated file): skip it rather than fail the whole
                // load. Never log the offending value — it is player-keyed data.
            }
        }
        return state;
    }

    private List<Entry> encode() {
        List<Entry> result = new ArrayList<>();
        this.visits.forEach((player, planets) ->
                result.add(new Entry(player.toString(), new ArrayList<>(planets))));
        return result;
    }
}
