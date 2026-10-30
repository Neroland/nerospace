package za.co.neroland.nerospace.world.planet.luna;

import static za.co.neroland.nerospace.world.planet.TerraformTable.biome;
import static za.co.neroland.nerospace.world.planet.TerraformTable.mc;
import static za.co.neroland.nerospace.world.planet.TerraformTable.ns;
import static za.co.neroland.nerospace.world.planet.TerraformTable.w;

import java.util.List;
import java.util.Optional;

import za.co.neroland.nerospace.world.planet.TerraformTable;
import za.co.neroland.nerospace.world.planet.TerraformTable.Rule;

/**
 * Luna terraform table: regolith turns to grey soil with lichen (Rooted), frost pools gather in the
 * craters (Hydrated), and a pressurised <b>lunar meadow</b> settles (Living). Luna is the only world with no
 * atmosphere to hold the change, so the Living edge shimmers — a particle ring at the terraformer radius, no block.
 */
public final class LunaTerraform {

    public static final TerraformTable TABLE = new TerraformTable("luna",
            biome("terraformed_luna_rooted"), biome("terraformed_luna_hydrated"), biome("terraformed_luna_living"),
            mc("grass_block"), mc("coarse_dirt"),
            List.of(ns("regolith"), ns("lunar_basalt"), ns("anorthosite")),
            Optional.empty(),
            mc("water"),
            List.of(w(mc("short_grass"), 60), w(mc("moss_carpet"), 30), w(mc("azure_bluet"), 10)),
            List.of(),
            List.of(ns("moon_hopper")),
            List.of(ns("ilmenite_ore"), ns("nerosium_ore")),
            Optional.empty(), 0.3,
            mc("end_rod"),
            List.of(Rule.SHIMMER_DOME));

    private LunaTerraform() {
    }
}
