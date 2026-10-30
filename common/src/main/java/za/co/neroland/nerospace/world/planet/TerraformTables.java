package za.co.neroland.nerospace.world.planet;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.world.planet.cindara.CindaraTerraform;
import za.co.neroland.nerospace.world.planet.glacira.GlaciraTerraform;
import za.co.neroland.nerospace.world.planet.greenxertz.GreenxertzTerraform;
import za.co.neroland.nerospace.world.planet.luna.LunaTerraform;
import za.co.neroland.nerospace.world.planet.mars.MarsTerraform;
import za.co.neroland.nerospace.world.planet.nyxara.NyxaraTerraform;

/**
 * The per-planet terraform tables, plus the {@link #LEGACY} table used in non-planet dimensions (the 1.x behaviour:
 * the neon {@code terraformed} look for stages 1–2 and the meadow at stage 3). The station has no table: the
 * terraformer refuses there ({@link TerraformRefusal#NO_SURFACE}).
 *
 * <p>{@link #ORDER} indexes the tables for the Monitor's synced readout; append only.</p>
 */
public final class TerraformTables {

    /** Outside the Nerospace planets (overworld, other mods' dimensions): exactly the 1.x behaviour. */
    public static final TerraformTable LEGACY = new TerraformTable("legacy",
            TerraformTable.biome("terraformed"), TerraformTable.biome("terraformed"),
            TerraformTable.biome("terraformed_meadow"),
            TerraformTable.mc("grass_block"), TerraformTable.mc("dirt"),
            List.of(), Optional.empty(), TerraformTable.mc("water"),
            TerraformTable.legacyFlora(), List.of(), List.of(TerraformTable.ns("meadow_loper")),
            TerraformTable.legacyOres(), Optional.empty(), 1.0, TerraformTable.mc("happy_villager"), List.of());

    /** Synced index order (Monitor data slot). Append only. */
    public static final List<TerraformTable> ORDER = List.of(LEGACY, GreenxertzTerraform.TABLE,
            CindaraTerraform.TABLE, GlaciraTerraform.TABLE, LunaTerraform.TABLE, MarsTerraform.TABLE,
            NyxaraTerraform.TABLE);

    private TerraformTables() {
    }

    /** The table for a planet's {@code langId}; empty for the station and unknown ids. */
    public static Optional<TerraformTable> forLangId(String langId) {
        for (TerraformTable t : ORDER) {
            if (t != LEGACY && t.langId().equals(langId)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    /** The table that applies in a dimension: the planet's own, LEGACY off-planet, null where it refuses. */
    @Nullable
    public static TerraformTable resolve(@Nullable PlanetDefinition planet) {
        if (planet == null) {
            return LEGACY;
        }
        return planet.terraform().orElse(null);
    }

    public static int indexOf(@Nullable TerraformTable table) {
        return table == null ? -1 : ORDER.indexOf(table);
    }

    @Nullable
    public static TerraformTable byIndex(int index) {
        return index >= 0 && index < ORDER.size() ? ORDER.get(index) : null;
    }
}
