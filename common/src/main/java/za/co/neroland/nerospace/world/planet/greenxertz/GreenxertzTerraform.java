package za.co.neroland.nerospace.world.planet.greenxertz;

import static za.co.neroland.nerospace.world.planet.TerraformTable.biome;
import static za.co.neroland.nerospace.world.planet.TerraformTable.mc;
import static za.co.neroland.nerospace.world.planet.TerraformTable.ns;
import static za.co.neroland.nerospace.world.planet.TerraformTable.w;

import java.util.List;
import java.util.Optional;

import za.co.neroland.nerospace.world.planet.TerraformTable;

/**
 * Greenxertz terraform table: the <b>xertz garden</b>. Rooted and Hydrated read like the 1.x meadow; Living writes
 * Xertz Soil on the surface, grows giant glowcaps among the trees and seeds Spore Moss that is tame inside the garden
 * (it does not spread there). Natural Xertz Soil (the 2.0 surface of every dry Greenxertz column) is listed as extra
 * surface, so stage 1 turns it to grass like 1.x grass ground; Xertz Soil written back by stage 3 is kept out of the
 * stage-1 swap on Living chunks.
 */
public final class GreenxertzTerraform {

    public static final TerraformTable TABLE = new TerraformTable("greenxertz",
            biome("terraformed_greenxertz_rooted"), biome("terraformed_greenxertz_hydrated"),
            biome("terraformed_greenxertz_living"),
            mc("grass_block"), mc("dirt"),
            List.of(ns("xertz_soil")),
            Optional.of(ns("xertz_soil")),
            mc("water"),
            TerraformTable.legacyFlora(),
            List.of(w(ns("spore_moss"), 3), w(ns("glowcap_fungus"), 1)),
            List.of(ns("meadow_loper")),
            TerraformTable.legacyOres(),
            Optional.of(ns("greenxertz_giant_glowcap")), 1.0,
            mc("happy_villager"),
            List.of());

    private GreenxertzTerraform() {
    }
}
