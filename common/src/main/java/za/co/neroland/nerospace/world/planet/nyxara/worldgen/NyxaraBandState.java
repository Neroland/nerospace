package za.co.neroland.nerospace.world.planet.nyxara.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import za.co.neroland.nerolandcore.data.SavedDataRecovery;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Nyxara's band width, stored with the world ({@code nerospace:nyxara_band} in the Nyxara level's data folder, plus a
 * second copy under the same id in the overworld's data folder). Both fields are optional ({@code band_width}, default
 * {@code -1} = never initialised; {@code bound_at}, the game time it was first bound, default {@code -1} = unknown);
 * the first load writes the current {@code nyxara.twilightBandWidth} and later config edits never change it. If one
 * copy is lost (a recovery reset), the other restores it ({@link BandWidthRules#reconcile}). Holds no player data.
 */
public final class NyxaraBandState extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "nyxara_band");
    public static final String RECOVERY_NAME = "nerospace:nyxara_band";

    public static final SavedDataType<NyxaraBandState> TYPE =
            new SavedDataType<>(ID, NyxaraBandState::new, codec(), null);

    int bandWidth = -1;
    long boundAt = -1L;

    public NyxaraBandState() {
    }

    public static NyxaraBandState get(ServerLevel level) {
        return SavedDataRecovery.get(level, TYPE, NyxaraBandState::new, RECOVERY_NAME);
    }

    private static Codec<NyxaraBandState> codec() {
        return RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("band_width", -1).forGetter(s -> s.bandWidth),
                Codec.LONG.optionalFieldOf("bound_at", -1L).forGetter(s -> s.boundAt)
        ).apply(i, NyxaraBandState::of));
    }

    private static NyxaraBandState of(int width, long boundAt) {
        NyxaraBandState s = new NyxaraBandState();
        s.bandWidth = width;
        s.boundAt = boundAt;
        return s;
    }

    /** The raw stored width ({@code <= 0} = none). */
    public int storedWidth() {
        return this.bandWidth;
    }

    /** The game time the width was first bound ({@code -1} = unknown). */
    public long boundAt() {
        return this.boundAt;
    }

    /** Stores a reconciled width; marks dirty only on change. */
    public void store(int width, long at) {
        if (this.bandWidth != width || this.boundAt != at) {
            this.bandWidth = width;
            this.boundAt = at;
            setDirty();
        }
    }

    /**
     * The stored width; on the first call for a world (nothing stored) stores {@code configWidth} (clamped) and marks
     * the data dirty. A stored value outside the range (hand-edited file) is clamped on read, not rewritten.
     */
    public int widthOrInit(int configWidth) {
        int w = BandWidthRules.resolve(this.bandWidth, configWidth);
        if (this.bandWidth <= 0) {
            this.bandWidth = w;
            setDirty();
        }
        return w;
    }

    public boolean initialised() {
        return this.bandWidth > 0;
    }
}
