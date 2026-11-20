package za.co.neroland.nerospace.world.planet.europa;

import static za.co.neroland.nerospace.world.planet.TerraformTable.biome;
import static za.co.neroland.nerospace.world.planet.TerraformTable.mc;
import static za.co.neroland.nerospace.world.planet.TerraformTable.ns;

import java.util.List;
import java.util.Optional;

import za.co.neroland.nerospace.world.planet.TerraformTable;
import za.co.neroland.nerospace.world.planet.TerraformTable.Rule;

/**
 * Europa terraform table: the surface ice softens to firn over clean ice (Rooted), dark leads of open water appear,
 * one spring per chunk (Hydrated), and the top of the shell opens into a shallow sea (Living). Nothing green grows,
 * so there is no ground cover, and the result is never breathable: a suit is still needed, a Cryo suit is not.
 */
public final class EuropaTerraform {

    public static final TerraformTable TABLE = new TerraformTable("europa",
            biome("terraformed_europa_rooted"), biome("terraformed_europa_hydrated"),
            biome("terraformed_europa_living"),
            mc("snow_block"), ns("europa_ice"),
            List.of(ns("europa_ice"), ns("chaos_ice"), ns("lineae_salt")),
            Optional.empty(),
            mc("water"),
            List.of(),
            List.of(),
            List.of(ns("lantern_drifter")),
            List.of(ns("oxidant_ice"), ns("deuterium_ice")),
            Optional.empty(), 0.0,
            mc("snowflake"),
            List.of(Rule.OPEN_WATER_SPRING, Rule.OPEN_OCEAN, Rule.NO_ATMOSPHERE));

    private EuropaTerraform() {
    }
}
