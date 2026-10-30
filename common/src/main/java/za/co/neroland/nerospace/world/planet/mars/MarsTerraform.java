package za.co.neroland.nerospace.world.planet.mars;

import static za.co.neroland.nerospace.world.planet.TerraformTable.biome;
import static za.co.neroland.nerospace.world.planet.TerraformTable.mc;
import static za.co.neroland.nerospace.world.planet.TerraformTable.ns;
import static za.co.neroland.nerospace.world.planet.TerraformTable.w;

import java.util.List;
import java.util.Optional;

import za.co.neroland.nerospace.world.planet.TerraformTable;
import za.co.neroland.nerospace.world.planet.TerraformTable.Rule;

/**
 * Mars terraform table (003 §8.1): rust sand turns to red loam under hardy grass (Rooted), the <b>first open water
 * on Mars</b> appears — one spring per chunk at its lowest surface cell (Hydrated), and a treeless red steppe
 * settles (Living). Storms still fire over terraformed land.
 */
public final class MarsTerraform {

    public static final TerraformTable TABLE = new TerraformTable("mars",
            biome("terraformed_mars_rooted"), biome("terraformed_mars_hydrated"), biome("terraformed_mars_living"),
            mc("grass_block"), mc("rooted_dirt"),
            List.of(ns("red_regolith"), ns("martian_sandstone"), ns("martian_sandstone_strata_1"),
                    ns("martian_sandstone_strata_2"), ns("martian_sandstone_strata_3"),
                    ns("martian_sandstone_strata_4")),
            Optional.empty(),
            mc("water"),
            List.of(w(mc("short_grass"), 55), w(mc("short_dry_grass"), 30), w(mc("dead_bush"), 15)),
            List.of(),
            List.of(ns("dust_strider")),
            List.of(ns("hematite_ore"), ns("nerosteel_ore")),
            Optional.empty(), 0.0,
            mc("white_ash"),
            List.of(Rule.OPEN_WATER_SPRING));

    private MarsTerraform() {
    }
}
