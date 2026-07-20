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

/** Persisted reversible overlay model; it never rewrites chunks or stores an actor/owner. */
@org.jetbrains.annotations.ApiStatus.Internal
public final class TerraformOverlayState extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID,
            "terraform_overlays");
    public static final SavedDataType<TerraformOverlayState> TYPE =
            new SavedDataType<>(ID, TerraformOverlayState::new, codec(), null);

    private final Map<String, Entry> entries = new LinkedHashMap<>();

    public static TerraformOverlayState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean apply(TerraformRequest request, int baselineStage, boolean authorized) {
        if (!authorized) {
            return false;
        }
        Entry previous = this.entries.get(request.id().toString());
        int baseline = previous == null ? Math.clamp(baselineStage, 0, 3) : previous.baselineStage();
        Entry next = new Entry(request.id().toString(), request.center().asLong(), request.radius(),
                baseline, request.stage(), request.progress());
        if (next.equals(previous)) {
            return false;
        }
        this.entries.put(next.id(), next);
        setDirty();
        return true;
    }

    public boolean rollback(Identifier id, boolean authorized) {
        if (!authorized || this.entries.remove(id.toString()) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    public Optional<TerraformRegion> get(Identifier id) {
        return Optional.ofNullable(this.entries.get(id.toString())).map(TerraformOverlayState::snapshot);
    }

    public Optional<TerraformRegion> at(BlockPos pos) {
        Entry best = null;
        for (Entry entry : this.entries.values()) {
            long radiusSquared = (long) entry.radius() * entry.radius();
            if (BlockPos.of(entry.center()).distSqr(pos) <= radiusSquared
                    && (best == null || entry.stage() > best.stage()
                            || entry.stage() == best.stage() && entry.progress() > best.progress())) {
                best = entry;
            }
        }
        return Optional.ofNullable(best).map(TerraformOverlayState::snapshot);
    }

    private static TerraformRegion snapshot(Entry entry) {
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
        entries.forEach(entry -> state.entries.put(entry.id(), entry));
        return state;
    }

    private List<Entry> encode() {
        return new ArrayList<>(this.entries.values());
    }
}
