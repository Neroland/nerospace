package za.co.neroland.nerospace;

/**
 * Balance tuning: the mod-owned BASE values plus the small set of config-exposed multipliers
 * (see {@link Config}). Pre-1.0 config refactor: instead of ~25 absolute config keys, modpacks
 * tune five multipliers ({@code oxygenDrainMultiplier}, {@code oxygenCapacityMultiplier},
 * {@code energyRateMultiplier}, {@code fuelCostMultiplier}, {@code machineSpeedMultiplier});
 * the base numbers live here in code.
 *
 * <p>All scaled values are clamped to a minimum of 1 so an extreme low multiplier (0.1x) can never
 * zero a rate/cost (no divide-by-zero, no stalled progression, no infinite air). Inverse-scaled
 * intervals (work periods) are likewise clamped to >= 1 tick so a 10x speed can't produce a
 * zero-tick interval. Multipliers themselves are range-clamped to 0.1..10 by the config spec, so
 * the largest scaled value (battery 200k FE x10 = 2M) is nowhere near integer overflow.</p>
 */
public final class Tuning {

    private Tuning() {
    }

    // ------------------------------------------------------------------
    // Base values: oxygen / atmosphere
    // ------------------------------------------------------------------

    /** Player air capacity (vanilla air-supply scale; 300 = full bar). */
    public static final int BASE_OXYGEN_MAX = 300;
    /** Air capacity of a full Tier 2 (cindrite-upgraded) Oxygen Suit. */
    public static final int BASE_OXYGEN_SUIT_T2_MAX = 600;
    /** Oxygen lost per tick while exposed without a suit. */
    public static final int BASE_OXYGEN_DRAIN_PER_TICK = 2;
    /** Oxygen drained per suit check (~0.5s) while wearing a full suit off a safe zone. */
    public static final int BASE_OXYGEN_SUIT_DRAIN = 1;
    /** Half-hearts of suffocation damage per interval (every 2s) at zero oxygen. */
    public static final int BASE_ATMOSPHERE_DAMAGE = 1;
    /** Air units restored per suit check (~0.5s) while airlock-refilling (T2 suits double it). */
    public static final int BASE_OXYGEN_AIRLOCK_REFILL_PER_CHECK = 20;
    /** Millibuckets of Oxygen gas drawn per air unit restored by an airlock refill. */
    public static final int BASE_OXYGEN_AIRLOCK_MB_PER_AIR = 5;
    /**
     * Oxygen-drain factor on a hazard dimension without the matching suit variant
     * (SUIT_HAZARD_DESIGN.md §2). Internalised (no config key): it multiplies the already
     * {@code oxygenDrainMultiplier}-scaled drains, and {@code atmosphereDamageEnabled=false}
     * still switches the whole system off.
     */
    public static final int BASE_HAZARD_DRAIN_MULTIPLIER = 4;

    // ------------------------------------------------------------------
    // Base values: power grid / storage
    // ------------------------------------------------------------------

    public static final int BASE_COMBUSTION_GENERATOR_FE_PER_TICK = 60;
    public static final int BASE_PASSIVE_GENERATOR_FE_PER_TICK = 10;
    public static final int BASE_ENERGY_PIPE_THROUGHPUT = 4_000;
    public static final int BASE_ENERGY_PIPE_CAPACITY = 8_000;
    public static final int BASE_FLUID_PIPE_CAPACITY = 4_000;
    public static final int BASE_FLUID_PIPE_THROUGHPUT = 500;
    public static final int BASE_GAS_PIPE_CAPACITY = 4_000;
    public static final int BASE_GAS_PIPE_THROUGHPUT = 250;
    public static final int BASE_BATTERY_CAPACITY = 200_000;
    public static final int BASE_FLUID_TANK_CAPACITY = 16_000;
    public static final int BASE_GAS_TANK_CAPACITY = 16_000;
    public static final int BASE_FUEL_TANK_CAPACITY = 32_000;
    public static final int BASE_COMBUSTION_GENERATOR_BUFFER = 50_000;
    public static final int BASE_PASSIVE_GENERATOR_BUFFER = 20_000;
    public static final int BASE_GRINDER_BUFFER = 10_000;
    public static final int BASE_OXYGEN_GENERATOR_BUFFER = 10_000;
    public static final int BASE_OXYGEN_GENERATOR_O2_CAPACITY = 8_000;
    public static final int BASE_TERRAFORMER_BUFFER = 100_000;

