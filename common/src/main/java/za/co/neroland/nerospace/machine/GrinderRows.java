package za.co.neroland.nerospace.machine;

import java.util.List;

/**
 * The grinder's Europa rows as plain ids (no Minecraft types, JUnit-tested). {@link GrinderRecipes} resolves them
 * after its older rows, so nothing that ground before grinds differently. Each row gives the block's or item's
 * smallest ordinary yield, so grinding never beats mining.
 */
public final class GrinderRows {

    /** One row: an input item id, an output item id and the output count. */
    public record Row(String input, String output, int count) {
    }

    /** Appended in 2.4.0; order is the display order. */
    public static final List<Row> EUROPA = List.of(
            new Row("nerospace:raw_vent_sulfide", "nerospace:sulfur", 2),
            new Row("nerospace:smoker_chimney", "nerospace:raw_vent_sulfide", 1),
            new Row("nerospace:deuterium_ice", "nerospace:deuterium_shard", 1),
            new Row("nerospace:oxidant_ice", "nerospace:oxidant_crystal", 1),
            new Row("nerospace:brine_salt_ore", "nerospace:brine_salt", 2),
            new Row("nerospace:vent_sulfide_ore", "nerospace:raw_vent_sulfide", 1));

    private GrinderRows() {
    }

    /** The row for an input id, or null. */
    public static Row find(String inputId) {
        for (Row r : EUROPA) {
            if (r.input().equals(inputId)) {
                return r;
            }
        }
        return null;
    }
}
