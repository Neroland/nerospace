package za.co.neroland.nerospace.world.planet.greenxertz;

import static za.co.neroland.nerospace.world.planet.TerraformTable.biome;
import static za.co.neroland.nerospace.world.planet.TerraformTable.mc;
import static za.co.neroland.nerospace.world.planet.TerraformTable.ns;
import static za.co.neroland.nerospace.world.planet.TerraformTable.w;

import java.util.List;
import java.util.Optional;

import za.co.neroland.nerospace.world.planet.TerraformTable;

/**
 * Greenxertz terraform table (003 §8.1): the <b>xertz garden</b>. Rooted and Hydrated read like the 1.x meadow;
 * Living writes Xertz Soil on the surface, grows giant glowcaps among the trees and seeds Spore Moss that is tame
 * inside the garden (it does not spread there). Xertz Soil written by stage 3 is kept out of the stage-1 grass swap.
 */
public final class GreenxertzTerraform {

    public static final TerraformTable TABLE = new TerraformTable("greenxertz",
            biome("terraformed_greenxertz_rooted"), biome("terraformed_greenxertz_hydrated"),
            biome("terraformed_greenxertz_living"),
            mc("grass_block"), mc("dirt"),
            List.of(),
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