    // ------------------------------------------------------------------
    // Base values: machine speeds / item logistics
    // ------------------------------------------------------------------

    /** Grinder ticks per crafted item (progress target; machineSpeed shortens it). */
    public static final int BASE_GRINDER_MAX_PROGRESS = 100;
    /** Oxygen produced per tick at full power (mB). */
    public static final int BASE_OXYGEN_GENERATOR_MAKE_MB_PER_TICK = 5;
    /** Oxygen consumed per tick to keep the breathable field alive (mB). */
    public static final int BASE_OXYGEN_GENERATOR_EMIT_MB_PER_TICK = 2;
    /** Fuel pumped per tick into a rocket on a partial pad cluster (mB). */
    public static final int BASE_FUEL_TANK_PUMP_RATE = 40;
    /** Faster feed once the canonical full 3x3 pad is formed (mB/tick). */
    public static final int BASE_FUEL_TANK_PUMP_RATE_FULL_PAD = 160;
    /** Heavy Launch Complex feed (5x5 + gantry; LAUNCH_PAD_DESIGN.md sign-off: 12x base). */
    public static final int BASE_FUEL_TANK_PUMP_RATE_HEAVY_PAD = 480;
    /** Server ticks between Terraformer work cycles (lower = faster expansion). */
    public static final int BASE_TERRAFORM_WORK_INTERVAL_TICKS = 8;
    /** Ticks an item takes to cross one pipe segment (10 = 2 blocks/second). */
    public static final int BASE_ITEM_PIPE_TICKS_PER_BLOCK = 10;
    /** Ticks between extraction pulses on pulling pipe faces. */
    public static final int BASE_ITEM_PIPE_EXTRACT_PERIOD = 10;
    /** Max items pulled from an inventory per extraction (deliberately NOT multiplier-scaled). */
    public static final int ITEM_PIPE_EXTRACT_AMOUNT = 8;

    // ------------------------------------------------------------------
    // Base values: costs
    // ------------------------------------------------------------------

    /** Energy spent converting one terraformed block (FE). */
    public static final int BASE_TERRAFORM_ENERGY_PER_BLOCK = 12;
    /** Grinder energy consumed per progress tick (FE). */
    public static final int BASE_GRINDER_ENERGY_PER_TICK = 30;
    /** Oxygen Generator energy cost per mB of oxygen produced (FE). */
    public static final int BASE_OXYGEN_GENERATOR_FE_PER_MB = 2;

    // ------------------------------------------------------------------
    // Internalised tuning (was config; cosmetic/balance constants, booleans still gate them)
    // ------------------------------------------------------------------

    /** Per-converted-column chance to place a plant/flower/sapling while terraforming. */
    public static final double TERRAFORM_PLANT_CHANCE = 0.08D;
    /** Per-converted-column chance for a Tier-3 Terraformer to seed one ore. */
    public static final double TERRAFORM_RESOURCE_CHANCE = 0.015D;

    // ------------------------------------------------------------------
    // Scaling helpers
    // ------------------------------------------------------------------

    /** Scales a base rate/cost/capacity, clamped so a 0.1x multiplier never zeroes it. */
    private static int scale(int base, double multiplier) {
        return Math.max(1, (int) Math.round(base * multiplier));
    }

    /** Inverse-scales an interval (higher multiplier = shorter period), clamped to >= 1 tick. */
    private static int scaleInverse(int base, double multiplier) {
        return Math.max(1, (int) Math.round(base / multiplier));
    }

    // ------------------------------------------------------------------
    // Oxygen (capacity x oxygenCapacityMultiplier, drains x oxygenDrainMultiplier)
    // ------------------------------------------------------------------

    public static int oxygenMax() {
        return scale(BASE_OXYGEN_MAX, Config.OXYGEN_CAPACITY_MULTIPLIER.get());
    }

    public static int oxygenSuitT2Max() {
        return scale(BASE_OXYGEN_SUIT_T2_MAX, Config.OXYGEN_CAPACITY_MULTIPLIER.get());
    }

