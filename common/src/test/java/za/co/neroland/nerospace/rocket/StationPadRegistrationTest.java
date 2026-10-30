package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import org.junit.jupiter.api.Test;

/** Review #2: an arrival at a legacy station never renames the player's pad node (D5 pairing). */
class StationPadRegistrationTest {

    private static final ResourceKey<Level> STATION = ResourceKey.create(Registries.DIMENSION,
            Identifier.parse("nerospace:orbital_station"));

    @Test
    void existingNodeIsNeverRenamed() {
        PadRegistry pads = new PadRegistry();
        BlockPos padCentre = new BlockPos(0, 64, 6);
        PadRegistry.PadNode tagged = pads.register("Home", STATION, padCentre);
        assertNotNull(tagged);
        for (int arrival = 0; arrival < 3; arrival++) {
            PadRegistry.PadNode n = StationStructure.registerPadIn(pads, STATION, padCentre, "Alpha");
            assertNotNull(n);
            assertEquals(tagged.id(), n.id());
            assertEquals("Home", n.name());
        }
        assertEquals("Home", pads.at(STATION, padCentre).name());
        assertEquals(1, pads.all().size());
    }

    @Test
    void missingNodeIsRegisteredOnce() {
        PadRegistry pads = new PadRegistry();
        BlockPos padCentre = new BlockPos(4096, 64, 6);
        PadRegistry.PadNode first = StationStructure.registerPadIn(pads, STATION, padCentre, "Alpha");
        assertNotNull(first);
        assertEquals("Alpha Landing", first.name());
        PadRegistry.PadNode again = StationStructure.registerPadIn(pads, STATION, padCentre, "Renamed");
        assertNotNull(again);
        assertEquals("Alpha Landing", again.name());
        assertEquals(1, pads.all().size());
    }
}
