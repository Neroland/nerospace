package za.co.neroland.nerospace.world.planet.europa;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The Europa "liquid" test is water or brine only: lava and other fluids do not wet a seam or wake a vent. The
 * water half rests on the {@code minecraft:water} fluid tag, which needs a game registry, so it is checked in the
 * source (bootstrap-free) rather than through a block state.
 */
class EuropaHeatTest {

    @Test
    @DisplayName("isLiquid reads the water fluid tag (so lava is not liquid) or brine")
    void liquidIsWaterOrBrine() throws IOException {
        Path root = EuropaFiles.commonMain();
        Assumptions.assumeTrue(root != null, "common sources not found");
        String src = Files.readString(root.resolve(
                "java/za/co/neroland/nerospace/world/planet/europa/block/EuropaHeat.java"), StandardCharsets.UTF_8);
        assertTrue(src.contains("liquid(state.getFluidState().is(FluidTags.WATER), isBrine(state))"),
                "isLiquid = water tag or brine");
        assertTrue(src.contains("return water || brine;"));
    }
}
