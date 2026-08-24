package za.co.neroland.nerospace.world;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.api.TerraformRegion;
import za.co.neroland.nerospace.api.TerraformRequest;

/**
 * Persisted reversible terraforming overlay model, backing
 * {@link za.co.neroland.nerospace.api.NerospaceTerraforming}.
 *
 * <p>It never rewrites chunks and never stores an actor or owner — authorization is decided by the
 * server's claim policy before anything reaches this class, so no identity needs to be persisted and this
 * store has no erasure path. Each region remembers the <em>physical</em> terraforming stage that was in
 * place when it was created, so rolling the overlay back restores that baseline rather than zero.</p>
 *
 * <p>Fetched through {@link SavedDataRecovery} like every other Nerospace saved-data manager.</p>
 */
@org.jetbrains.annotations.ApiStatus.Internal
public final class TerraformOverlayState extends SavedData {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "terraform_overlays");

    public static final SavedDataType<TerraformOverlayState> TYPE =
            new SavedDataType<>(ID, TerraformOverlayState::new, codec(), null);

    /** Stable non-identifying label for the recovery helper's logs, telemetry and backup file name. */
    public static final String RECOVERY_NAME = "nerospace:terraform_overlays";

    private final Map<String, Entry> entries = new LinkedHashMap<>();

    /** Per-dimension: an overlay covers a region of one level. */
    public static TerraformOverlayState get(ServerLevel level) {
        return SavedDataRecovery.get(level, TYPE, TerraformOverlayState::new, RECOVERY_NAME);
    }

    /**
     * Creates or advances a region. Authorization is the caller's responsibility — by the time a request
     * reaches here the claim policy has already approved it.
     *
     * @param baselineStage the physical chunk stage to restore on rollback; only honoured when the region
     *                      is first created, so advancing a region cannot rewrite its own baseline
     */
    public boolean apply(TerraformRequest request, int baselineStage) {
        Entry previous = this.entries.get(request.id().toString());
        int baseline = previous == null
                ? Math.clamp(baselineStage, 0, TerraformRequest.MAX_STAGE)
                : previous.baselineStage();
        Entry next = new Entry(request.id().toString(), request.center().asLong(), request.radius(),
                baseline, request.stage(), request.progress());
        if (next.equals(previous)) {
            return false;
        }
        this.entries.put(next.id(), next);
        setDirty();
        return true;
    }

    /** Removes a region. Authorization is the caller's responsibility. */
    public boolean rollback(Identifier id) {
        if (this.entries.remove(id.toString()) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    public Optional<TerraformRegion> get(Identifier id) {
        return Optional.ofNullable(this.entries.get(id.toString())).map(TerraformOverlayState::snapshot);
    }

    /**
     * The most advanced region covering {@code pos} — highest stage wins, then highest progress.
     *
     * <p><b>Coverage is horizontal.</b> Only X and Z are compared, so a region reaches from bedrock to
     * build limit like a column. A spherical test would have meant an overlay quietly switching off above
     * its centre while the physical terraforming it sits alongside — a whole-chunk column flag — stayed
     * on. Terraforming is a property of the ground and the air above it, not of a bubble.</p>
     */
    public Optional<TerraformRegion> at(BlockPos pos) {
        Entry best = null;
        for (Entry entry : this.entries.values()) {
            // Unpacked in place rather than through BlockPos.of, which would allocate per entry per call.
            long dx = (long) BlockPos.getX(entry.center()) - pos.getX();
            long dz = (long) BlockPos.getZ(entry.center()) - pos.getZ();
            long radiusSquared = (long) entry.radius() * entry.radius();
            if (dx * dx + dz * dz <= radiusSquared
                    && (best == null || entry.stage() > best.stage()
                            || entry.stage() == best.stage() && entry.progress() > best.progress())) {
                best = entry;
            }
        }
        return Optional.ofNullable(best).map(TerraformOverlayState::snapshot);
    }

    /** The region's baseline physical stage, for tests and for callers reasoning about rollback. */
    public int baselineStage(Identifier id) {
        Entry entry = this.entries.get(id.toString());
        return entry == null ? 0 : entry.baselineStage();
    }

    private static TerraformRegion snapshot(Entry entry) {
        // TerraformRegion clamps, so a persisted row can never hand a consumer an out-of-range value.
        return new TerraformRegion(Identifier.parse(entry.id()), BlockPos.of(entry.center()), entry.radius(),
                entry.stage(), entry.progress());
    }

    private record Entry(String id, long center, int radius, int baselineStage, int stage, float progress) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Entry::id),
                Codec.LONG.fieldOf("center").forGetter(Entry::center),
                Codec.INT.fieldOf("radius").forGetter(Entry::radius),
                Codec.INT.fieldOf("baseline_stage").forGetter(Entry::baselineStage),
                Codec.INT.fieldOf("stage").forGetter(Entry::stage),
                Codec.FLOAT.fieldOf("progress").forGetter(Entry::progress)
        ).apply(instance, Entry::new));
    }

    public static Codec<TerraformOverlayState> codec() {
        return Entry.CODEC.listOf().optionalFieldOf("regions", List.of()).codec()
                .xmap(TerraformOverlayState::decode, TerraformOverlayState::encode);
    }

    private static TerraformOverlayState decode(List<Entry> entries) {
        TerraformOverlayState state = new TerraformOverlayState();
        for (Entry entry : entries) {
            // A region id that is not a valid Identifier would throw later, inside snapshot(), on a read
            // path with no way to recover — so reject it here instead. Identifier.tryParse returns null
            // rather than throwing, which is exactly the "skip the bad row" behaviour we want.
            if (entry.radius() < 1 || Identifier.tryParse(entry.id()) == null) {
                continue;
            }
            state.entries.put(entry.id(), entry);
        }
        return state;
    }

    private List<Entry> encode() {
        return new ArrayList<>(this.entries.values());
    }
}
