package za.co.neroland.nerospace.block.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Frost Crystal Cluster. */
@SuppressWarnings("unused")
class CrystalStageRulesTest {

    @Test
    @DisplayName("one stage per event end, sky-exposed only, max 3")
    void stage() {
        assertEquals(1, CrystalStageRules.nextStage(0, true, true));
        assertEquals(3, CrystalStageRules.nextStage(3, true, true));
        assertEquals(1, CrystalStageRules.nextStage(1, false, true));
        assertEquals(1, CrystalStageRules.nextStage(1, true, false));
        assertEquals(64, CrystalStageRules.PER_CHUNK_CAP);
    }

    @Test
    @DisplayName("light 3/5/7/9 and 1+stage shards")
    void lightAndDrops() {
        assertEquals(3, CrystalStageRules.light(0));
        assertEquals(9, CrystalStageRules.light(3));
        assertEquals(1, CrystalStageRules.shardDrops(0));
        assertEquals(4, CrystalStageRules.shardDrops(3));
    }
}
