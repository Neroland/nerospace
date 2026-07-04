package za.co.neroland.nerospace.config;

import za.co.neroland.nerolandcore.config.ConfigManager;
import za.co.neroland.nerolandcore.config.ConfigSchema;
import za.co.neroland.nerolandcore.config.ConfigValue;

/**
 * Nerospace's config, now backed by Neroland Core's shared {@link ConfigManager}. Core owns the single
 * {@code config/nerospace.properties} file (defaults, range validation, in-place key migration, the
 * {@code /neroland config reload} hot-reload, and server-authoritative client sync) — Nerospace just
 * declares the schema once and reads the typed {@link ConfigValue} handles through the same static getters
 * the rest of the mod already calls, so no machine code changed.
 *
 * <p>The key names are unchanged from the previous standalone properties file, so an existing
 * {@code nerospace.properties} migrates seamlessly (Core preserves user edits, only rewriting when a key
 * is missing).</p>
 *
 * <p><b>POPIA/GDPR:</b> the gameplay-balance multipliers are <i>server-authoritative</i> (a client uses
 * the server's values when connected). {@code telemetryEnabled} is deliberately <b>not</b>
 * server-authoritative — anonymous crash reporting is a per-client opt-out that a server must never force
 * on or off. The server-sync snapshot carries only config keys/values, never player data.</p>
 */
public final class NerospaceConfig {

    /** Multiplier range (mirrors the prior config): 0.1× .. 10×. */
    private static final double MULT_MIN = 0.1D;
    private static final double MULT_MAX = 10.0D;

    private static final ConfigSchema SCHEMA = ConfigSchema.create("nerospace",
            "Nerospace config (managed by Neroland Core). All multipliers 0.1..10, default 1.");

    private static final ConfigValue<Boolean> TELEMETRY = SCHEMA.bool("telemetryEnabled", true, false,
            "send anonymous, Nerospace-only crash reports (Sentry, EU servers) - stack trace, "
            + "mod/MC/loader/OS/Java versions, your other installed mods, this mod's config, recent in-game "
            + "actions, anonymous stability/timing; no IP, username, UUID, world data or chat; file paths "
            + "scrubbed of your account name. false = opt out of all of it. See PRIVACY.md");
    private static final ConfigValue<Double> ENERGY_RATE = SCHEMA.doubleRange("energyRateMultiplier",
            1.0D, MULT_MIN, MULT_MAX, true, "scales FE/tick of all generators (combustion, passive, solar)");
    private static final ConfigValue<Double> OXYGEN_DRAIN = SCHEMA.doubleRange("oxygenDrainMultiplier",
            1.0D, MULT_MIN, MULT_MAX, true, "scales how fast air drains off a safe zone");
    private static final ConfigValue<Double> OXYGEN_CAPACITY = SCHEMA.doubleRange("oxygenCapacityMultiplier",
            1.0D, MULT_MIN, MULT_MAX, true, "scales player + suit air capacity");
    private static final ConfigValue<Double> FUEL_COST = SCHEMA.doubleRange("fuelCostMultiplier",
            1.0D, MULT_MIN, MULT_MAX, true, "scales fuel burned per rocket launch (clamped to the tank)");
    private static final ConfigValue<Double> MACHINE_SPEED = SCHEMA.doubleRange("machineSpeedMultiplier",
            1.0D, MULT_MIN, MULT_MAX, true, "scales machine work speed (higher = faster)");
    private static final ConfigValue<Double> GRAVITY = SCHEMA.doubleRange("gravityMultiplier",
            1.0D, MULT_MIN, MULT_MAX, true, "global scale on per-dimension/per-biome gravity (higher = stronger)");
    private static final ConfigValue<Integer> QUARRY_MAX_SIDE = SCHEMA.intRange("quarryMaxSide",
            64, 4, 64, true, "max quarry landmark claim side in blocks (4..64, default 64)");
    private static final ConfigValue<Integer> QUARRY_FRAME_DECAY_TICKS = SCHEMA.intRange("quarryFrameDecayTicks",
            600, 20, 24000, true, "base delay in ticks before an orphaned quarry frame block crumbles and "
            + "drops its casing; each block waits base + rand(base*7), so the default 600 spreads the ring's "
            + "decay over ~30s..4min (20..24000)");
    private static final ConfigValue<Boolean> ALIEN_RAIDS = SCHEMA.bool("alienRaidsEnabled", true, true,
            "allow hostile mobs to raid alien villages at night (true by default; false = opt out)");
    private static final ConfigValue<Boolean> TERRAFORMER_FORCE_LOAD = SCHEMA.bool("terraformerForceLoadEnabled",
            false, true, "a running Terraformer keeps a small window of chunks loaded so it terraforms "
            + "while you are away (false by default; opt-in - costs server memory/TPS while it runs)");

    private static volatile boolean loaded;

    private NerospaceConfig() {
    }

    public static boolean isTelemetryEnabled() {
        return TELEMETRY.get();
    }

    public static double energyRateMultiplier() {
        return ENERGY_RATE.get();
    }

    public static double oxygenDrainMultiplier() {
        return OXYGEN_DRAIN.get();
    }

    public static double oxygenCapacityMultiplier() {
        return OXYGEN_CAPACITY.get();
    }

    public static double fuelCostMultiplier() {
        return FUEL_COST.get();
    }

    public static double machineSpeedMultiplier() {
        return MACHINE_SPEED.get();
    }

    /** Global multiplier applied to every resolved gravity factor (default 1.0 = unchanged). */
    public static double gravityMultiplier() {
        return GRAVITY.get();
    }

    /** Configured quarry footprint side, clamped to a practical 4..64 block range. */
    public static int quarryMaxSide() {
        return QUARRY_MAX_SIDE.get();
    }

    /**
     * Base scheduled-tick delay before an orphaned quarry frame block crumbles (each block adds a
     * random spread of up to 7× this base, so the ring decays block by block; default 600 ≈ 30s–4min).
     */
    public static int quarryFrameDecayTicks() {
        return QUARRY_FRAME_DECAY_TICKS.get();
    }

    /** Whether config-gated night raids on alien villages are enabled (default true; opt-out). */
    public static boolean alienRaidsEnabled() {
        return ALIEN_RAIDS.get();
    }

    /** Whether the Terraformer actively force-loads a small window of chunks while running (default false; opt-in). */
    public static boolean terraformerForceLoadEnabled() {
        return TERRAFORMER_FORCE_LOAD.get();
    }

    /**
     * Inverse-scales a base work interval by the machine-speed multiplier: a higher speed yields a
     * SHORTER interval, clamped to ≥1 tick (so 10× can't produce a zero-tick interval). Mirrors the root
     * {@code Tuning} interval-clamp contract.
     */
    public static int scaleInterval(int baseTicks, double speedMultiplier) {
        return Math.max(1, (int) Math.round(baseTicks / Math.max(0.01D, speedMultiplier)));
    }

    /**
     * Applies a balance multiplier to a base integer rate, clamped to a minimum of 1 so an extreme low
     * multiplier (0.1×) can never zero a rate (mirrors the root {@code Tuning} clamping contract).
     */
    public static int scale(int base, double multiplier) {
        return Math.max(1, (int) Math.round(base * multiplier));
    }

    /**
     * Registers the schema with Neroland Core, which reads {@code nerospace.properties} immediately
     * (creating it with defaults if absent). Idempotent — safe to call once at mod init, before telemetry
     * checks {@link #isTelemetryEnabled()}.
     */
    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        ConfigManager.register(SCHEMA);
    }
}