    public static int oxygenDrainPerTick() {
        return scale(BASE_OXYGEN_DRAIN_PER_TICK, Config.OXYGEN_DRAIN_MULTIPLIER.get());
    }

    public static int oxygenSuitDrain() {
        return scale(BASE_OXYGEN_SUIT_DRAIN, Config.OXYGEN_DRAIN_MULTIPLIER.get());
    }

    /** Suffocation damage; {@code atmosphereDamageEnabled=false} is the off switch, so min 1. */
    public static float atmosphereDamage() {
        return scale(BASE_ATMOSPHERE_DAMAGE, Config.OXYGEN_DRAIN_MULTIPLIER.get());
    }

    /** Airlock refill rate (machine-driven, so machineSpeedMultiplier). */
    public static int oxygenAirlockRefillPerCheck() {
        return scale(BASE_OXYGEN_AIRLOCK_REFILL_PER_CHECK, Config.MACHINE_SPEED_MULTIPLIER.get());
    }

    /** Gas cost of airlock refilling (a consumable cost, so fuelCostMultiplier). */
    public static int oxygenAirlockMbPerAir() {
        return scale(BASE_OXYGEN_AIRLOCK_MB_PER_AIR, Config.FUEL_COST_MULTIPLIER.get());
    }

    // ------------------------------------------------------------------
    // Energy & storage (x energyRateMultiplier)
    // ------------------------------------------------------------------

    public static int combustionGeneratorFePerTick() {
        return scale(BASE_COMBUSTION_GENERATOR_FE_PER_TICK, Config.ENERGY_RATE_MULTIPLIER.get());
    }

    public static int passiveGeneratorFePerTick() {
        return scale(BASE_PASSIVE_GENERATOR_FE_PER_TICK, Config.ENERGY_RATE_MULTIPLIER.get());
    }

    public static int energyPipeThroughput() {
        return scale(BASE_ENERGY_PIPE_THROUGHPUT, Config.ENERGY_RATE_MULTIPLIER.get());
    }

    public static int energyPipeCapacity() {
        return scale(BASE_ENERGY_PIPE_CAPACITY, Config.ENERGY_RATE_MULTIPLIER.get());
    }

    public static int fluidPipeCapacity() {
        return scale(BASE_FLUID_PIPE_CAPACITY, Config.ENERGY_RATE_MULTIPLIER.get());
    }

    public static int gasPipeCapacity() {
        return scale(BASE_GAS_PIPE_CAPACITY, Config.ENERGY_RATE_MULTIPLIER.get());
    }

    public static int batteryCapacity() {
        return scale(BASE_BATTERY_CAPACITY, Config.ENERGY_RATE_MULTIPLIER.get());
    }

    public static int fluidTankCapacity() {
        return scale(BASE_FLUID_TANK_CAPACITY, Config.ENERGY_RATE_MULTIPLIER.get());
    }

    public static int gasTankCapacity() {
        return scale(BASE_GAS_TANK_CAPACITY, Config.ENERGY_RATE_MULTIPLIER.get());
    }

    public static int fuelTankCapacity() {
        return scale(BASE_FUEL_TANK_CAPACITY, Config.ENERGY_RATE_MULTIPLIER.get());
    }

    public static int combustionGeneratorBuffer() {
        return scale(BASE_COMBUSTION_GENERATOR_BUFFER, Config.ENERGY_RATE_MULTIPLIER.get());
    }

    public static int passiveGeneratorBuffer() {
        return scale(BASE_PASSIVE_GENERATOR_BUFFER, Config.ENERGY_RATE_MULTIPLIER.get());
    }

    public static int grinderBuffer() {
        return scale(BASE_GRINDER_BUFFER, Config.ENERGY_RATE_MULTIPLIER.get());
    }

    public static int oxygenGeneratorBuffer() {
        return scale(BASE_OXYGEN_GENERATOR_BUFFER, Config.ENERGY_RATE_MULTIPLIER.get());
    }

    public static int oxygenGeneratorO2Capacity() {
        return scale(BASE_OXYGEN_GENERATOR_O2_CAPACITY, Config.ENERGY_RATE_MULTIPLIER.get());
    }

