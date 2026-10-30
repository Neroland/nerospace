package za.co.neroland.nerospace.block.light;

import java.util.Arrays;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.BlockLightEngine;

/**
 * Dev-only relight benchmark for the Nyx Stone absorption. Builds a <b>private</b> block-light engine over
 * the already-loaded 4×4 chunks around a centre chunk and times a full block-light relight, once with no absorbers
 * indexed and once with {@code count} synthetic absorbers spread through the area (the same index the real Nyx Stone
 * feeds, so the measured cost is the mixin hot path). The live light engine and the world are never modified.
 * Server thread only. Reports timings only — no player data.
 */
public final class RelightBench {

    public static final int SIDE = 4;
    public static final int RUNS = 5;

    private RelightBench() {
    }

    /** Median milliseconds {0 absorbers, count absorbers}, or null when some chunk in the area is not loaded. */
    public static double[] run(ServerLevel level, ChunkPos centre, int count, long seed) {
        LevelChunk[] chunks = new LevelChunk[SIDE * SIDE];
        int x0 = centre.x() - SIDE / 2;
        int z0 = centre.z() - SIDE / 2;
        for (int i = 0; i < chunks.length; i++) {
            chunks[i] = level.getChunkSource().getChunkNow(x0 + i % SIDE, z0 + i / SIDE);
            if (chunks[i] == null) {
                return null;
            }
        }
        double[] base = new double[RUNS];
        double[] with = new double[RUNS];
        for (int r = 0; r < RUNS; r++) {
            base[r] = once(level, chunks, 0, seed + r);
            with[r] = once(level, chunks, count, seed + r);
        }
        Arrays.sort(base);
        Arrays.sort(with);
        return new double[] {base[RUNS / 2], with[RUNS / 2]};
    }

    private static double once(ServerLevel level, LevelChunk[] chunks, int absorbers, long seed) {
        LightChunkGetter getter = new LightChunkGetter() {
            @Override
            public LightChunk getChunkForLighting(int x, int z) {
                return level.getChunkSource().getChunkNow(x, z);
            }

            @Override
            public BlockGetter getLevel() {
                return level;
            }
        };
        BlockLightEngine engine = new BlockLightEngine(getter);
        NyxLightIndex index = ((NyxIndexHolder) (Object) engine).nerospace$nyxIndex();
        long start = System.nanoTime();
        for (LevelChunk chunk : chunks) {
            ChunkPos pos = chunk.getPos();
            LevelChunkSection[] sections = chunk.getSections();
            for (int i = 0; i < sections.length; i++) {
                engine.updateSectionStatus(SectionPos.of(pos, chunk.getMinSectionY() + i), sections[i].hasOnlyAir());
            }
            engine.setLightEnabled(pos, true);
        }
        if (absorbers > 0) {
            RandomSource random = RandomSource.create(seed);
            ChunkPos first = chunks[0].getPos();
            int minY = level.getMinY();
            int height = level.getHeight();
            for (int n = 0; n < absorbers; n++) {
                index.add(first.getMinBlockX() + random.nextInt(SIDE * 16), minY + random.nextInt(height),
                        first.getMinBlockZ() + random.nextInt(SIDE * 16));
            }
        }
        for (LevelChunk chunk : chunks) {
            engine.propagateLightSources(chunk.getPos());
        }
        int guard = 0;
        while (engine.hasLightWork() && guard++ < 1024) {
            engine.runLightUpdates();
        }
        return (System.nanoTime() - start) / 1.0e6;
    }
}
