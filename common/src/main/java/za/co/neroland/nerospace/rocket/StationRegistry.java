package za.co.neroland.nerospace.rocket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.data.SavedDataRecovery;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.world.station.DockingPolicy;
import za.co.neroland.nerospace.world.station.ModuleEntry;
import za.co.neroland.nerospace.world.station.ModuleKind;
import za.co.neroland.nerospace.world.station.StationAccess;
import za.co.neroland.nerospace.world.station.StationClass;

/**
 * The server-global registry of player-founded stations. Stored on the overworld (always loaded) via
 * the same {@link SavedDataType} codec pattern as {@code OxygenFieldManager}. All stations live in the
 * single {@code nerospace:station} void dimension at well-separated X offsets; slot numbers are never
 * reused, so a new station can never be founded inside an abandoned hull.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> each entry stores the founder's UUID and the UUIDs of up to 16 members the
 * owner added, solely to gate management and use. Those identifiers are kept locally in world save data only —
 * never logged, never sent in telemetry, and sent to a client only in the Station Console payload for a viewer who
 * passes {@code canUse}. Erasure blanks the owner and strips the UUID from every member list
 * ({@link #forgetPlayer}).</p>
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

    /** Most modules a station holds, the two core kinds excluded. */
    public static final int MAX_MODULES = 8;

    /** A station's claim box half-width around its centre (slots are 4096 apart). */
    public static final int CLAIM_RADIUS = 48;

    public static final SavedDataType<StationRegistry> TYPE = new SavedDataType<>(
            ID, StationRegistry::new, codec(), null);

    /**
     * One founded station. The four 1.x fields are unchanged; every 2.0 field is
     * optional on disk with a default, so a 1.x {@code nerospace:stations} store loads as-is: class
     * {@code WAYSTATION}, no members, no modules, founding day and class-change time {@code -1}, docking
     * {@code OPEN}. The owner and the member list are UUID strings, stored locally for access control only — never
     * logged, telemetered, or sent to players who fail {@code canUse}. An empty owner means unowned (legacy or
     * anonymised by erasure) — anyone may manage those.
     */
    public record StationEntry(int slot, String name, BlockPos center, String owner, StationClass stationClass,
            List<String> members, List<ModuleEntry> modules, long foundedAt, long classChangedAt,
            DockingPolicy docking) {

        public static final Codec<StationEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.fieldOf("slot").forGetter(StationEntry::slot),
                Codec.STRING.fieldOf("name").forGetter(StationEntry::name),
                BlockPos.CODEC.fieldOf("center").forGetter(StationEntry::center),
                Codec.STRING.optionalFieldOf("owner", "").forGetter(StationEntry::owner),
                StationClass.CODEC.optionalFieldOf("class", StationClass.WAYSTATION)
                        .forGetter(StationEntry::stationClass),
                Codec.STRING.listOf().optionalFieldOf("members", List.of()).forGetter(StationEntry::members),
                ModuleEntry.LIST_CODEC.optionalFieldOf("modules", List.of()).forGetter(StationEntry::modules),
                Codec.LONG.optionalFieldOf("founded_at", -1L).forGetter(StationEntry::foundedAt),
                Codec.LONG.optionalFieldOf("class_changed_at", -1L).forGetter(StationEntry::classChangedAt),
                DockingPolicy.CODEC.optionalFieldOf("docking", DockingPolicy.OPEN).forGetter(StationEntry::docking)
        ).apply(inst, StationEntry::of));

        public StationEntry {
            owner = owner == null ? "" : owner;
            stationClass = stationClass == null ? StationClass.WAYSTATION : stationClass;
            members = members == null ? List.of() : List.copyOf(members);
            modules = modules == null ? List.of() : List.copyOf(modules);
            docking = docking == null ? DockingPolicy.OPEN : docking;
        }

        /** The 1.x four-field shape; every 2.0 field takes its default. Kept public ([COMPAT]). */
        public StationEntry(int slot, String name, BlockPos center, String owner) {
            this(slot, name, center, owner, StationClass.WAYSTATION, List.of(), List.of(), -1L, -1L,
                    DockingPolicy.OPEN);
        }

        /** Factory for the 1.x shape with 2.0 defaults. */
        public static StationEntry legacy(int slot, String name, BlockPos center, String owner) {
            return new StationEntry(slot, name, center, owner);
        }

        /** Boxed-parameter factory for the codec (avoids the ECJ unboxing null-safety warning). */
        private static StationEntry of(Integer slot, String name, BlockPos center, String owner, StationClass cls,
                List<String> members, List<ModuleEntry> modules, Long foundedAt, Long classChangedAt,
                DockingPolicy docking) {
            return new StationEntry(slot.intValue(), name, center, owner, cls, members, modules,
                    foundedAt.longValue(), classChangedAt.longValue(), docking);
        }

        public StationEntry withName(String newName) {
            return new StationEntry(this.slot, newName, this.center, this.owner, this.stationClass, this.members,
                    this.modules, this.foundedAt, this.classChangedAt, this.docking);
        }

        public StationEntry withOwner(String newOwner) {
            return new StationEntry(this.slot, this.name, this.center, newOwner, this.stationClass, this.members,
                    this.modules, this.foundedAt, this.classChangedAt, this.docking);
        }

        /** A class change stamped with the overworld game time it happened at (the cooldown source). */
        public StationEntry withClass(StationClass cls, long gameTime) {
            return new StationEntry(this.slot, this.name, this.center, this.owner, cls, this.members, this.modules,
                    this.foundedAt, gameTime, this.docking);
        }

        public StationEntry withMembers(List<String> newMembers) {
            return new StationEntry(this.slot, this.name, this.center, this.owner, this.stationClass, newMembers,
                    this.modules, this.foundedAt, this.classChangedAt, this.docking);
        }

        public StationEntry withModules(List<ModuleEntry> newModules) {
            return new StationEntry(this.slot, this.name, this.center, this.owner, this.stationClass, this.members,
                    newModules, this.foundedAt, this.classChangedAt, this.docking);
        }

        public StationEntry withDocking(DockingPolicy policy) {
            return new StationEntry(this.slot, this.name, this.center, this.owner, this.stationClass, this.members,
                    this.modules, this.foundedAt, this.classChangedAt, policy);
        }

        public StationEntry withFoundedAt(long epochDay) {
            return new StationEntry(this.slot, this.name, this.center, this.owner, this.stationClass, this.members,
                    this.modules, epochDay, this.classChangedAt, this.docking);
        }

        public boolean isOwned() {
            return !this.owner.isEmpty();
        }

        /** The module entry of {@code kind}/{@code index}, or {@code null}. */
        @Nullable
        public ModuleEntry module(ModuleKind kind, int index) {
            for (ModuleEntry m : this.modules) {
                if (m.kind() == kind && m.index() == index) {
                    return m;
                }
            }
            return null;
        }

        public boolean hasModule(ModuleKind kind) {
            for (ModuleEntry m : this.modules) {
                if (m.kind() == kind) {
                    return true;
                }
            }
            return false;
        }

        public int countOf(ModuleKind kind) {
            int n = 0;
            for (ModuleEntry m : this.modules) {
                if (m.kind() == kind) {
                    n++;
                }
            }
            return n;
        }

        /** Modules counting toward the eight-module cap (both core kinds excluded). */
        public int capCount() {
            int n = 0;
            for (ModuleEntry m : this.modules) {
                if (m.kind().countsTowardCap()) {
                    n++;
                }
            }
            return n;
        }
    }

    /** Whether {@code player} is an operator (permission level 2+), through the same predicate commands use. */
    public static boolean isOp(net.minecraft.server.level.ServerPlayer player) {
        return net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS)
                .test(player.createCommandSourceStack());
    }

    /**
     * Owner, operator (permission level 2+,), or anyone on an unowned/legacy station — may
     * rename, re-class, manage members, build and abandon. Callers log an operator override through
     * {@link za.co.neroland.nerospace.world.station.StationAudit}. Null entry → false.
     */
    public static boolean canManage(@Nullable StationEntry entry, net.minecraft.server.level.ServerPlayer player) {
        if (entry == null) {
            return false;
        }
        return StationAccess.canManage(entry.owner(), player.getUUID().toString(), isOp(player));
    }

    /** Owner, member, operator, or anyone on an unowned station — the console's use actions. */
    public static boolean canUse(@Nullable StationEntry entry, net.minecraft.server.level.ServerPlayer player) {
        if (entry == null) {
            return false;
        }
        return StationAccess.canUse(entry.owner(), entry.members(), player.getUUID().toString(), isOp(player));
    }

    /** Insertion-ordered (founding order) — the UI cycles stations in this order. */
    private final Map<Integer, StationEntry> stations = new LinkedHashMap<>();
    private int nextSlot;

    public StationRegistry() {
    }

    /** The store codec (public for the migration test; the store id and shape are unchanged since 1.x). */
    public static Codec<StationRegistry> codec() {
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
        return SavedDataRecovery.get(server.overworld(), TYPE, StationRegistry::new, "nerospace:stations");
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
        StationEntry entry = new StationEntry(slot, stationName, centerFor(slot), owner == null ? "" : owner)
                .withFoundedAt(java.time.LocalDate.now(java.time.ZoneOffset.UTC).toEpochDay());
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
        StationEntry renamed = existing.withName(name);
        this.stations.put(slot, renamed);
        setDirty();
        return renamed;
    }

    /**
     * POPIA/GDPR erasure: blanks the owner wherever it is {@code uuid} and removes {@code uuid} from the
     * member list of <b>every</b> station, keeping the physical stations (shared content) with their class, modules
     * and founding day. Afterwards an anonymised station is unowned (anyone may manage it). Never logs the UUID.
     * @return how many station entries were touched.
     */
    public int forgetPlayer(java.util.UUID uuid) {
        String target = uuid.toString();
        int changed = 0;
        for (Map.Entry<Integer, StationEntry> e : this.stations.entrySet()) {
            StationEntry entry = e.getValue();
            StationEntry updated = entry;
            if (target.equals(updated.owner())) {
                updated = updated.withOwner("");
            }
            if (updated.members().contains(target)) {
                List<String> kept = new ArrayList<>(updated.members());
                kept.removeIf(target::equals);
                updated = updated.withMembers(kept);
            }
            if (updated != entry) {
                e.setValue(updated);
                changed++;
            }
        }
        if (changed > 0) {
            setDirty();
        }
        return changed;
    }

    /** Whether any station still names {@code uuid} as owner or member (erasure conformance). */
    public boolean retainsData(java.util.UUID uuid) {
        String target = uuid.toString();
        for (StationEntry entry : this.stations.values()) {
            if (target.equals(entry.owner()) || entry.members().contains(target)) {
                return true;
            }
        }
        return false;
    }

    /** Replaces a registered entry (same slot) and marks the store dirty; unknown slots are ignored. */
    @Nullable
    public StationEntry put(StationEntry entry) {
        if (!this.stations.containsKey(entry.slot())) {
            return null;
        }
        this.stations.put(entry.slot(), entry);
        setDirty();
        return entry;
    }

    /**
     * Adds a member (the caller has already checked {@code canManage} and that the player is online): refuses the
     * owner, a duplicate and a 17th member. @return whether the list changed.
     */
    public boolean addMember(int slot, String uuid) {
        StationEntry entry = this.stations.get(slot);
        if (entry == null || StationAccess.canAddMember(entry.owner(), entry.members(), uuid, true)
                != StationAccess.AddVerdict.OK) {
            return false;
        }
        List<String> next = new ArrayList<>(entry.members());
        next.add(uuid);
        put(entry.withMembers(next));
        return true;
    }

    /** Removes a member; idempotent. @return whether the list changed. */
    public boolean removeMember(int slot, String uuid) {
        StationEntry entry = this.stations.get(slot);
        if (entry == null || !entry.members().contains(uuid)) {
            return false;
        }
        List<String> next = new ArrayList<>(entry.members());
        next.removeIf(uuid::equals);
        put(entry.withMembers(next));
        return true;
    }

    /** Appends a module entry unless it already exists or the eight-module cap is reached. @return added. */
    public boolean addModule(int slot, ModuleEntry module) {
        StationEntry entry = this.stations.get(slot);
        if (entry == null || entry.module(module.kind(), module.index()) != null) {
            return false;
        }
        if (module.kind().countsTowardCap() && entry.capCount() >= MAX_MODULES) {
            return false;
        }
        List<ModuleEntry> next = new ArrayList<>(entry.modules());
        next.add(module);
        put(entry.withModules(next));
        return true;
    }

    /** Sets the power flag on every module of {@code kind}. @return whether anything changed. */
    public boolean setModuleEnabled(int slot, ModuleKind kind, boolean on) {
        StationEntry entry = this.stations.get(slot);
        if (entry == null) {
            return false;
        }
        boolean changed = false;
        List<ModuleEntry> next = new ArrayList<>(entry.modules());
        for (int i = 0; i < next.size(); i++) {
            ModuleEntry m = next.get(i);
            if (m.kind() == kind && m.enabled() != on) {
                next.set(i, m.withEnabled(on));
                changed = true;
            }
        }
        if (changed) {
            put(entry.withModules(next));
        }
        return changed;
    }

    /**
     * The station whose slot band holds {@code pos} in the station dimension (each slot owns a
     * {@link #SLOT_SPACING}-wide X band), or {@code null} off-station or for the origin platform.
     */
    @Nullable
    public StationEntry entryAt(ResourceKey<Level> dim, BlockPos pos) {
        if (!ModDimensions.STATION_LEVEL.equals(dim)) {
            return null;
        }
        for (StationEntry entry : this.stations.values()) {
            if (Math.abs(entry.center().getX() - pos.getX()) <= SLOT_SPACING / 2) {
                return entry;
            }
        }
        return null;
    }

    /** Whether {@code pos} lies in a station's claim box (centre ± {@link #CLAIM_RADIUS}, any Y). */
    public static boolean inClaimBox(StationEntry entry, BlockPos pos) {
        return Math.abs(entry.center().getX() - pos.getX()) <= CLAIM_RADIUS
                && Math.abs(entry.center().getZ() - pos.getZ()) <= CLAIM_RADIUS;
    }

    /**
     * Whether the module covering {@code pos} (if any) is switched on: false only inside the box of a
     * disabled module of the slot {@code pos} falls in. A cheap box test over at most eight entries.
     */
    public boolean moduleEnabledAt(ResourceKey<Level> dim, BlockPos pos) {
        StationEntry entry = entryAt(dim, pos);
        if (entry == null) {
            return true;
        }
        for (ModuleEntry m : entry.modules()) {
            if (!m.enabled() && za.co.neroland.nerospace.world.station.ModuleTemplates
                    .box(m.kind(), m.index(), entry.center()).contains(pos)) {
                return false;
            }
        }
        return true;
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

    /**
     * The founder UUID of the station whose footprint contains {@code pos} in {@code dim}, or {@code null}
     * when {@code pos} is outside the station dimension, in an unowned/legacy station, or no station claims
     * it. Used as a graceful operator fallback for an unattended in-station meteor grinder. Each station
     * occupies a {@link #SLOT_SPACING}-wide X band centred on its slot, so the owning slot is found by
     * proximity on X. Never logs the UUID (POPIA/GDPR).
     */
    @Nullable
    public java.util.UUID ownerAt(ResourceKey<Level> dim, BlockPos pos) {
        if (!ModDimensions.STATION_LEVEL.equals(dim)) {
            return null;
        }
        for (StationEntry entry : this.stations.values()) {
            if (entry.owner() == null || entry.owner().isEmpty()) {
                continue;
            }
            if (Math.abs(entry.center().getX() - pos.getX()) <= SLOT_SPACING / 2) {
                try {
                    return java.util.UUID.fromString(entry.owner());
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
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