    public static int terraformerBuffer() {
        return scale(BASE_TERRAFORMER_BUFFER, Config.ENERGY_RATE_MULTIPLIER.get());
    }

    /** Rocket tank size for a tier's base capacity (storage, so energyRateMultiplier). */
    public static int rocketFuelCapacity(int baseCapacity) {
        return scale(baseCapacity, Config.ENERGY_RATE_MULTIPLIER.get());
    }

    // ------------------------------------------------------------------
    // Costs (x fuelCostMultiplier)
    // ------------------------------------------------------------------

    /**
     * Fuel burned per launch for a tier. Clamped to the (scaled) tank size so a high
     * fuelCostMultiplier with a low energyRateMultiplier can never make a launch impossible.
     */
    public static int rocketFuelPerLaunch(int baseCost, int baseCapacity) {
        return Math.min(scale(baseCost, Config.FUEL_COST_MULTIPLIER.get()),
                rocketFuelCapacity(baseCapacity));
    }

    public static int terraformEnergyPerBlock() {
        return scale(BASE_TERRAFORM_ENERGY_PER_BLOCK, Config.FUEL_COST_MULTIPLIER.get());
    }

    public static int grinderEnergyPerTick() {
        return scale(BASE_GRINDER_ENERGY_PER_TICK, Config.FUEL_COST_MULTIPLIER.get());
    }

    public static int oxygenGeneratorFePerMb() {
        return scale(BASE_OXYGEN_GENERATOR_FE_PER_MB, Config.FUEL_COST_MULTIPLIER.get());
    }

    // ------------------------------------------------------------------
    // Machine speed (x machineSpeedMultiplier; intervals inverse-scaled)
    // ------------------------------------------------------------------

    /** Grinder ticks per item (inverse: 10x speed = 10 ticks, 0.1x = 1000 ticks). */
    public static int grinderMaxProgress() {
        return scaleInverse(BASE_GRINDER_MAX_PROGRESS, Config.MACHINE_SPEED_MULTIPLIER.get());
    }

    public static int oxygenGeneratorMakeMbPerTick() {
        return scale(BASE_OXYGEN_GENERATOR_MAKE_MB_PER_TICK, Config.MACHINE_SPEED_MULTIPLIER.get());
    }

    public static int oxygenGeneratorEmitMbPerTick() {
        return scale(BASE_OXYGEN_GENERATOR_EMIT_MB_PER_TICK, Config.MACHINE_SPEED_MULTIPLIER.get());
    }

    public static int fuelTankPumpRate() {
        return scale(BASE_FUEL_TANK_PUMP_RATE, Config.MACHINE_SPEED_MULTIPLIER.get());
    }

    public static int fuelTankPumpRateFullPad() {
        return scale(BASE_FUEL_TANK_PUMP_RATE_FULL_PAD, Config.MACHINE_SPEED_MULTIPLIER.get());
    }

    public static int fuelTankPumpRateHeavyPad() {
        return scale(BASE_FUEL_TANK_PUMP_RATE_HEAVY_PAD, Config.MACHINE_SPEED_MULTIPLIER.get());
    }

    public static int fluidPipeThroughput() {
        return scale(BASE_FLUID_PIPE_THROUGHPUT, Config.MACHINE_SPEED_MULTIPLIER.get());
    }

    public static int gasPipeThroughput() {
        return scale(BASE_GAS_PIPE_THROUGHPUT, Config.MACHINE_SPEED_MULTIPLIER.get());
    }

    public static int terraformWorkIntervalTicks() {
        return scaleInverse(BASE_TERRAFORM_WORK_INTERVAL_TICKS, Config.MACHINE_SPEED_MULTIPLIER.get());
    }

    public static int itemPipeTicksPerBlock() {
        return scaleInverse(BASE_ITEM_PIPE_TICKS_PER_BLOCK, Config.MACHINE_SPEED_MULTIPLIER.get());
    }

    public static int itemPipeExtractPeriod() {
        return scaleInverse(BASE_ITEM_PIPE_EXTRACT_PERIOD, Config.MACHINE_SPEED_MULTIPLIER.get());
    }
}
