package za.co.neroland.nerospace.machine;

import java.util.Set;

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

import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModDimensions;

/**
 * One solar panel. Adjacent same-tier panels pool into a {@link SolarArray}: total storage and
 * generation are the sums across every member, balanced each tick so a pipe on ANY panel drains the
 * whole pool. Generation scales with the sun's height (peaks at noon, zero at night), needs a clear
 * view of the sky, is cut by rain/thunder, and is doubled in the mod's airless dimensions (permanent
 * sun). The cap is exposed on every side (extract-only) via the energy seam.
 *
 * <p>Cross-loader port: rebuilt on the multiloader {@link EnergyBuffer} (the NeoForge transfer
 * {@code SimpleEnergyHandler} isn't ported); daylight uses vanilla {@code getSkyDarken} rather than the
 * NeoForge-only dimension clock, and the airless 2× bonus keys off {@link ModDimensions} (no
 * {@code ModDimensionTypes}). Every panel is a 1×1 self-anchor here; the N×N multiblock + sun-tracking
 * renderer are a deferred enhancement.</p>
 */
public class SolarPanelBlockEntity extends BlockEntity {

    /** The mod's airless planets/station — permanent sun, no weather (the solar 2× bonus). */
    private static final Set<ResourceKey<Level>> AIRLESS = Set.of(
            ModDimensions.GREENXERTZ_LEVEL, ModDimensions.CINDARA_LEVEL,
            ModDimensions.STATION_LEVEL, ModDimensions.GLACIRA_LEVEL);

    private final SolarTier tier;
    private final EnergyBuffer energy;

    /** This cell's unit anchor. Always self for a 1×1 panel; kept for forward-compatible multiblocks. */
    private BlockPos anchorPos;

    /** Transient: the array this unit belongs to, lazily (re)built and shared with all members. */
    @Nullable
    private SolarArray array;

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
        if (!(level instanceof ServerLevel server) || !isAnchor()) {
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
        return Math.round(this.tier.fePerTick() * solarFactor(level, this.worldPosition));
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
    /** True in the mod's airless dimensions (permanent sun, no weather) — drives the solar 2× bonus. */
    public static boolean isAirless(Level level) {
        return AIRLESS.contains(level.dimension());
    }

    private static float solarFactor(ServerLevel level, BlockPos pos) {
        if (AIRLESS.contains(level.dimension())) {
            return 2.0F; // permanent sun in orbit / on an airless moon, no weather — the 2x bonus
        }
        if (!level.canSeeSky(pos.above())) {
            return 0.0F; // roofed over — no sun reaches the panel
        }
        int darken = level.getSkyDarken();
        return Mth.clamp((10 - darken) / 10.0F, 0.0F, 1.0F);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", this.energy.getRaw());
        output.putLong("Anchor", anchorPos().asLong());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.setRaw(input.getIntOr("Energy", 0));
        this.anchorPos = BlockPos.of(input.getLongOr("Anchor", this.worldPosition.asLong()));
    }
}
