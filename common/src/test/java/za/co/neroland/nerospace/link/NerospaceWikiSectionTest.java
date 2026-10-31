package za.co.neroland.nerospace.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The public NeroLink {@code wiki} section (wiki contract v1), served from the bundled {@code wiki/} pages. */
class NerospaceWikiSectionTest {

    private static JsonObject wiki(Map<String, String> params) {
        // Any requester gets the same answer: the pages are public, and no server is needed.
        return new NerospaceLinkSnapshots().snapshot(UUID.randomUUID(), NerospaceLinkModule.SECTION_WIKI, params);
    }

    @Test
    @DisplayName("the wiki section is advertised")
    void advertised() {
        assertTrue(new NerospaceLinkSnapshots().sections().contains(NerospaceLinkModule.SECTION_WIKI));
        assertEquals("wiki", NerospaceLinkModule.SECTION_WIKI);
    }

    @Test
    @DisplayName("the index lists the bundled pages with their first heading as title, never wiki chrome")
    void index() {
        JsonObject index = wiki(Map.of());
        assertEquals("nerospace", index.get("mod").getAsString());
        assertTrue(index.has("asOf"));
        JsonArray pages = index.getAsJsonArray("pages");
        Set<String> slugs = new HashSet<>();
        for (JsonElement e : pages) {
            JsonObject p = e.getAsJsonObject();
            slugs.add(p.get("slug").getAsString());
            assertFalse(p.get("title").getAsString().isBlank());
        }
        assertTrue(slugs.containsAll(Set.of("Home", "Rockets", "Nyxara", "Upgrading-to-2.0", "Items")), slugs.toString());
        assertFalse(slugs.contains("_Sidebar"), "the sidebar is wiki chrome, not a page");
        assertTrue(pages.size() > 40, "the generated index (not the fallback) is bundled: " + pages.size());
        assertEquals("Nerospace Wiki", WikiLibrary.titleOf("Home"), "title is the page's first # heading");
    }

    @Test
    @DisplayName("a page returns its markdown; the same for every requester")
    void page() {
        JsonObject page = wiki(Map.of("page", "Rockets"));
        assertEquals("nerospace", page.get("mod").getAsString());
        assertEquals("Rockets", page.get("slug").getAsString());
        assertEquals("markdown", page.get("format").getAsString());
        assertTrue(page.get("content").getAsString().startsWith("# Rockets"));
        assertEquals(page.get("content"), wiki(Map.of("page", "Rockets")).get("content"));
    }

    @Test
    @DisplayName("unknown or unsafe slugs answer an error object and never read outside the wiki")
    void unknown() {
        assertTrue(WikiLibrary.isSafeSlug("Upgrading-to-2.0"));
        assertFalse(WikiLibrary.isSafeSlug(".."));
        assertFalse(WikiLibrary.isSafeSlug("a..b"));
        assertFalse(WikiLibrary.isSafeSlug(".hidden"));
        for (String slug : new String[] { "___not_a_page___", "../lang/en_us", "..", "Home.md", "_Sidebar" }) {
            JsonObject page = wiki(Map.of("page", slug));
            assertEquals("unknown page", page.get("error").getAsString(), slug);
            assertFalse(page.has("content"), slug);
        }
    }
}
