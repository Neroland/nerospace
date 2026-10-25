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
 * Nyxara's band width, stored with the world ({@code nerospace:nyxara_band} in the Nyxara level's data folder). The
 * only field is optional ({@code band_width}, default {@code -1} = never initialised); the first load writes the
 * current {@code nyxara.twilightBandWidth} and later config edits never change it. Holds no player data.
 */
public final class NyxaraBandState extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "nyxara_band");
    public static final String RECOVERY_NAME = "nerospace:nyxara_band";

    public static final SavedDataType<NyxaraBandState> TYPE =
            new SavedDataType<>(ID, NyxaraBandState::new, codec(), null);

    int bandWidth = -1;

    public NyxaraBandState() {
    }

    public static NyxaraBandState get(ServerLevel level) {
        return SavedDataRecovery.get(level, TYPE, NyxaraBandState::new, RECOVERY_NAME);
    }

    private static Codec<NyxaraBandState> codec() {
        return RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("band_width", -1).forGetter(s -> s.bandWidth)
        ).apply(i, NyxaraBandState::of));
    }

    private static NyxaraBandState of(int width) {
        NyxaraBandState s = new NyxaraBandState();
        s.bandWidth = width;
        return s;
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
