package za.co.neroland.nerospace.compat.jei;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Runs the optional "recipe extras" (animated arrow/flame, FE + time text) of a Nerospace JEI category so
 * that an installed JEI older than the API we compile against cannot crash the recipe screen.
 *
 * <p>JEI resolves a category's extras lazily, the first time the page is drawn. A JEI build that lacks a
 * widget method we were compiled against throws a {@link LinkageError} ({@code NoSuchMethodError} /
 * {@code AbstractMethodError} / {@code IncompatibleClassChangeError}) from inside JEI's input handler, which
 * takes the whole client down (MC-NEROSPACE-T, MC-NEROSPACE-V). The extras are decoration only — the
 * slots, which carry the actual recipe, are built separately in {@code setRecipe} — so on such a JEI the
 * page simply shows without them. The loader metadata also declares a minimum JEI per Minecraft line; this
 * is the safety net for anything that slips past it.</p>
 *
 * <p>Logs one warning per session, naming only the JEI API mismatch — no player data.</p>
 */
final class JeiExtras {

    private static volatile boolean warned;

    private JeiExtras() {
    }

    static void run(String category, Runnable extras) {
        try {
            extras.run();
        } catch (LinkageError e) {
            if (!warned) {
                warned = true;
                NerospaceCommon.LOGGER.warn("[Nerospace] JEI category '{}' skipped its extra widgets: the "
                        + "installed JEI is older than the API this build targets ({}). Update JEI to restore "
                        + "the energy/time readout.", category, e.toString());
            }
        }
    }
}
