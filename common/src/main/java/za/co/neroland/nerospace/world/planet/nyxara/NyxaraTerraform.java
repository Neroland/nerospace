package za.co.neroland.nerospace.world.planet.nyxara;

import static za.co.neroland.nerospace.world.planet.TerraformTable.biome;
import static za.co.neroland.nerospace.world.planet.TerraformTable.mc;
import static za.co.neroland.nerospace.world.planet.TerraformTable.ns;
import static za.co.neroland.nerospace.world.planet.TerraformTable.w;

import java.util.List;
import java.util.Optional;

import za.co.neroland.nerospace.world.planet.TerraformTable;
import za.co.neroland.nerospace.world.planet.TerraformTable.Rule;

/**
 * Nyxara terraform table: the <b>twilight grove</b>. It only roots in the twilight band — the
 * terraformer refuses on the dayside and nightside, and the Monitor says why — and columns outside the band are
 * skipped as the frontier grows past it.
 */
public final class NyxaraTerraform {

    public static final TerraformTable TABLE = new TerraformTable("nyxara",
            biome("terraformed_nyxara_rooted"), biome("terraformed_nyxara_hydrated"), biome("terraformed_nyxara_living"),
            mc("grass_block"), mc("dirt"),
            List.of(ns("nyx_stone"), mc("blackstone"), mc("smooth_basalt")),
            Optional.empty(),
            mc("water"),
            List.of(w(mc("short_grass"), 55), w(mc("fern"), 25), w(mc("blue_orchid"), 10),
                    w(mc("lily_of_the_valley"), 10)),
            List.of(),
            List.of(ns("dusk_moth")),
            List.of(ns("voidglass_ore"), ns("nerosium_ore")),
            Optional.of(mc("dark_oak")), 1.0,
            mc("glow"),
            List.of(Rule.BAND_ONLY));

    private NyxaraTerraform() {
    }
}
