package za.co.neroland.nerospace.block;

import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import za.co.neroland.nerospace.block.rules.WeatherScanRules;

/**
 * Weather transition scan (002 §3): on an event start/end, visit matching blocks in the loaded chunks near players,
 * at most {@code perChunkCap} per chunk. Sections whose palette cannot hold a match cost one palette check. Used by
 * Frost Crystal (diamond dust end, cap 64) and Dawnband Stone (flare start/end, cap 256).
 */
public final class WeatherScan {

    private WeatherScan() {
    }

    @FunctionalInterface
    public interface Visitor {
        /** Return true when the block was changed (counts against the cap). */
        boolean visit(BlockPos pos, BlockState state);
    }

    public static int forEachLoadedBlock(ServerLevel level, Predicate<BlockState> match, int perChunkCap, Visitor visitor) {
        int[] total = {0};
        LoadedChunks.forEachNearPlayers(level, (cx, cz) -> {
            LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (chunk == null) {
                return;
            }
            int touched = 0;
            LevelChunkSection[] sections = chunk.getSections();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int i = 0; i < sections.length && WeatherScanRules.underCap(touched, perChunkCap); i++) {
                LevelChunkSection section = sections[i];
                if (section == null || section.hasOnlyAir() || !section.maybeHas(match)) {
                    continue;
                }
                int baseY = (chunk.getMinSectionY() + i) << 4;
                for (int y = 0; y < 16 && WeatherScanRules.underCap(touched, perChunkCap); y++) {
                    for (int z = 0; z < 16 && WeatherScanRules.underCap(touched, perChunkCap); z++) {
                        for (int x = 0; x < 16 && WeatherScanRules.underCap(touched, perChunkCap); x++) {
                            BlockState s = section.getBlockState(x, y, z);
                            if (match.test(s) && visitor.visit(pos.set((cx << 4) + x, baseY + y, (cz << 4) + z), s)) {
                                touched++;
                            }
                        }
                    }
                }
            }
            total[0] += touched;
        });
        return total[0];
    }
}
