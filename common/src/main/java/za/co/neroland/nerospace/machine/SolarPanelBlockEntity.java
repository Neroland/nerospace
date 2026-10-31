package za.co.neroland.nerospace.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SideConfigured;
import za.co.neroland.nerolandcore.sideconfig.SideMode;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;

import za.co.neroland.nerospace.block.rules.SolarReflectRules;
import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.world.planet.PlanetClock;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.PlanetWeather;
import za.co.neroland.nerospace.world.planet.Planets;
import za.co.neroland.nerospace.world.planet.SolarMath;

/**
 * One solar panel. Adjacent same-tier panels pool into a {@link SolarArray}: total storage and
 * generation are the sums across every member, balanced each tick so a pipe on ANY panel drains the
 * whole pool. Generation scales with the sun's height (peaks at noon, zero at night), needs a clear
 * view of the sky, is cut by rain/thunder, and is doubled in the mod's airless dimensions (permanent
 * sun). The cap is exposed on every side (extract-only) via the energy seam.
 *
 * <p>Cross-loader port: rebuilt on the multiloader {@link EnergyBuffer} (the NeoForge transfer
 * {@code SimpleEnergyHandler} isn't ported); daylight uses vanilla {@code getSkyDarken} rather than the
 * NeoForge-only dimension clock. On a Nerospace planet (2.0) the panel follows the planet clock instead — see
 * {@link #solarFactor}. Every panel is a 1×1 self-anchor here; the N×N multiblock + sun-tracking
 * renderer are a deferred enhancement.</p>
 */
public class SolarPanelBlockEntity extends BlockEntity implements SideConfigured {

    private final SolarTier tier;
    private final EnergyBuffer energy;

    /** This cell's unit anchor. Always self for a 1×1 panel; kept for forward-compatible multiblocks. */
    private BlockPos anchorPos;

    /** Transient: the array this unit belongs to, lazily (re)built and shared with all members. */
    @Nullable
    private SolarArray array;

    /**
     * Universal side configuration (Neroland Core): ENERGY only, GENERATOR preset — the panel's pooled
     * buffer is exposed as OUTPUT on every face (the pipe pulls power out) and never accepts power in.
     * Energy is already a {@code NeroEnergyStorage}, so the gated view registers straight onto Core's
     * energy capability; a face set to DISABLED then exposes nothing. Composed, not inherited.
     */
    private final SideConfigComponent sideConfig =
            new SideConfigComponent(buildSideConfig(), this).withEnergy(this::getEnergy);

    private static SideConfig buildSideConfig() {
        return SideConfig.builder()
                .channel(Channel.ENERGY)
                .allow(Channel.ENERGY, SideMode.INPUT, false)
                .allow(Channel.ENERGY, SideMode.IO, false)
                .autoEject(Channel.ENERGY, true) // push power into adjacent receivers by default (no cable needed)
                .defaultPreset(SidePreset.GENERATOR)
                .build();
    }

