package za.co.neroland.nerospace.world;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import za.co.neroland.nerospace.NerospaceCommon;

/** Persisted, unattributed external oxygen contributions keyed only by a caller-owned source id. */
@org.jetbrains.annotations.ApiStatus.Internal
public final class OxygenContributionState extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID,
            "oxygen_contributions");
    public static final SavedDataType<OxygenContributionState> TYPE =
            new SavedDataType<>(ID, OxygenContributionState::new, codec(), null);

    private final Map<String, Entry> entries = new LinkedHashMap<>();

    public static OxygenContributionState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean put(Identifier source, BlockPos center, int radius, int strength, long now,
            long durationTicks) {
        Entry next = new Entry(source.toString(), center.asLong(), radius, strength, now,
                Math.addExact(now, durationTicks));
        Entry previous = this.entries.put(source.toString(), next);
        boolean changed = !next.equals(previous);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean remove(Identifier source) {
        boolean changed = this.entries.remove(source.toString()) != null;
        if (changed) {
            setDirty();
        }
        return changed;
    }

    /** Bounded pressure at a loaded position, with linear time and distance decay. */
    public int pressureAt(BlockPos pos, long now) {
        prune(now);
        int total = 0;
        for (Entry entry : this.entries.values()) {
            BlockPos center = BlockPos.of(entry.center());
            double distance = Math.sqrt(center.distSqr(pos));
            if (distance > entry.radius()) {
                continue;
            }
            double timeFraction = (double) (entry.expiresAt() - now)
                    / Math.max(1L, entry.expiresAt() - entry.createdAt());
            double distanceFraction = 1.0D - distance / Math.max(1, entry.radius());
            total += (int) Math.floor(entry.strength() * timeFraction * distanceFraction);
        }
        return Math.clamp(total, 0, 15);
    }

    public int size(long now) {
        prune(now);
        return this.entries.size();
    }

    public void prune(long now) {
        if (this.entries.values().removeIf(entry -> entry.expiresAt() <= now)) {
            setDirty();
        }
    }

    private record Entry(String source, long center, int radius, int strength, long createdAt,
            long expiresAt) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("source").forGetter(Entry::source),
                Codec.LONG.fieldOf("center").forGetter(Entry::center),
                Codec.INT.fieldOf("radius").forGetter(Entry::radius),
                Codec.INT.fieldOf("strength").forGetter(Entry::strength),
                Codec.LONG.fieldOf("created_at").forGetter(Entry::createdAt),
                Codec.LONG.fieldOf("expires_at").forGetter(Entry::expiresAt)
        ).apply(instance, Entry::new));
    }

    public static Codec<OxygenContributionState> codec() {
        return Entry.CODEC.listOf().optionalFieldOf("entries", List.of()).codec()
                .xmap(OxygenContributionState::decode, OxygenContributionState::encode);
    }

    private static OxygenContributionState decode(List<Entry> entries) {
        OxygenContributionState state = new OxygenContributionState();
        entries.forEach(entry -> state.entries.put(entry.source(), entry));
        return state;
    }

    private List<Entry> encode() {
        return new ArrayList<>(this.entries.values());
    }
}
