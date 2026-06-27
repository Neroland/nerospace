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
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * The server-global registry of player-founded stations. Stored on the overworld (always loaded) via
 * the same {@link SavedDataType} codec pattern as {@code OxygenFieldManager}. All stations live in the
 * single {@code nerospace:station} void dimension at well-separated X offsets; slot numbers are never
 * reused, so a new station can never be founded inside an abandoned hull.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> each entry stores the founder's UUID solely to gate management
 * (rename / remove) to its founder or a server op. That identifier is kept locally in world save data
 * only — never logged, never sent in telemetry, never broadcast to other clients. Stations remain
 * server-global and usable (flyable) by everyone; only renaming/removing is owner-restricted.</p>
 *
 * <p>Cross-loader port: identical to the standalone mod except the {@code SavedDataType} uses the 4-arg
 * NeoForm ctor ({@code DataFixTypes = null}) and {@code org.jetbrains.annotations.Nullable}.</p>
 */
public final class StationRegistry extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "stations");

    /** Hard cap on founded stations (a full X-row of ~262k blocks; lift post-1.0 if ever hit). */
    public static final int MAX_STATIONS = 64;

    /** X spacing between station slots — far beyond any render/simulation distance. */
    public static final int SLOT_SPACING = 4096;

    /** The Y level every platform is built at (matches the origin public platform). */
    public static final int PLATFORM_Y = 64;

    public static final SavedDataType<StationRegistry> TYPE = new SavedDataType<>(
            ID, StationRegistry::new, codec(), null);

    /**
     * One founded station: slot, display name, centre, and the founder's UUID string ({@code owner}).
     * The owner is stored locally for access control only (who may rename / remove the station); it is
     * never logged, telemetered, or sent to other clients. An empty owner means unowned (legacy
     * stations, or those founded before ownership existed) — anyone may manage those.
     */
    public record StationEntry(int slot, String name, BlockPos center, String owner) {

        public static final Codec<StationEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.fieldOf("slot").forGetter(StationEntry::slot),
                Codec.STRING.fieldOf("name").forGetter(StationEntry::name),
                BlockPos.CODEC.fieldOf("center").forGetter(StationEntry::center),
                Codec.STRING.optionalFieldOf("owner", "").forGetter(StationEntry::owner)
        ).apply(inst, StationEntry::of));

        /** Boxed-parameter factory for the codec (avoids the ECJ unboxing null-safety warning). */
        private static StationEntry of(Integer slot, String name, BlockPos center, String owner) {
            return new StationEntry(slot.intValue(), name, center, owner);
        }
    }

    /** Whether {@code player} founded {@code entry} (or it is unowned/legacy) — i.e. may rename it. */
    public static boolean canManage(@Nullable StationEntry entry, net.minecraft.server.level.ServerPlayer player) {
        if (entry == null) {
            return false;
        }
        if (entry.owner() == null || entry.owner().isEmpty()) {
            return true; // unowned / legacy station
        }
        return entry.owner().equals(player.getUUID().toString());
    }

    /** Insertion-ordered (founding order) — the UI cycles stations in this order. */
    private final Map<Integer, StationEntry> stations = new LinkedHashMap<>();
    private int nextSlot;

    public StationRegistry() {
    }

    private static Codec<StationRegistry> codec() {
        return RecordCodecBuilder.create(inst -> inst.group(
                StationEntry.CODEC.listOf().fieldOf("stations")
                        .forGetter(r -> new ArrayList<>(r.stations.values())),
                Codec.INT.fieldOf("next_slot").forGetter(r -> r.nextSlot)
        ).apply(inst, StationRegistry::fromEntries));
    }

    private static StationRegistry fromEntries(List<StationEntry> entries, Integer nextSlot) {
        StationRegistry registry = new StationRegistry();
        for (StationEntry entry : entries) {
            registry.stations.put(entry.slot(), entry);
        }
        registry.nextSlot = nextSlot.intValue();
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
    public StationEntry found(@Nullable String name, @Nullable String owner) {
        if (this.stations.size() >= MAX_STATIONS) {
            return null;
        }
        int slot = this.nextSlot++;
        String stationName = name == null || name.isBlank() ? "Station " + (slot + 1) : name;
        StationEntry entry = new StationEntry(slot, stationName, centerFor(slot), owner == null ? "" : owner);
        this.stations.put(slot, entry);
        setDirty();
        return entry;
    }

    /** Renames {@code slot}; @return the updated entry, or {@code null} if it wasn't registered. */
    @Nullable
    public StationEntry rename(int slot, String name) {
        StationEntry existing = this.stations.get(slot);
        if (existing == null) {
            return null;
        }
        StationEntry renamed = new StationEntry(slot, name, existing.center(), existing.owner());
        this.stations.put(slot, renamed);
        setDirty();
        return renamed;
    }

    /**
     * POPIA/GDPR erasure: anonymises every station founded by {@code uuid} — clears the stored owner UUID
     * (the only player-keyed field) while keeping the physical station, which is server-global, shared
     * content. Afterwards those stations are unowned/legacy (anyone may manage them). Never logs the UUID.
     * @return how many stations were anonymised.
     */
    public int forgetPlayer(java.util.UUID uuid) {
        String target = uuid.toString();
        int changed = 0;
        for (Map.Entry<Integer, StationEntry> e : this.stations.entrySet()) {
            StationEntry entry = e.getValue();
            if (target.equals(entry.owner())) {
                e.setValue(new StationEntry(entry.slot(), entry.name(), entry.center(), ""));
                changed++;
            }
        }
        if (changed > 0) {
            setDirty();
        }
        return changed;
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
