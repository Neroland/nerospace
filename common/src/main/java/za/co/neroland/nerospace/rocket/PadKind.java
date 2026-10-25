package za.co.neroland.nerospace.rocket;

import java.util.Locale;

/**
 * What a {@link PadRegistry} node is (decision 001 §6.2, 005 §1.2). Persisted <b>by name</b>; an absent or
 * unknown name loads as {@link #LAUNCH_PAD}, so every 1.6.0 node keeps its meaning.
 */
public enum PadKind {
    /** A name-tagged rocket launch pad (the only kind 1.6.0 knew). */
    LAUNCH_PAD,
    /** A Docking Ring. Preferred for station arrivals; a plain pad everywhere else. */
    DOCK,
    /** An auto-placed Landing Pod. Never a Rocket-screen choice; only reused by the pod step. */
    POD;

    /** Lenient by-name lookup: {@code null}/unknown → {@link #LAUNCH_PAD}. */
    public static PadKind byName(String name) {
        if (name == null) {
            return LAUNCH_PAD;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return LAUNCH_PAD;
        }
    }

    /** Whether nodes of this kind appear in the Rocket screen's pad cycler and resolver steps 1–4. */
    public boolean selectable() {
        return this != POD;
    }
}