    @Override
    public SideConfigComponent sideConfig() {
        return this.sideConfig;
    }

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_PANEL.get(), pos, state);
        this.tier = state.getBlock() instanceof SolarPanelBlock panel ? panel.tier() : SolarTier.TIER_1;
        this.energy = new EnergyBuffer(this.tier.buffer(), 0, this.tier.maxExtract(), this::setChanged);
        this.anchorPos = pos;
    }

    public SolarTier tier() {
        return this.tier;
    }

    public BlockPos anchorPos() {
        return this.anchorPos == null ? this.worldPosition : this.anchorPos;
    }

    public boolean isAnchor() {
        return anchorPos().equals(this.worldPosition);
    }

    /** Point a filler cell at its anchor (set during multiblock placement). */
    public void setAnchor(BlockPos anchor) {
        this.anchorPos = anchor;
        setChanged();
    }

    /** The anchor's BE (this one if it is the anchor), or {@code null} if the anchor is gone. */
    @Nullable
    private SolarPanelBlockEntity anchorEntity() {
        BlockPos anchor = anchorPos();
        if (anchor.equals(this.worldPosition)) {
            return this;
        }
        return this.level != null && this.level.getBlockEntity(anchor) instanceof SolarPanelBlockEntity a
                ? a : null;
    }

    /**
     * Exposed to the energy capability on every face. Filler cells forward to the anchor's buffer, so the
     * whole multiblock reads as one extract-only pool from any side.
     */
    public NerospaceEnergyStorage getEnergy() {
        SolarPanelBlockEntity anchor = anchorEntity();
        return anchor != null ? anchor.energy : this.energy;
    }

    /** The raw buffer — used by {@link SolarArray} to balance the pool. */
    EnergyBuffer energy() {
        return this.energy;
    }

    /** Number of pooled units in this panel's array (1 until resolved). */
    public int arraySize() {
        SolarArray net = this.array;
        return net == null ? 1 : net.size();
    }

    /** Called by {@link SolarArray#getOrBuild} so every member shares the one array instance. */
    void adopt(SolarArray net) {
        this.array = net;
    }

    /** Drop the cached array so the next tick rebuilds it (placement / break / neighbour change). */
    public void invalidateArray() {
        this.array = null;
    }

    /** Raise the buffer (generation path; bypasses the zero external-receive limit). */
    void generate(int amount) {
        this.energy.generate(amount);
    }

    public int comparatorSignal() {
        int cap = (int) this.energy.getCapacity();
        int stored = (int) this.energy.getAmount();
        return (cap <= 0 || stored <= 0) ? 0 : 1 + (int) (stored / (double) cap * 14.0D);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        // Optional auto-eject (default off per face — a safe no-op until a player enables it). Runs per
        // panel (each cell governs its own faces) regardless of whether this cell is the array anchor.
        this.sideConfig.serverTick(level, pos, MachineSideConfig.TRANSFER_RATE);
        if (!isAnchor()) {
            return;
        }
        SolarArray net = this.array; // local so the null/valid check holds for the analyzer
        if (net == null || !net.isValid()) {
            net = SolarArray.getOrBuild(server, pos, this.tier);
            this.array = net;
        }
        net.tick(server);
    }

    /** FE this unit adds this tick = its tier's peak output × the daylight/weather/dimension factor. */
    public int generationThisTick(ServerLevel level) {
        return Math.round(this.tier.fePerTick() * solarFactor(level, this.worldPosition) * reflectFactor(level));
    }

    /** Cached Anorthosite reflector multiplier; derived from the world, never saved. */
    private float reflect = 1.0F;
    private long nextReflectCheck = Long.MIN_VALUE;

    /**
     * The Anorthosite reflector multiplier for this panel: x1.25 when all nine blocks of the 3x3 directly under it
     * are Anorthosite, else x1. Re-counted every {@link SolarReflectRules#CHECK_INTERVAL_TICKS} ticks and cached, so
     * the array tick reads a field; it reads nine block states and nothing else (no saved-data lookups). It is the
     * only multiplier a ground panel takes on top of the solar factor (a Solar Wing never gets it).
     */
    float reflectFactor(ServerLevel level) {
        long now = level.getGameTime();
        if (now >= this.nextReflectCheck) {
            this.nextReflectCheck = now + SolarReflectRules.CHECK_INTERVAL_TICKS;
            BlockPos below = this.worldPosition.below();
            int count = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (level.getBlockState(below.offset(dx, 0, dz))
                            .is(za.co.neroland.nerospace.world.planet.luna.LunaContent.ANORTHOSITE.get())) {
                        count++;
                    }
                }
            }
            this.reflect = (float) SolarReflectRules.reflectMultiplier(count);
        }
        return this.reflect;
    }

    /**
     * FE this unit adds this tick given a planet factor already computed once for the whole array this tick
     * ({@link #planetFactor}); off-planet ({@code planetFactor < 0}) it falls back to the per-panel sky check.
     */
    public int generationThisTick(ServerLevel level, float planetFactor) {
        float factor = planetFactor >= 0.0F ? planetFactor : solarFactor(level, this.worldPosition);
        return Math.round(this.tier.fePerTick() * factor * reflectFactor(level));
    }

    /**
     * The planet solar factor for {@code level} this tick (the same for every panel on a planet: no roof check
     * there), or {@code -1} off-planet where each panel needs its own sky view. Call once per array tick.
     */
    public static float planetFactor(ServerLevel level) {
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null || !planet.traits().airless()) {
            return -1.0F;
        }
        return (float) SolarMath.factor(planet, PlanetClock.phase(level), PlanetWeather.solarFactor(level),
                NerospaceConfig.solarClockDriven());
    }

    /**
     * Daylight factor in [0, 2]: a daylight curve (0 at night / when roofed over), and doubled in the
     * airless dimensions (permanent sun, no weather).
     *
     * <p>Cross-loader note: derived from vanilla {@code getSkyDarken()} (the NeoForge dimension clock
     * used by the standalone mod isn't on the de-obf classpath). {@code getSkyDarken()} is 0 at full
     * daylight and ramps toward ~11 at night — and rises during rain/thunder, so weather is already
     * folded in (no separate multiplier needed).</p>
     */
    /**
     * True on a Nerospace planet (every one is airless): no roof or vanilla-weather check applies there, as in
     * 1.x. Client-safe (the renderer uses it to pick its pose source).
     */
    public static boolean isAirless(Level level) {
        return isPlanet(level.dimension());
    }

    private static boolean isPlanet(ResourceKey<Level> dimension) {
        PlanetDefinition planet = Planets.get(dimension);
        return planet != null && planet.traits().airless();
    }

    private static float solarFactor(ServerLevel level, BlockPos pos) {
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet != null && planet.traits().airless()) {
            // 2.0: follows the planet clock and weather, averaging at least the 1.x flat 2x over a day (the
            // station stays a constant 2x). solar.clockDriven=false restores the flat 2x. No roof check, as in 1.x.
            return (float) SolarMath.factor(planet, PlanetClock.phase(level), PlanetWeather.solarFactor(level),
                    NerospaceConfig.solarClockDriven());
        }
        if (!level.canSeeSky(pos.above())) {
            return 0.0F; // roofed over — no sun reaches the panel
        }
        int darken = level.getSkyDarken();
        return Mth.clamp((10 - darken) / 10.0F, 0.0F, 1.0F);
    }

    //? if >=1.21.6 {
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
    //?} else {
    /*@Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ValueOutput output = ValueOutput.of(tag, registries);
    *///?}
        output.putInt("Energy", this.energy.getRaw());
        output.putLong("Anchor", anchorPos().asLong());
        this.sideConfig.save(output);
    }

    //? if >=1.21.6 {
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
    //?} else {
    /*@Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ValueInput input = ValueInput.of(tag, registries);
    *///?}
        this.energy.setRaw(input.getIntOr("Energy", 0));
        this.anchorPos = BlockPos.of(input.getLongOr("Anchor", this.worldPosition.asLong()));
        this.sideConfig.load(input);
    }
}
