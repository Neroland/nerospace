package za.co.neroland.nerospace.world;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;

import za.co.neroland.nerospace.village.VillagerCulture;
import za.co.neroland.nerospace.village.VillagerCultures;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;

/**
 * The shared gate in front of every alien settlement feature: per-planet {@link StructureSpacing} salt, the
 * culture's Mega City flag, the Duskborn twilight-band rule, and the culture's build {@link AlienBuild.Palette}.
 * A dimension with no culture (a datapack placing a hamlet elsewhere) builds in the Xertz palette, as 1.6.0 did.
 */
public final class SettlementPlacement {

    private SettlementPlacement() {
    }

    /** The planet's structure salt for this level (0 for the 1.x planets and any non-planet). */
    public static long saltFor(WorldGenLevel level) {
        PlanetDefinition planet = Planets.get(level.getLevel().dimension());
        return planet == null ? 0L : planet.structureSalt();
    }

    /**
     * Whether a settlement of kind {@code roi} stands at {@code origin}, and if so the {@link AlienBuild.Site} to
     * build it through (palette chosen, writes confined to the feature's 3×3-chunk region).
     */
    @Nullable
    public static AlienBuild.Site begin(WorldGenLevel level, BlockPos origin, StructureSpacing.Roi roi) {
        if (!StructureSpacing.shouldPlace(origin, roi, saltFor(level))) {
            return null;
        }
        Optional<VillagerCulture> culture = VillagerCultures.forDimension(level.getLevel().dimension());
        if (culture.isPresent()) {
            VillagerCulture c = culture.get();
            if (roi == StructureSpacing.Roi.MEGA_CITY && !c.megaCity()) {
                return null;
            }
            if (!VillagerCultures.allowsBiome(c, level.getBiome(origin))) {
                return null;
            }
        }
        return new AlienBuild.Site(level, AlienBuild.paletteFor(culture.orElse(null)),
                StructureRegion.around(origin.getX(), origin.getZ()));
    }

    /** A non-settlement structure (Mars): salt + region, Xertz-free palette is the caller's business. */
    @Nullable
    public static StructureRegion beginPlain(WorldGenLevel level, BlockPos origin, StructureSpacing.Roi roi) {
        if (!StructureSpacing.shouldPlace(origin, roi, saltFor(level))) {
            return null;
        }
        return StructureRegion.around(origin.getX(), origin.getZ());
    }
}
