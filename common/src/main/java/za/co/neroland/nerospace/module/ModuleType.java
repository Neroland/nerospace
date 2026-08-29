package za.co.neroland.nerospace.module;

/**
 * The kinds of cross-machine upgrade module (the quarry is the first consumer, but the system is
 * machine-agnostic: any machine can embed a {@link MachineModules} and read the same effects). Each
 * module is its own registered item ({@link UpgradeModuleItem}); a machine counts the modules in its
 * slots and aggregates their effects.
 */
public enum ModuleType {

    /** Raises the per-cycle work cap so the machine does more when fed more power. */
    SPEED,
    /** Lowers the energy cost per unit of work. */
    EFFICIENCY,
    /** Applies a Fortune level to harvested blocks (mutually overridden by {@link #SILK_TOUCH}). */
    FORTUNE,
    /** Harvests blocks with Silk Touch (takes precedence over {@link #FORTUNE}). */
    SILK_TOUCH,
    /**
     * Destroys liquid source blocks outright instead of buffering them, so the machine's fluid buffer
     * stays free for a fluid the player actually wants to collect (and so a dig over an ocean isn't
     * spent shuttling water). Declared last on purpose: {@link #byOrdinal} makes the ordinal the wire
     * form of this enum, so new constants must only ever be appended.
     */
    EVAPORATOR;

    public static ModuleType byOrdinal(int ordinal) {
        ModuleType[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return SPEED;
        }
        return values[ordinal];
    }
}
