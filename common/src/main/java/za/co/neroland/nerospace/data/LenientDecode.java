package za.co.neroland.nerospace.data;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * One warning when a save decodes a value this version does not know and keeps the default instead.
 * The raw text is never logged: a corrupt field could hold a name. Each field warns once per session.
 */
public final class LenientDecode {

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private LenientDecode() {
    }

    /** True when {@code raw} is present and was not one of the known names. Blank and null are absent, not unknown. */
    public static boolean unknownSaved(String raw, boolean recognized) {
        return raw != null && !raw.isBlank() && !recognized;
    }

    /** Logs once per {@code field} (a fixed label such as {@code "station class"}, never the saved text). */
    public static void warnOnce(String field) {
        if (field == null || field.isBlank() || !WARNED.add(field)) {
            return;
        }
        NerospaceCommon.LOGGER.warn(
                "[Nerospace] Ignored an unknown saved {} value and kept the default. Further values of this field are not logged.",
                field);
    }
}
