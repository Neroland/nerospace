package za.co.neroland.nerospace.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The grinder's rows. The six 2.4.0 rows are a pure id table; the rows from before 2.4.0 are item-identity branches in
 * {@code GrinderRecipes.getResult}, which cannot run without a game registry, so their snapshot is taken from the
 * source text: every branch must still be there, word for word, and ahead of the table lookup.
 */
class GrinderRecipesTest {

    /** The pre-2.4.0 branches of {@code getResult}, in order: the input test and the result line. */
    private static final List<String[]> LEGACY = List.of(
            new String[] {"input.is(ModItems.NEROSIUM_ORE_ITEM.get())", "new ItemStack(ModItems.NEROSIUM_DUST.get(), 2)"},
            new String[] {"input.is(ModItems.DEEPSLATE_NEROSIUM_ORE_ITEM.get())",
                    "new ItemStack(ModItems.NEROSIUM_DUST.get(), 2)"},
            new String[] {"input.is(ModItems.RAW_NEROSIUM.get())", "new ItemStack(ModItems.NEROSIUM_DUST.get(), 2)"},
            new String[] {"input.is(ModItems.NEROSIUM_INGOT.get())", "new ItemStack(ModItems.NEROSIUM_DUST.get(), 1)"},
            new String[] {"input.is(ModItems.NEROSTEEL_ORE_ITEM.get())", "new ItemStack(ModItems.RAW_NEROSTEEL.get(), 2)"},
            new String[] {"input.is(ModItems.CINDRITE_ORE_ITEM.get())", "new ItemStack(ModItems.CINDRITE.get(), 2)"},
            new String[] {"input.is(ModItems.GLACITE_ORE_ITEM.get())", "new ItemStack(ModItems.GLACITE.get(), 2)"},
            new String[] {"input.is(ModItems.XERTZ_QUARTZ_ORE_ITEM.get())",
                    "new ItemStack(ModItems.XERTZ_QUARTZ.get(), 2)"});

    private static Path source() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = dir; p != null; p = p.getParent()) {
            Path candidate = p.resolve("common/src/main/java/za/co/neroland/nerospace/machine/GrinderRecipes.java");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    @Test
    @DisplayName("the six Europa rows return the stated stacks")
    void europaRows() {
        assertEquals(List.of(
                new GrinderRows.Row("nerolandcore:raw_vent_sulfide", "nerospace:sulfur", 2),
                new GrinderRows.Row("nerospace:smoker_chimney", "nerolandcore:raw_vent_sulfide", 1),
                new GrinderRows.Row("nerospace:deuterium_ice", "nerospace:deuterium_shard", 1),
                new GrinderRows.Row("nerospace:oxidant_ice", "nerospace:oxidant_crystal", 1),
                new GrinderRows.Row("nerolandcore:brine_salt_ore", "nerolandcore:brine_salt", 2),
                new GrinderRows.Row("nerolandcore:vent_sulfide_ore", "nerolandcore:raw_vent_sulfide", 1)), GrinderRows.EUROPA);
        Set<String> inputs = new HashSet<>();
        for (GrinderRows.Row row : GrinderRows.EUROPA) {
            assertTrue(inputs.add(row.input()), "one row per input: " + row.input());
            assertEquals(row, GrinderRows.find(row.input()));
            assertTrue(row.count() >= 1 && row.count() <= 2, row.input());
        }
        assertNull(GrinderRows.find("nerolandcore:europium_ore"));
        assertNull(GrinderRows.find("nerolandcore:raw_europium"));
        assertNull(GrinderRows.find("nerospace:europium_lamp"));
        assertNull(GrinderRows.find("minecraft:stone"));
    }

    @Test
    @DisplayName("no row feeds itself, and the one chain (ore to raw sulfide to sulfur) ends")
    void noLoop() {
        for (GrinderRows.Row row : GrinderRows.EUROPA) {
            String next = row.output();
            for (int hops = 0; hops < GrinderRows.EUROPA.size() && GrinderRows.find(next) != null; hops++) {
                next = GrinderRows.find(next).output();
            }
            assertNull(GrinderRows.find(next), row.input() + " never settles");
        }
    }

    @Test
    @DisplayName("every pre-2.4.0 row is still in getResult, unchanged and ahead of the Europa lookup; all() lists both")
    void legacySnapshot() throws IOException {
        Path file = source();
        Assumptions.assumeTrue(file != null, "common sources not found");
        String text = Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n");
        int body = text.indexOf("public static ItemStack getResult(ItemStack input)");
        assertTrue(body > 0);
        int lookup = text.indexOf("GrinderRows.find(", body);
        assertTrue(lookup > body, "the Europa rows are resolved in getResult");
        int at = body;
        for (String[] row : LEGACY) {
            int test = text.indexOf(row[0], at);
            assertTrue(test >= at && test < lookup, row[0]);
            int result = text.indexOf("return " + row[1] + ";", test);
            assertTrue(result > test && result < lookup, row[0] + " -> " + row[1]);
            at = test;
        }
        String all = text.substring(text.indexOf("public static List<Grinding> all()"), body);
        for (String item : List.of("NEROSIUM_ORE_ITEM", "DEEPSLATE_NEROSIUM_ORE_ITEM", "RAW_NEROSIUM", "NEROSIUM_INGOT",
                "NEROSTEEL_ORE_ITEM", "CINDRITE_ORE_ITEM", "GLACITE_ORE_ITEM", "XERTZ_QUARTZ_ORE_ITEM")) {
            assertTrue(all.contains("ModItems." + item + ".get()"), "all() lists " + item);
        }
        assertTrue(all.contains("GrinderRows.EUROPA"), "all() lists the Europa rows");
        assertNotNull(GrinderRows.find("nerospace:smoker_chimney"));
    }
}
