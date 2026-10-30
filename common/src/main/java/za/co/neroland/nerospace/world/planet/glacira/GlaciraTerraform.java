package za.co.neroland.nerospace.world.planet.glacira;

import static za.co.neroland.nerospace.world.planet.TerraformTable.biome;
import static za.co.neroland.nerospace.world.planet.TerraformTable.mc;
import static za.co.neroland.nerospace.world.planet.TerraformTable.ns;

import java.util.List;
import java.util.Optional;

import za.co.neroland.nerospace.world.planet.TerraformTable;

/**
 * Glacira terraform table (003 §8.1): moss on glacite (Rooted), meltwater only over non-permafrost ground
 * (Hydrated; {@code #nerospace:hydration_immune} floors are skipped), and the 1.x tundra — the kept
 * {@code terraformed_tundra} id — with permafrost thawing at stage 3 only (Living).
 *
 * <p>Compatibility: surface swap, scatter, ores, trees and livestock are the 1.6.0 values, so a running 1.6.0
 * stage-3 terraformer continues exactly as it did (same radii, energy, stage and Living output).</p>
 */
public final class GlaciraTerraform {

    public static final TerraformTable TABLE = new TerraformTable("glacira",
            biome("terraformed_glacira_rooted"), biome("terraformed_glacira_hydrated"), biome("terraformed_tundra"),
            mc("grass_block"), mc("dirt"),
            List.of(),
            Optional.empty(),
            mc("water"),
            TerraformTable.legacyFlora(),
            List.of(),
            List.of(ns("woolly_drift")),
            TerraformTable.legacyOres(),
            Optional.empty(), 1.0,
            mc("happy_villager"),
            List.of());

    private GlaciraTerraform() {
    }
}
