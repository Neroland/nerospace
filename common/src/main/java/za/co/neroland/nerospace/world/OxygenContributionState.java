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

/**
 * Persisted external oxygen contributions, backing {@link za.co.neroland.nerospace.api.NerospaceOxygen}.
 *
 * <p>Keyed only by a caller-owned source id — <b>no player is recorded, not even transiently</b>, so this
 * store has (and needs) no erasure path. Every row expires on its own, so an integration that stops
 * refreshing cannot leave permanent state behind.</p>
 *
 * <p>Fetched through {@link SavedDataRecovery} like every other Nerospace saved-data manager.</p>
 */
@org.jetbrains.annotations.ApiStatus.Internal
public final class OxygenContributionState extends SavedData {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "oxygen_contributions");

    public static final SavedDataType<OxygenContributionState> TYPE =
            new SavedDataType<>(ID, OxygenContributionState::new, codec(), null);

    /** Stable non-identifying label for the recovery helper's logs, telemetry and backup file name. */
    public static final String RECOVERY_NAME = "nerospace:oxygen_contributions";

    /** Same 0-15 scale as the internal oxygen field. */
    private static final int MAX_PRESSURE = 15;

    private final Map<String, Entry> entries = new LinkedHashMap<>();

    /** Per-dimension: a contribution is a physical thing at a position in one level. */
    public static OxygenContributionState get(ServerLevel level) {
        return SavedDataRecovery.get(level, TYPE, OxygenContributionState::new, RECOVERY_NAME);
    }

    /** Stores or replaces {@code source}'s contribution. Also collects expired rows (a write path). */
    public boolean put(Identifier source, BlockPos center, int radius, int strength, long now,
            long durationTicks) {
        prune(now);
        Entry next = new Entry(source.toString(), center.asLong(), radius, strength, now,
                Math.addExact(now, durationTicks));
        Entry previous = this.entries.put(next.source(), next);
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

    /**
     * Bounded pressure at a position, with linear time and distance decay.
     *
     * <p><b>Pure read.</b> Expired rows are skipped but not collected, and nothing is marked dirty — this
     * runs on the breathability tick path, where pruning would mean a {@code setDirty} every tick for a
     * query that changed nothing. Collection happens on the write paths ({@link #put}, {@link #prune}).</p>
     */
    public int pressureAt(BlockPos pos, long now) {
        // Accumulated as a double and floored once at the end: flooring each entry first would lose most
        // of a fractional contribution, so two overlapping sources of 5.9 would read 10 instead of 11.
        double total = 0.0D;
        for (Entry entry : this.entries.values()) {
            if (entry.expiresAt() <= now) {
                continue;
            }
            // Unpacked in place rather than through BlockPos.of, which would allocate per entry per call
            // on the breathability tick path.
            double dx = (double) BlockPos.getX(entry.center()) - pos.getX();
            double dy = (double) BlockPos.getY(entry.center()) - pos.getY();
            double dz = (double) BlockPos.getZ(entry.center()) - pos.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > entry.radius()) {
                continue;
            }
            double timeFraction = (double) (entry.expiresAt() - now)
                    / Math.max(1L, entry.expiresAt() - entry.createdAt());
            double distanceFraction = 1.0D - distance / Math.max(1, entry.radius());
            total += entry.strength() * timeFraction * distanceFraction;
        }
        return Math.clamp((int) Math.floor(total), 0, MAX_PRESSURE);
    }

    /** Live (unexpired) contribution count, collecting expired rows on the way. */
    public int size(long now) {
        prune(now);
        return this.entries.size();
    }

    /** Drops expired rows. A write path: safe to mark dirty. */
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
        for (Entry entry : entries) {
            // Skip rows a hand-edited or truncated file could make nonsensical: a non-positive radius
            // would divide by zero in the decay maths, and an inverted lifetime would never expire.
            if (entry.radius() < 1 || entry.strength() < 1 || entry.expiresAt() <= entry.createdAt()) {
                continue;
            }
            state.entries.put(entry.source(), entry);
        }
        return state;
    }

    private List<Entry> encode() {
        return new ArrayList<>(this.entries.values());
    }
}
