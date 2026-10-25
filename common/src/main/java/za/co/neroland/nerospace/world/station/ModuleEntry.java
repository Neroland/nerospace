package za.co.neroland.nerospace.world.station;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One built station module. {@code index} is the per-kind ordinal (Solar Wing 0 is
 * north, 1 south); {@code anchor} is the module's origin block — the idempotence key; {@code enabled} is the
 * {@code canUse} power switch (absent → {@code true}).
 */
public record ModuleEntry(ModuleKind kind, int index, BlockPos anchor, boolean enabled) {

    private static final Logger LOGGER = LoggerFactory.getLogger("Nerospace");

    /** Raw on-disk shape: the kind as a string so an unknown kind can be dropped instead of failing the load. */
    private record Raw(String kind, int index, BlockPos anchor, boolean enabled) {
        static final Codec<Raw> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("kind").forGetter(Raw::kind),
                Codec.INT.optionalFieldOf("index", 0).forGetter(Raw::index),
                BlockPos.CODEC.fieldOf("anchor").forGetter(Raw::anchor),
                Codec.BOOL.optionalFieldOf("enabled", Boolean.TRUE).forGetter(Raw::enabled)
        ).apply(inst, Raw::of));

        private static Raw of(String kind, Integer index, BlockPos anchor, Boolean enabled) {
            return new Raw(kind, index.intValue(), anchor, enabled.booleanValue());
        }
    }

    /**
     * The module list codec. An entry whose kind is unknown is <b>dropped</b> (logged once, anonymously) rather than
     * defaulted — a module with the wrong kind would be repaired in the wrong shape.
     */
    public static final Codec<List<ModuleEntry>> LIST_CODEC = Raw.CODEC.listOf().xmap(ModuleEntry::fromRaw,
            ModuleEntry::toRaw);

    public ModuleEntry(ModuleKind kind, int index, BlockPos anchor) {
        this(kind, index, anchor, true);
    }

    public ModuleEntry withEnabled(boolean on) {
        return new ModuleEntry(this.kind, this.index, this.anchor, on);
    }

    private static List<ModuleEntry> fromRaw(List<Raw> raw) {
        List<ModuleEntry> out = new ArrayList<>(raw.size());
        int dropped = 0;
        for (Raw r : raw) {
            ModuleKind kind = ModuleKind.parse(r.kind());
            if (kind == null) {
                dropped++;
                continue;
            }
            out.add(new ModuleEntry(kind, r.index(), r.anchor(), r.enabled()));
        }
        if (dropped > 0) {
            LOGGER.warn("[Nerospace] Dropped {} station module entr(y/ies) of an unknown kind.",
                    dropped);
        }
        return List.copyOf(out);
    }

    private static List<Raw> toRaw(List<ModuleEntry> entries) {
        List<Raw> out = new ArrayList<>(entries.size());
        for (ModuleEntry e : entries) {
            out.add(new Raw(e.kind().name(), e.index(), e.anchor(), e.enabled()));
        }
        return out;
    }
}
