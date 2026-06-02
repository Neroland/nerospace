package za.co.neroland.nerospace;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    // --- Nerospace: planet/station atmosphere (Phase 7) ---------------------

    public static final ModConfigSpec.BooleanValue ATMOSPHERE_DAMAGE_ENABLED = BUILDER
            .comment("Whether the thin atmosphere on Nerospace planets and the station hurts unprotected players.")
            .define("atmosphereDamageEnabled", true);

    public static final ModConfigSpec.IntValue ATMOSPHERE_DAMAGE = BUILDER
            .comment("Half-hearts of suffocation damage applied each interval (every 2s) off a safe zone.")
            .defineInRange("atmosphereDamage", 1, 0, 20);

    public static final ModConfigSpec.IntValue ATMOSPHERE_SAFE_RADIUS = BUILDER
            .comment("Blocks from a Rocket Launch Pad treated as a safe, pressurised zone.")
            .defineInRange("atmosphereSafeRadius", 6, 0, 32);

    // --- Nerospace: oxygen (Phase 8c) ---------------------------------------

    public static final ModConfigSpec.IntValue OXYGEN_MAX = BUILDER
            .comment("Maximum oxygen a player can carry (matches the vanilla air-supply scale, 300 = full).")
            .defineInRange("oxygenMax", 300, 1, 6000);

    public static final ModConfigSpec.IntValue OXYGEN_DRAIN_PER_TICK = BUILDER
            .comment("Oxygen lost per tick while exposed in an airless dimension without a breathable zone.")
            .defineInRange("oxygenDrainPerTick", 2, 1, 300);

    public static final ModConfigSpec.IntValue OXYGEN_BUBBLE_RADIUS = BUILDER
            .comment("Radius (blocks) of the breathable bubble around an active Oxygen Generator.")
            .defineInRange("oxygenBubbleRadius", 5, 0, 32);

    public static final ModConfigSpec.IntValue OXYGEN_SUIT_DRAIN = BUILDER
            .comment("Oxygen drained per ~0.5s while wearing a full Oxygen Suit off a safe zone "
                    + "(its finite air tank). Lower = the suit lasts longer; 0 = the suit never runs out.")
            .defineInRange("oxygenSuitDrain", 1, 0, 300);

    public static final ModConfigSpec.IntValue OXYGEN_SEALED_ROOM_MAX = BUILDER
            .comment("Max air blocks the sealed-room scan will flood-fill from an active Oxygen "
                    + "Generator. Larger = bigger habitable rooms but more work; 0 disables sealed rooms.")
            .defineInRange("oxygenSealedRoomMax", 600, 0, 4096);

    // --- Nerospace: oxygen FIELD (terraform design) ------------------------
    //
    // A persistent per-block diffusion-with-decay field replaces the old on-demand flood-fill.
    // One rule yields all three behaviours: open space dissipates (decay outpaces supply far from a
    // source), sealed space fills (walls remove the diffusion path to vacuum), and a hole leaks (an
    // opening is a low-concentration sink). See OXYGEN_TERRAFORM_DESIGN.md §1.

    public static final ModConfigSpec.IntValue OXYGEN_MAX_CONCENTRATION = BUILDER
            .comment("Maximum per-block oxygen concentration in the field (0..this). 15 is plenty.")
            .defineInRange("oxygenMaxConcentration", 15, 1, 15);

    public static final ModConfigSpec.IntValue OXYGEN_BREATHABLE_THRESHOLD = BUILDER
            .comment("Field concentration at/above which a cell is breathable.")
            .defineInRange("oxygenBreathableThreshold", 6, 1, 15);

    public static final ModConfigSpec.DoubleValue OXYGEN_DIFFUSION_RATE = BUILDER
            .comment("Fraction of the neighbour gradient that flows into a cell each sim step (0..0.4).")
            .defineInRange("oxygenDiffusionRate", 0.22D, 0.0D, 0.4D);

    public static final ModConfigSpec.DoubleValue OXYGEN_DECAY_PER_STEP = BUILDER
            .comment("Oxygen bled to the thin atmosphere/vacuum each sim step. The sink that keeps "
                    + "open-air bubbles finite; small values let big sealed rooms fill.")
            .defineInRange("oxygenDecayPerStep", 0.18D, 0.0D, 5.0D);

    public static final ModConfigSpec.IntValue OXYGEN_SIM_INTERVAL_TICKS = BUILDER
            .comment("Server ticks between oxygen-field relaxation passes (5 ~= 4 Hz).")
            .defineInRange("oxygenSimIntervalTicks", 5, 1, 100);

    public static final ModConfigSpec.IntValue OXYGEN_MAX_ACTIVE_CELLS_PER_SOURCE = BUILDER
            .comment("Safety cap on active (simulated) cells per source. A generator dumped into open "
                    + "vacuum stops expanding its frontier at this size instead of spiking the tick.")
            .defineInRange("oxygenMaxActiveCellsPerSource", 4096, 64, 65536);

    public static final ModConfigSpec.IntValue OXYGEN_SYNC_RADIUS = BUILDER
            .comment("Blocks around a source within which the field is simulated and synced to clients. "
                    + "Sources with no player in range pause (state persists).")
            .defineInRange("oxygenSyncRadius", 32, 8, 128);

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

    // --- Nerospace: Terraformer machine (terraform design §2) ---------------

    public static final ModConfigSpec.IntValue TERRAFORM_ENERGY_PER_BLOCK = BUILDER
            .comment("Energy spent converting one block. Energy is the real throttle: a bigger sphere "
                    + "has a bigger shell and takes longer unless fed more power or a higher tier.")
            .defineInRange("terraformEnergyPerBlock", 4, 0, 10_000);

    public static final ModConfigSpec.IntValue TERRAFORM_MAX_COLUMNS_PER_TICK = BUILDER
            .comment("Hard per-tick work cap (columns converted) regardless of tier, to protect TPS.")
            .defineInRange("terraformMaxColumnsPerTick", 48, 1, 4096);

    public static final ModConfigSpec.IntValue TERRAFORM_WORK_INTERVAL_TICKS = BUILDER
            .comment("Server ticks between Terraformer work cycles.")
            .defineInRange("terraformWorkIntervalTicks", 4, 1, 100);

    public static final ModConfigSpec.BooleanValue TERRAFORM_PLANTS_ENABLED = BUILDER
            .comment("Whether terraforming scatters grass/flowers and saplings on converted ground.")
            .define("terraformPlantsEnabled", true);

    public static final ModConfigSpec.DoubleValue TERRAFORM_PLANT_CHANCE = BUILDER
            .comment("Per-converted-column chance to place a plant/flower/sapling.")
            .defineInRange("terraformPlantChance", 0.08D, 0.0D, 1.0D);

    public static final ModConfigSpec.BooleanValue TERRAFORM_WATER_ENABLED = BUILDER
            .comment("Whether terraforming fills low/exposed cells with water.")
            .define("terraformWaterEnabled", true);

    public static final ModConfigSpec.BooleanValue TERRAFORM_RESOURCES_ENABLED = BUILDER
            .comment("Whether a Tier-3 Terraformer seeds ores into the converted subsurface (low rate).")
            .define("terraformResourcesEnabled", true);

    public static final ModConfigSpec.DoubleValue TERRAFORM_RESOURCE_CHANCE = BUILDER
            .comment("Per-converted-column chance to seed one ore (Tier 3 only). Kept low so it does "
                    + "not trivialise mining.")
            .defineInRange("terraformResourceChance", 0.015D, 0.0D, 1.0D);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> TERRAFORM_RESOURCE_ORES = BUILDER
            .comment("Ore block ids the Tier-3 Terraformer may seed. Defaults to Nerospace ores.")
            .defineListAllowEmpty("terraformResourceOres",
                    List.of("nerospace:nerosteel_ore", "nerospace:xertz_quartz_ore", "nerospace:nerosium_ore"),
                    () -> "nerospace:nerosteel_ore", Config::validateResourceLocation);

    public static final ModConfigSpec.BooleanValue TERRAFORM_FORCE_LOAD_CHUNKS = BUILDER
            .comment("ACTIVE terraforming: force-load a bounded arc around the working frontier so it "
                    + "continues while you are away. Off by default — force-loading is a TPS footgun.")
            .define("terraformForceLoadChunks", false);

    public static final ModConfigSpec.IntValue TERRAFORM_MAX_FORCED_CHUNKS = BUILDER
            .comment("Guard on how many chunks active terraforming may force-load at once.")
            .defineInRange("terraformMaxForcedChunks", 16, 0, 256);

    static final ModConfigSpec SPEC = BUILDER.build();

    /** Client oxygen-visual quality tiers. */
    public enum OxygenVisualQuality {
        OFF, MINIMAL, FULL
    }

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(Identifier.parse(itemName));
    }

    /** Lenient validator: a well-formed resource location (registry membership checked at use). */
    private static boolean validateResourceLocation(final Object obj) {
        return obj instanceof String s && Identifier.tryParse(s) != null;
    }
}
