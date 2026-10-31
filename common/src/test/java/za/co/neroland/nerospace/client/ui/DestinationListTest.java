package za.co.neroland.nerospace.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.rocket.Destinations;
import za.co.neroland.nerospace.rocket.RocketTier;

/** The console list's row → destination-index mapping, at every scroll offset. */
class DestinationListTest {

    private static final int VISIBLE = 3;

    @Test
    @DisplayName("Tier 4 from the overworld: every visible row maps to the card drawn in it, Nyxara included")
    void tier4RowsMapToTheirCards() {
        int mask = Destinations.availableMask(RocketTier.TIER_4, ModDimensions.OVERWORLD);
        List<Integer> entries = DestinationList.entries(mask, Destinations.HOME, Destinations.all().size(), List.of());
        assertEquals(List.of(Destinations.STATION, Destinations.GREENXERTZ, Destinations.CINDARA,
                Destinations.GLACIRA, Destinations.LUNA, Destinations.MARS, Destinations.NYXARA), entries.subList(1, 8));
        assertEquals(Destinations.HOME, entries.get(0), "the you-are-here node leads the list");
        for (int scroll = 0; scroll <= entries.size() - VISIBLE; scroll++) {
            for (int row = 0; row < VISIBLE; row++) {
                assertEquals(entries.get(scroll + row), DestinationList.indexAt(entries, scroll, row, VISIBLE));
            }
        }
        int last = DestinationList.clampScroll(99, entries.size(), VISIBLE);
        assertEquals(Destinations.NYXARA, DestinationList.indexAt(entries, last, VISIBLE - 1, VISIBLE));
        assertEquals(-1, DestinationList.indexAt(entries, 0, VISIBLE, VISIBLE));
    }

    @Test
    @DisplayName("previews follow the reachable worlds; scrolling keeps the selection in view")
    void previewsAndScroll() {
        int mask = Destinations.availableMask(RocketTier.TIER_1, ModDimensions.OVERWORLD);
        List<Integer> entries = DestinationList.entries(mask, Destinations.HOME, Destinations.all().size(),
                List.of(Destinations.GREENXERTZ, Destinations.MARS));
        assertEquals(List.of(Destinations.HOME, Destinations.STATION, Destinations.LUNA, Destinations.GREENXERTZ,
                Destinations.MARS), entries);
        assertEquals(2, DestinationList.scrollToShow(entries, 0, Destinations.MARS, VISIBLE));
        assertEquals(1, DestinationList.scrollToShow(entries, 2, Destinations.STATION, VISIBLE));
        assertEquals(1, DestinationList.scrollToShow(entries, 1, Destinations.LUNA, VISIBLE));
        assertEquals(0, DestinationList.clampScroll(-3, entries.size(), VISIBLE));
        assertEquals(0, DestinationList.clampScroll(4, 2, VISIBLE));
    }
}
