package za.co.neroland.nerospace.world.planet.nyxara.worldgen;

import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * {@code nerospace:fixed_x} — Nyxara's biome source: the biome depends on block X only (plus a ragged edge),
 * never on climate noise, so the dayside, twilight band and nightside are fixed regions of the map:
 * <pre>
 * { "type": "nerospace:fixed_x", "west": "nerospace:nyxara_dayside", "band": "nerospace:nyxara_twilight_band",
 *   "east": "nerospace:nyxara_nightside", "band_width": 2048, "origin_x": 0, "edge_noise": 96 }
 * </pre>
 * {@code band_width} is the full width (it replaced an earlier {@code band_half_width} draft). The datapack
 * value is only the fallback: once the Nyxara level loads, {@link NyxaraBand} binds the width stored in the level's
 * {@code nerospace:nyxara_band} saved data (initialised once from {@code nyxara.twilightBandWidth} on the first load),
 * so editing the config later never moves an existing world's band. Terrain shape comes from the noise router, not the
 * biome, so a biome edge changes only surface blocks and features — no terrain wall forms at the terminator.
 */
public final class FixedXBiomeSource extends BiomeSource {

    public static final MapCodec<FixedXBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Biome.CODEC.fieldOf("west").forGetter(s -> s.west),
            Biome.CODEC.fieldOf("band").forGetter(s -> s.band),
            Biome.CODEC.fieldOf("east").forGetter(s -> s.east),
            Codec.intRange(FixedXBands.MIN_WIDTH, FixedXBands.MAX_WIDTH)
                    .optionalFieldOf("band_width", FixedXBands.DEFAULT_WIDTH).forGetter(s -> s.datapackWidth),
            Codec.INT.optionalFieldOf("origin_x", 0).forGetter(s -> s.originX),
            Codec.intRange(0, FixedXBands.MAX_EDGE_NOISE)
                    .optionalFieldOf("edge_noise", FixedXBands.DEFAULT_EDGE_NOISE).forGetter(s -> s.edgeNoise)
    ).apply(i, FixedXBiomeSource::new));

    private final Holder<Biome> west;
    private final Holder<Biome> band;
    private final Holder<Biome> east;
    private final int datapackWidth;
    private final int originX;
    private final int edgeNoise;
    /** Width bound from the level's saved data; {@code -1} until bound (the datapack width applies). */
    private volatile int boundWidth = -1;
    /** World seed bound with the width (seeds the terminator's raggedness); {@code 0} until bound. */
    private volatile long boundSeed;

    public FixedXBiomeSource(Holder<Biome> west, Holder<Biome> band, Holder<Biome> east, int width, int originX,
            int edgeNoise) {
        this.west = west;
        this.band = band;
        this.east = east;
        this.datapackWidth = FixedXBands.clampWidth(width);
        this.originX = originX;
        this.edgeNoise = edgeNoise;
    }

    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(this.west, this.band, this.east);
    }

    /** Binds the world's stored band width (clamped). Called by {@link NyxaraBand} when the level loads. */
    public void bindWidth(int width) {
        this.boundWidth = FixedXBands.clampWidth(width);
    }

    /** Binds the world seed that shapes the terminator's ragged edges. Called by {@link NyxaraBand}. */
    public void bindSeed(long seed) {
        this.boundSeed = seed;
    }

    public long seed() {
        return this.boundSeed;
    }

    /** The band width in effect: the bound world value, else the datapack value. */
    public int width() {
        int bound = this.boundWidth;
        return bound > 0 ? bound : this.datapackWidth;
    }

    public int datapackWidth() {
        return this.datapackWidth;
    }

    public int originX() {
        return this.originX;
    }

    public int edgeNoise() {
        return this.edgeNoise;
    }

    /** The biome at block ({@code x}, {@code z}). */
    public Holder<Biome> biomeAtBlock(int x, int z) {
        return switch (FixedXBands.sideAt(x, z, width(), this.originX, this.edgeNoise, this.boundSeed)) {
            case WEST -> this.west;
            case BAND -> this.band;
            case EAST -> this.east;
        };
    }

    /** Quart (4-block) coordinates → the biome at the cell's corner block. */
    private Holder<Biome> biomeAtQuart(int qx, int qz) {
        return biomeAtBlock((int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, (long) qx << 2)),
                (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, (long) qz << 2)));
    }

    //? if >=26.3 {
    /*@Override
    public net.minecraft.world.level.biome.BiomeResolver createResolver(Climate.Sampler sampler) {
        return (x, y, z) -> biomeAtQuart(x, z);
    }
    *///?} else {
    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return biomeAtQuart(x, z);
    }
    //?}
}
