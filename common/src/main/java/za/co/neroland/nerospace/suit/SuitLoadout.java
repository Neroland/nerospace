package za.co.neroland.nerospace.suit;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The {@code nerospace:suit_loadout} component. Every field is optional and defaults to an empty Mk I piece.
 * Unknown module ids are kept. An unknown frame ordinal becomes Mk I.
 */
public record SuitLoadout(int frame, String weave, List<String> linings, List<String> modules, int toggles,
        int liveryColor, int liveryPattern) {

    public static final SuitLoadout EMPTY = new SuitLoadout(0, "", List.of(), List.of(), 0, -1, 0);

    public static final Codec<SuitLoadout> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("frame", 0).forGetter(SuitLoadout::frame),
            Codec.STRING.optionalFieldOf("weave", "").forGetter(SuitLoadout::weave),
            Codec.STRING.listOf().optionalFieldOf("linings", List.of()).forGetter(SuitLoadout::linings),
            Codec.STRING.listOf().optionalFieldOf("modules", List.of()).forGetter(SuitLoadout::modules),
            Codec.INT.optionalFieldOf("toggles", 0).forGetter(SuitLoadout::toggles),
            Codec.INT.optionalFieldOf("livery_color", -1).forGetter(SuitLoadout::liveryColor),
            Codec.INT.optionalFieldOf("livery_pattern", 0).forGetter(SuitLoadout::liveryPattern)
    ).apply(instance, SuitLoadout::normalized));

    /** Codec factory: clamps the frame, caps the lists, keeps unknown module strings. */
    public static SuitLoadout normalized(Integer frame, String weave, List<String> linings, List<String> modules,
            Integer toggles, Integer liveryColor, Integer liveryPattern) {
        int ordinal = frame == null ? 0 : frame;
        if (ordinal < 0 || ordinal >= SuitFrame.values().length) {
            ordinal = 0;
        }
        return new SuitLoadout(ordinal, weave == null ? "" : weave, cap(linings, 2), cap(modules, 4),
                toggles == null ? 0 : toggles, liveryColor == null ? -1 : liveryColor,
                liveryPattern == null ? 0 : liveryPattern);
    }

    private static List<String> cap(List<String> in, int max) {
        if (in == null || in.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String id : in) {
            if (id == null || id.isEmpty()) {
                continue;
            }
            out.add(id);
            if (out.size() == max) {
                break;
            }
        }
        return List.copyOf(out);
    }

    public SuitFrame frameEnum() {
        return SuitFrame.byOrdinal(this.frame);
    }

    public boolean hasWeave() {
        return this.weave != null && !this.weave.isEmpty();
    }

    public boolean hasModule(String id) {
        return id != null && this.modules.contains(id);
    }

    public int moduleCount(String id) {
        int n = 0;
        for (String module : this.modules) {
            if (id.equals(module)) {
                n++;
            }
        }
        return n;
    }

    public boolean toggleOn(int index) {
        return index >= 0 && index < 32 && (this.toggles & (1 << index)) != 0;
    }

    public SuitLoadout withFrame(SuitFrame next) {
        return new SuitLoadout(next.ordinal(), this.weave, this.linings, this.modules, this.toggles,
                this.liveryColor, this.liveryPattern);
    }

    public SuitLoadout withWeave(String id) {
        return new SuitLoadout(this.frame, id == null ? "" : id, this.linings, this.modules, this.toggles,
                this.liveryColor, this.liveryPattern);
    }

    public SuitLoadout withLinings(List<String> next) {
        return new SuitLoadout(this.frame, this.weave, cap(next, 2), this.modules, this.toggles, this.liveryColor,
                this.liveryPattern);
    }

    public SuitLoadout withModules(List<String> next) {
        return new SuitLoadout(this.frame, this.weave, this.linings, cap(next, 4), this.toggles, this.liveryColor,
                this.liveryPattern);
    }

    public SuitLoadout withToggle(int index, boolean on) {
        int mask = index >= 0 && index < 32 ? (1 << index) : 0;
        int next = on ? (this.toggles | mask) : (this.toggles & ~mask);
        return new SuitLoadout(this.frame, this.weave, this.linings, this.modules, next, this.liveryColor,
                this.liveryPattern);
    }

    public SuitLoadout withLivery(int color, int pattern) {
        return new SuitLoadout(this.frame, this.weave, this.linings, this.modules, this.toggles, color, pattern);
    }
}
