package za.co.neroland.nerospace.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.progression.StarGuide;

/** The companion's Star Guide counts: a completed hidden bonus step never counts toward a chapter. */
class StarGuideSnapshotCountTest {

    private static StarGuide.Step step(String id, boolean hidden) {
        return new StarGuide.Step(id, () -> null, Identifier.fromNamespaceAndPath("nerospace", "test/" + id), hidden);
    }

    @Test
    void chapterCompletionCountsOnlyRequiredSteps() {
        StarGuide.Chapter chapter = new StarGuide.Chapter("test",
                List.of(step("a", false), step("bonus", true), step("b", false)));
        int all = 0b111;
        assertEquals(2, NerospaceLinkSnapshots.requiredStepsComplete(chapter, all), "the hidden bonus is not counted");
        assertTrue(chapter.isComplete(all));
        assertEquals(1, NerospaceLinkSnapshots.requiredStepsComplete(chapter, 0b011), "a + bonus = one required step");
        assertEquals(0, NerospaceLinkSnapshots.requiredStepsComplete(chapter, 0b010));
    }
}
