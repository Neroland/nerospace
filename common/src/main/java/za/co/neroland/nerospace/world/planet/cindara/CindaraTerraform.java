package za.co.neroland.nerospace.world.planet.cindara;

import static za.co.neroland.nerospace.world.planet.TerraformTable.biome;
import static za.co.neroland.nerospace.world.planet.TerraformTable.mc;
import static za.co.neroland.nerospace.world.planet.TerraformTable.ns;

import java.util.List;
import java.util.Optional;

import za.co.neroland.nerospace.world.planet.TerraformTable;

/**
 * Cindara terraform table: ashstone turns to cinder soil (Rooted), the steam vents cool and water
 * appears (Hydrated), and the 1.x savanna settles — the kept {@code terraformed_savanna} id with its acacia-family
 * Cinder Trees and Ember Strutters (Living).
 */
public final class CindaraTerraform {

    public static final TerraformTable TABLE = new TerraformTable("cindara",
            biome("terraformed_cindara_rooted"), biome("terraformed_cindara_hydrated"), biome("terraformed_savanna"),
            mc("grass_block"), mc("coarse_dirt"),
            List.of(ns("ashstone"), ns("ember_cracked_ashstone")),
            Optional.empty(),
            mc("water"),
            TerraformTable.legacyFlora(),
            List.of(),
            List.of(ns("ember_strutter")),
            TerraformTable.legacyOres(),
            Optional.empty(), 1.0,
            mc("happy_villager"),
            List.of());

    private CindaraTerraform() {
    }
}
