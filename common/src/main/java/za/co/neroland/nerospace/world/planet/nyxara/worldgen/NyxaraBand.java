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
 * Binds Nyxara's stored band width and the world seed onto its {@link FixedXBiomeSource}.
 * {@link #bind(MinecraftServer)} is idempotent and O(1) after the first call per server. It runs when the server
 * prepares its levels (before any chunk, force-loaded ones included, is generated) and again from the common server
 * tick as a fallback. The width is kept in two copies (the Nyxara level's store and an overworld backup) and
 * reconciled on bind, so losing one store never shifts an existing world's band.
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
        NyxaraBandState primary = NyxaraBandState.get(level);
        ServerLevel overworld = level.getServer().overworld();
        NyxaraBandState backup = overworld == null || overworld == level ? null : NyxaraBandState.get(overworld);
        BandWidthRules.Bound bound = BandWidthRules.reconcile(primary.storedWidth(), primary.boundAt(),
                backup == null ? -1 : backup.storedWidth(), backup == null ? -1L : backup.boundAt(),
                NerospaceConfig.nyxaraTwilightBandWidth(), level.getGameTime());
        primary.store(bound.width(), bound.boundAt());
        if (backup != null) {
            backup.store(bound.width(), bound.boundAt());
        }
        fixedX.bindWidth(bound.width());
        fixedX.bindSeed(level.getSeed());
        return bound.width();
    }

    /** The band width in effect on {@code level} (the sky blend and band checks read this), or the default. */
    public static int width(ServerLevel level) {
        return source(level) instanceof FixedXBiomeSource fixedX ? fixedX.width() : FixedXBands.DEFAULT_WIDTH;
    }

    private static BiomeSource source(ServerLevel level) {
        return level.getChunkSource().getGenerator().getBiomeSource();
    }
}
