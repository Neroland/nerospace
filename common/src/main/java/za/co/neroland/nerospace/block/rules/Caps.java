package za.co.neroland.nerospace.block.rules;

/** Config may lower a locked cap, never raise it. */
public final class Caps {

    private Caps() {
    }

    /** {@code min(locked, configured)}, floored at 0. */
    public static int lower(int locked, int configured) {
        return Math.max(0, Math.min(locked, configured));
    }
}
