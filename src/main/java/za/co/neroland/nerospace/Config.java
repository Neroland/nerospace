package za.co.neroland.nerospace;

import java.util.List;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Nerospace common config. Pre-1.0 refactor (BREAKING — the old flat key list is gone, see
 * {@code wiki/Configuration.md}): the mod owns its base balance numbers in {@link Tuning}; packs
 * tune them through five clamped multipliers. Everything else that remains here is genuinely
 * absolute — booleans, radii, performance caps, the advanced oxygen-field simulation tuning and
 * client visual preferences.
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // --- Telemetry / crash reporting (see PRIVACY.md) -----------------------

    public static final ModConfigSpec.BooleanValue TELEMETRY_ENABLED = BUILDER
            .comment(
                    "Send anonymous error reports for Nerospace bugs to the developers (Sentry, EU servers).",
                    "Only errors caused by Nerospace are sent: stack trace, mod/Minecraft/NeoForge versions,",
                    "OS and Java version. No IP address, username, UUID, world data or chat is ever sent;",
                    "file paths are scrubbed of your account name. Set to false to opt out (takes effect",
                    "immediately on config reload). Full details: PRIVACY.md in the mod repository.")
            .define("telemetryEnabled", true);

    // --- Balance multipliers -------------------------------------------------
    //
    // The base numbers live in code (Tuning.java) and are documented in wiki/Configuration.md.
    // All scaled rates/costs are clamped to >= 1 internally, so extreme values can't zero a rate,
    // stall progression or divide by zero.

    public static final ModConfigSpec.DoubleValue OXYGEN_DRAIN_MULTIPLIER = BUILDER
            .comment("Scales how fast oxygen is consumed: bare-lungs drain, suit tank drain and",
                    "suffocation damage. >1 = harsher planets, <1 = gentler.")
            .defineInRange("oxygenDrainMultiplier", 1.0D, 0.1D, 10.0D);

    public static final ModConfigSpec.DoubleValue OXYGEN_CAPACITY_MULTIPLIER = BUILDER
            .comment("Scales air capacities: the player's oxygen supply and the Tier 2 suit tank.")
            .defineInRange("oxygenCapacityMultiplier", 1.0D, 0.1D, 10.0D);

    public static final ModConfigSpec.DoubleValue ENERGY_RATE_MULTIPLIER = BUILDER
            .comment("Scales the energy & storage economy: generator FE/t output, energy pipe",
                    "throughput, and every buffer/tank capacity (battery, fluid/gas/fuel tanks,",
                    "pipe buffers, machine energy buffers, rocket fuel tanks).")
            .defineInRange("energyRateMultiplier", 1.0D, 0.1D, 10.0D);

    public static final ModConfigSpec.DoubleValue FUEL_COST_MULTIPLIER = BUILDER
            .comment("Scales consumable costs: rocket fuel per launch (clamped to tank size),",
                    "airlock oxygen-gas per air unit, and machine energy costs (Terraformer FE per",
                    "block, Grinder FE/t, Oxygen Generator FE per mB).")
            .defineInRange("fuelCostMultiplier", 1.0D, 0.1D, 10.0D);

    public static final ModConfigSpec.DoubleValue MACHINE_SPEED_MULTIPLIER = BUILDER
            .comment("Scales machine working speed: grinder progress, oxygen make/emit rate, fuel",
                    "tank pump rate, airlock refill rate, fluid/gas pipe throughput, item pipe",
                    "travel/extraction speed, and the Terraformer work interval.")
            .defineInRange("machineSpeedMultiplier", 1.0D, 0.1D, 10.0D);

    // --- Atmosphere ----------------------------------------------------------

    public static final ModConfigSpec.BooleanValue ATMOSPHERE_DAMAGE_ENABLED = BUILDER
            .comment("Whether the thin atmosphere on Nerospace planets and the station hurts unprotected players.")
            .define("atmosphereDamageEnabled", true);

    public static final ModConfigSpec.IntValue ATMOSPHERE_SAFE_RADIUS = BUILDER
            .comment("Blocks from a Rocket Launch Pad treated as a safe, pressurised zone.")
            .defineInRange("atmosphereSafeRadius", 6, 0, 32);

    // --- Oxygen: structural radii / caps -------------------------------------

    public static final ModConfigSpec.IntValue OXYGEN_BUBBLE_RADIUS = BUILDER
            .comment("Falloff radius (blocks) of the breathable bubble a generator pressurises in OPEN / "
                    + "leaky space (the air escapes toward the opening). A fully sealed room fills "
                    + "completely regardless. Bigger = more usable coverage when not perfectly sealed.")
            .defineInRange("oxygenBubbleRadius", 14, 0, 32);

    public static final ModConfigSpec.IntValue OXYGEN_AIRLOCK_RADIUS = BUILDER
            .comment("Radius (blocks) within which a worn Oxygen Suit refills its air tank from a Gas "
                    + "Tank or Oxygen Generator holding Oxygen — a tank by the base door acts as an "
                    + "airlock. 0 disables airlock refilling.")
            .defineInRange("oxygenAirlockRadius", 3, 0, 16);

    // --- Oxygen field simulation (ADVANCED) ----------------------------------
    //
    // Simulation tuning, not balance. Wrong values can break terraforming/oxygen behaviour —
    // leave at defaults unless you are debugging server performance. See OXYGEN_TERRAFORM_DESIGN.md.

    public static final ModConfigSpec.IntValue OXYGEN_MAX_CONCENTRATION = BUILDER
            .comment("ADVANCED. Maximum per-block oxygen concentration in the field (0..this). 15 is plenty.")
            .defineInRange("oxygenMaxConcentration", 15, 1, 15);

    public static final ModConfigSpec.IntValue OXYGEN_BREATHABLE_THRESHOLD = BUILDER
            .comment("ADVANCED. Field concentration at/above which a cell is breathable.")
            .defineInRange("oxygenBreathableThreshold", 6, 1, 15);

    public static final ModConfigSpec.DoubleValue OXYGEN_DIFFUSION_RATE = BUILDER
            .comment("ADVANCED. Fraction of the neighbour gradient that flows into a cell each sim step (0..0.4).")
            .defineInRange("oxygenDiffusionRate", 0.22D, 0.0D, 0.4D);

    public static final ModConfigSpec.DoubleValue OXYGEN_DECAY_PER_STEP = BUILDER
            .comment("ADVANCED. Oxygen bled to the thin atmosphere/vacuum each sim step. The sink that keeps "
                    + "open-air bubbles finite; small values let big sealed rooms fill.")
            .defineInRange("oxygenDecayPerStep", 0.18D, 0.0D, 5.0D);

    public static final ModConfigSpec.IntValue OXYGEN_SIM_INTERVAL_TICKS = BUILDER
            .comment("ADVANCED. Server ticks between oxygen-field relaxation passes (5 ~= 4 Hz).")
            .defineInRange("oxygenSimIntervalTicks", 5, 1, 100);

    public static final ModConfigSpec.IntValue OXYGEN_MAX_ACTIVE_CELLS_PER_SOURCE = BUILDER
            .comment("ADVANCED. Safety cap on active (simulated) cells per source. A generator dumped into open "
                    + "vacuum stops expanding its frontier at this size instead of spiking the tick.")
            .defineInRange("oxygenMaxActiveCellsPerSource", 4096, 64, 65536);

    public static final ModConfigSpec.IntValue OXYGEN_LEAK_RANGE = BUILDER
            .comment("ADVANCED. Max distance (blocks, through connected air) the flood-fill searches from a "
                    + "generator for leaks / room walls. A generator pressurises at most this far. "
                    + "Smaller = cheaper. Default 16.")
            .defineInRange("oxygenLeakRange", 16, 4, 64);

    public static final ModConfigSpec.IntValue OXYGEN_EVAPORATE_SECONDS = BUILDER
            .comment("ADVANCED. How long oxygen takes to evaporate once it is no longer supplied (generator out "
                    + "of fuel/broken) or after it starts leaking out an opening.")
            .defineInRange("oxygenEvaporateSeconds", 10, 1, 120);

    public static final ModConfigSpec.IntValue OXYGEN_SYNC_RADIUS = BUILDER
            .comment("ADVANCED. Blocks around a source within which the field is simulated and synced to clients. "
                    + "Sources with no player in range pause (state persists).")
            .defineInRange("oxygenSyncRadius", 32, 8, 128);

    // --- Client visuals -------------------------------------------------------

    public static final ModConfigSpec.EnumValue<OxygenVisualQuality> OXYGEN_VISUAL_QUALITY = BUILDER
            .comment("Client oxygen visuals: OFF (none), MINIMAL (HUD + sparse particles), "
                    + "FULL (particles + haze tint + boundary shimmer + HUD).")
            .defineEnum("oxygenVisualQuality", OxygenVisualQuality.FULL);

    public static final ModConfigSpec.DoubleValue OXYGEN_PARTICLE_INTENSITY = BUILDER
            .comment("Drifting-particle density multiplier (0 disables just this layer).")
            .defineInRange("oxygenParticleIntensity", 1.0D, 0.0D, 4.0D);

    public static final ModConfigSpec.DoubleValue OXYGEN_HAZE_INTENSITY = BUILDER
            .comment("Soft haze/fog-tint alpha multiplier inside breathable air (0 disables the layer).")
            .defineInRange("oxygenHazeIntensity", 1.0D, 0.0D, 2.0D);

    public static final ModConfigSpec.DoubleValue OXYGEN_BOUNDARY_INTENSITY = BUILDER
            .comment("Boundary-shimmer membrane alpha multiplier (0 disables the layer).")
            .defineInRange("oxygenBoundaryIntensity", 1.0D, 0.0D, 2.0D);

    public static final ModConfigSpec.BooleanValue OXYGEN_DEBUG_LOG = BUILDER
            .comment("Verbose, NON-personal oxygen/terraform logging (dimension, positions, anonymous "
                    + "counts only — never player identifiers). Off by shipped default (POPIA/GDPR).")
            .define("oxygenDebugLog", false);

    // --- Terraformer: feature toggles + performance caps ----------------------

    public static final ModConfigSpec.BooleanValue TERRAFORM_PLANTS_ENABLED = BUILDER
            .comment("Whether terraforming scatters grass/flowers and saplings on converted ground.")
            .define("terraformPlantsEnabled", true);

    public static final ModConfigSpec.BooleanValue TERRAFORM_WATER_ENABLED = BUILDER
            .comment("Whether the Hydrated terraform stage fills basins below the water table with water.")
            .define("terraformWaterEnabled", true);

    public static final ModConfigSpec.IntValue TERRAFORM_WATER_MAX_DEPTH = BUILDER
            .comment("Deepest basin (blocks below the water table) the Hydrated stage will fill; "
                    + "deeper chasms are skipped entirely.")
            .defineInRange("terraformWaterMaxDepth", 8, 1, 32);

    public static final ModConfigSpec.BooleanValue TERRAFORM_DRIFT_ENABLED = BUILDER
            .comment("Cosmetic drift: settled terraformed land keeps sprouting sparse ground cover "
                    + "even while the machine idles (pure garnish, budgeted; no gameplay effect).")
            .define("terraformDriftEnabled", true);

    public static final ModConfigSpec.IntValue TERRAFORM_DRIFT_PER_SECOND = BUILDER
            .comment("Cosmetic-drift placement budget per second per level (0 also disables).")
            .defineInRange("terraformDriftPerSecond", 4, 0, 64);

    public static final ModConfigSpec.BooleanValue TERRAFORM_FAUNA_ENABLED = BUILDER
            .comment("Whether the Living terraform stage seeds starter herds of the planet's "
                    + "livestock species (biome spawn settings still apply either way).")
            .define("terraformFaunaEnabled", true);

    public static final ModConfigSpec.BooleanValue TERRAFORM_RESOURCES_ENABLED = BUILDER
            .comment("Whether a Tier-3 Terraformer seeds ores into the converted subsurface (low rate).")
            .define("terraformResourcesEnabled", true);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> TERRAFORM_RESOURCE_ORES = BUILDER
            .comment("Ore block ids the Tier-3 Terraformer may seed. Defaults to Nerospace ores.")
            .defineListAllowEmpty("terraformResourceOres",
                    List.of("nerospace:nerosteel_ore", "nerospace:xertz_quartz_ore", "nerospace:nerosium_ore"),
                    () -> "nerospace:nerosteel_ore", Config::validateResourceLocation);

    public static final ModConfigSpec.IntValue TERRAFORM_MAX_COLUMNS_PER_TICK = BUILDER
            .comment("Hard per-tick work cap (columns converted) regardless of tier, to protect TPS.")
            .defineInRange("terraformMaxColumnsPerTick", 48, 1, 4096);

    public static final ModConfigSpec.BooleanValue TERRAFORM_FORCE_LOAD_CHUNKS = BUILDER
            .comment("ACTIVE terraforming: force-load a bounded arc around the working frontier so it "
                    + "continues while you are away. Off by default — force-loading is a TPS footgun.")
            .define("terraformForceLoadChunks", false);

    public static final ModConfigSpec.IntValue TERRAFORM_MAX_FORCED_CHUNKS = BUILDER
            .comment("Guard on how many chunks active terraforming may force-load at once.")
            .defineInRange("terraformMaxForcedChunks", 16, 0, 256);

    // --- Meteor events (meteor-events-design.md) -----------------------------
    // Spawn pacing + loot tunables. Defaults give roughly one natural meteor every ~2-3 play-hours
    // per active level; tune for busier or quieter skies.

    public static final ModConfigSpec.BooleanValue METEOR_NATURAL_SPAWN = BUILDER
            .comment("Whether meteors fall naturally near players (the creative Meteor Caller works either way).")
            .define("meteorNaturalSpawn", true);

    public static final ModConfigSpec.IntValue METEOR_AVG_INTERVAL_SECONDS = BUILDER
            .comment("Average seconds between natural meteor impacts on an eligible dimension with players online.",
                    "Default 9000 (~2.5 hours). Each interval is randomised 0.66x..1.33x so impacts feel irregular.")
            .defineInRange("meteorAvgIntervalSeconds", 9000, 60, 1_000_000);

    public static final ModConfigSpec.IntValue METEOR_WARNING_SECONDS = BUILDER
            .comment("Warning window: seconds a meteor is tracked as 'incoming' before it actually falls.")
            .defineInRange("meteorWarningSeconds", 30, 0, 600);

    public static final ModConfigSpec.IntValue METEOR_MIN_DISTANCE = BUILDER
            .comment("Minimum horizontal distance (blocks) from the anchor player a meteor targets.")
            .defineInRange("meteorMinDistance", 200, 0, 2000);

    public static final ModConfigSpec.IntValue METEOR_MAX_DISTANCE = BUILDER
            .comment("Maximum horizontal distance (blocks) from the anchor player a meteor targets.")
            .defineInRange("meteorMaxDistance", 500, 16, 4000);

    public static final ModConfigSpec.IntValue METEOR_CRATER_RADIUS = BUILDER
            .comment("Radius (blocks) of the small crater a meteor carves. Kept modest to avoid griefing builds.")
            .defineInRange("meteorCraterRadius", 3, 1, 8);

    public static final ModConfigSpec.IntValue METEOR_MAX_ACTIVE_SITES = BUILDER
            .comment("Max simultaneous scheduled/falling meteors tracked per dimension.")
            .defineInRange("meteorMaxActiveSites", 4, 1, 64);

    public static final ModConfigSpec.IntValue METEOR_LOOT_BONUS_ROLLS = BUILDER
            .comment("Weighted bonus loot rolls in a meteor core, on top of its guaranteed alien fragments.")
            .defineInRange("meteorLootBonusRolls", 3, 0, 32);

    public static final ModConfigSpec.BooleanValue METEOR_DEBUG_LOG = BUILDER
            .comment("Verbose, NON-personal meteor logging (dimension + coordinates only, never player",
                    "identifiers). Off by shipped default (POPIA/GDPR).")
            .define("meteorDebugLog", false);

    static final ModConfigSpec SPEC = BUILDER.build();

    /** Client oxygen-visual quality tiers. */
    public enum OxygenVisualQuality {
        OFF, MINIMAL, FULL
    }

    /** Lenient validator: a well-formed resource location (registry membership checked at use). */
    private static boolean validateResourceLocation(final Object obj) {
        return obj instanceof String s && Identifier.tryParse(s) != null;
    }
}
