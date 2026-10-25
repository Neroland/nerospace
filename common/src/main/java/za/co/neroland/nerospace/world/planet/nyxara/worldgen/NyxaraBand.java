package za.co.neroland.nerospace.world.planet.nyxara.worldgen;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.BiomeSource;

import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.registry.ModDimensions;

/**
 * Binds Nyxara's stored band width onto its {@link FixedXBiomeSource}. {@link #bind(MinecraftServer)} is idempotent
 * and O(1) after the first call per server, so it can run from the common server tick: the first tick after start
 * reads (or initialises) {@link NyxaraBandState} and binds it before any Nyxara chunk is generated for a player.
 */
public final class NyxaraBand {

    private static final Map<MinecraftServer, Boolean> BOUND = Collections.synchronizedMap(new WeakHashMap<>());

    private NyxaraBand() {
    }

    public static void bind(MinecraftServer server) {
        if (BOUND.containsKey(server)) {
            return;
        }
        BOUND.put(server, Boolean.TRUE);
        ServerLevel level = server.getLevel(ModDimensions.NYXARA_LEVEL);
        if (level != null) {
            bind(level);
        }
    }

    /** Binds and returns the width for {@code level}, or {@code -1} if it does not use the fixed-X source. */
    public static int bind(ServerLevel level) {
        if (!(source(level) instanceof FixedXBiomeSource fixedX)) {
            return -1;
        }
        int width = NyxaraBandState.get(level).widthOrInit(NerospaceConfig.nyxaraTwilightBandWidth());
        fixedX.bindWidth(width);
        return width;
    }

    /** The band width in effect on {@code level} (the sky blend and band checks read this), or the default. */
    public static int width(ServerLevel level) {
        return source(level) instanceof FixedXBiomeSource fixedX ? fixedX.width() : FixedXBands.DEFAULT_WIDTH;
    }

    private static BiomeSource source(ServerLevel level) {
        return level.getChunkSource().getGenerator().getBiomeSource();
    }
}
