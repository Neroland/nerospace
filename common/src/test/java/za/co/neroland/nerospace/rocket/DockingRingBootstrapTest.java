package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

/** 26.3 rule: a random-tick-dependent block declares randomTicks() and bootstraps only when its node is missing. */
class DockingRingBootstrapTest {

    static Path source(String rel) {
        for (Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath(); p != null; p = p.getParent()) {
            Path c = p.resolve("common/src/main/java/za/co/neroland/nerospace/rocket").resolve(rel);
            if (Files.isRegularFile(c)) {
                return c;
            }
        }
        throw new IllegalStateException("source not found: " + rel);
    }

    @Test
    void registrationDeclaresRandomTicks() throws Exception {
        String s = Files.readString(source("DockingContent.java"));
        int start = s.indexOf("\"docking_ring\"");
        int end = s.indexOf(";", start);
        assertTrue(start >= 0 && end > start);
        assertTrue(s.substring(start, end).contains(".randomTicks()"));
        String block = Files.readString(source("DockingRingBlock.java"));
        assertTrue(block.contains("protected void randomTick("));
        assertTrue(block.contains("protected void onPlace("));
    }

    @Test
    void bootstrapOnlyWhenMissing() {
        assertTrue(DockRules.shouldRegister(false));
        assertFalse(DockRules.shouldRegister(true));
        assertEquals(new net.minecraft.world.phys.Vec3(9.5D, 64.0D + 3.0D / 16.0D, 0.5D),
                DockRules.dockPosition(new BlockPos(9, 64, 0), LandingResolver.SURFACE_HEIGHT));
    }

    @Test
    void partsMapBackToTheCentre() {
        BlockPos c = new BlockPos(9, 64, 0);
        for (DockingRingPart part : DockingRingPart.values()) {
            assertEquals(c, part.centreFrom(part.at(c)));
        }
        assertEquals(9, DockingRingPart.values().length);
    }
}
