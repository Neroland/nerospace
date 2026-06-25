package za.co.neroland.nerospace.rocket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * The server-global registry of player-founded stations. Stored on the overworld (always loaded) via
 * the same {@link SavedDataType} codec pattern as {@code OxygenFieldManager}. All stations live in the
 * single {@code nerospace:station} void dimension at well-separated X offsets; slot numbers are never
 * reused, so a new station can never be founded inside an abandoned hull.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> entries deliberately store NO player identity — no names, no UUIDs,
 * no founder field. Stations are server-global and usable by everyone, so nothing personal is ever
 * written to disk.</p>
 *
 * <p>Cross-loader port: identical to the standalone mod except the {@code SavedDataType} uses the
 * 4-arg NeoForm ctor and {@code org.jetbrains.annotations.Nullable}.</p>
 */
public final class StationRegistry extends SavedData {

    public static final @org.jspecify.annotations.NonNull Identifier ID = NerospaceCommon.id("stations");

    /** Hard cap on founded stations (a full X-row of ~262k blocks; lift post-1.0 if ever hit). */
    public static final int MAX_STATIONS = 64;

    /** X spacing between station slots — far beyond any render/simulation distance. */
    public static final int SLOT_SPACING = 4096;

    /** The Y level every platform is built at (matches the origin public platform). */
    public static final int PLATFORM_Y = 64;

    public static final @NonNull SavedDataType<StationRegistry> TYPE = new SavedDataType<>(
            ID, StationRegistry::new, codec(), DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    /** One founded station. The name comes from the founding charter (or an auto "Station N"). */
    public record StationEntry(int slot, String name, BlockPos center) {

        public static final @NonNull Codec<StationEntry> CODEC = NerospaceCommon.requireNonNull(
                RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.fieldOf("slot").forGetter(StationEntry::slot),
                Codec.STRING.fieldOf("name").forGetter(StationEntry::name),
                BlockPos.CODEC.fieldOf("center").forGetter(StationEntry::center)
        ).apply(inst, StationEntry::of)));

        /** Boxed-parameter factory for the codec (avoids the ECJ unboxing null-safety warning). */
        private static @NonNull StationEntry of(Integer slot, String name, BlockPos center) {
            return new StationEntry(NerospaceCommon.requireNonNull(slot).intValue(),
                    NerospaceCommon.requireNonNull(name), NerospaceCommon.requireNonNull(center));
        }
    }

    /** Insertion-ordered (founding order) — the UI cycles stations in this order. */
    private final Map<Integer, StationEntry> stations = new LinkedHashMap<>();
    private int nextSlot;

    public StationRegistry() {
    }

    private static @NonNull Codec<StationRegistry> codec() {
        return NerospaceCommon.requireNonNull(RecordCodecBuilder.create(inst -> inst.group(
                StationEntry.CODEC.listOf().fieldOf("stations")
                        .forGetter(r -> new ArrayList<>(r.stations.values())),
                Codec.INT.fieldOf("next_slot").forGetter(r -> r.nextSlot)
        ).apply(inst, StationRegistry::fromEntries)));
    }

    private static @NonNull StationRegistry fromEntries(List<StationEntry> entries, Integer nextSlot) {
        StationRegistry registry = new StationRegistry();
        for (StationEntry entry : NerospaceCommon.requireNonNull(entries)) {
            registry.stations.put(entry.slot(), entry);
        }
        registry.nextSlot = NerospaceCommon.requireNonNull(nextSlot).intValue();
        return registry;
    }

    /** The one registry, stored on the overworld so it is always loaded. */
    public static StationRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Where station slot {@code i} sits in the station dimension (the origin platform is slot −1). */
    public static BlockPos centerFor(int slot) {
        return new BlockPos(SLOT_SPACING * (slot + 1), PLATFORM_Y, 0);
    }

    /**
     * Founds a new station: allocates the next slot (never reused), registers and returns the entry —
     * or {@code null} when {@link #MAX_STATIONS} is reached. A blank name auto-names "Station N".
     */
    @Nullable
    public StationEntry found(@Nullable String name) {
        if (this.stations.size() >= MAX_STATIONS) {
            return null;
        }
        int slot = this.nextSlot++;
        String stationName = name == null || name.isBlank() ? "Station " + (slot + 1) : name;
        StationEntry entry = new StationEntry(slot, stationName, centerFor(slot));
        this.stations.put(slot, entry);
        setDirty();
        return entry;
    }

    /** Unregisters {@code slot}; @return the removed entry, or {@code null} if it wasn't registered. */
    @Nullable
    public StationEntry unregister(int slot) {
        StationEntry removed = this.stations.remove(slot);
        if (removed != null) {
            setDirty();
        }
        return removed;
    }

    @Nullable
    public StationEntry get(int slot) {
        return this.stations.get(slot);
    }

    /** All stations in founding order (the UI's cycle order). */
    public List<StationEntry> all() {
        return List.copyOf(this.stations.values());
    }

    public int count() {
        return this.stations.size();
    }

    public boolean isFull() {
        return this.stations.size() >= MAX_STATIONS;
    }

    /** The slot after {@code currentSlot} in founding order (wraps; −1/none starts at the first). */
    public int nextSlotAfter(int currentSlot) {
        List<StationEntry> ordered = all();
        if (ordered.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).slot() == currentSlot) {
                return ordered.get((i + 1) % ordered.size()).slot();
            }
        }
        return ordered.get(0).slot();
    }
}
