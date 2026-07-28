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

/** UUID-keyed historical planet visits; contains no names, timestamps, or location trail. */
@org.jetbrains.annotations.ApiStatus.Internal
public final class PlanetVisitState extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "planet_visits");
    public static final SavedDataType<PlanetVisitState> TYPE =
            new SavedDataType<>(ID, PlanetVisitState::new, codec(), null);

    private final Map<UUID, Set<String>> visits = new LinkedHashMap<>();

    public static PlanetVisitState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

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

    public Set<String> export(UUID player) {
        return Set.copyOf(this.visits.getOrDefault(player, Set.of()));
    }

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
        entries.forEach(entry -> {
            try {
                state.visits.put(UUID.fromString(entry.player()), new LinkedHashSet<>(entry.planets()));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed UUID rows.
            }
        });
        return state;
    }

    private List<Entry> encode() {
        List<Entry> result = new ArrayList<>();
        this.visits.forEach((player, planets) ->
                result.add(new Entry(player.toString(), new ArrayList<>(planets))));
        return result;
    }
}
