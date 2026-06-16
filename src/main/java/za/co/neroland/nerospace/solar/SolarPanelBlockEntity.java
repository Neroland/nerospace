package za.co.neroland.nerospace.solar;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.Tuning;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModDimensionTypes;

/**
 * One cell of a solar panel. A Tier 1 panel is a single 1×1 cell; Tier 2/3 are N×N multiblocks made of
 * one <b>anchor</b> cell (min-corner) plus filler cells that all point back to it ({@link #anchorPos}).
 * Only the anchor ticks, generates, and holds the unit's FE buffer; filler cells forward their energy
 * capability to the anchor, so a pipe on ANY face of the multiblock pulls from the one pooled buffer.
 *
 * <p>Adjacent same-tier units pool further into a {@link SolarArray}: total storage and generation are
 * the sums across every unit's anchor. Generation scales with the sun's height (peaks at noon, zero at
 * night), needs a clear view of the sky, is cut by rain/thunder, and is doubled in the mod's
 * space/airless dimensions (permanent sun).</p>
 */
public class SolarPanelBlockEntity extends BlockEntity {

    private final SolarTier tier;
    private final SolarEnergy energy;

    /** This cell's unit anchor (the multiblock min-corner). Defaults to self — every T1 cell is its own. */
    private BlockPos anchorPos;

    /** Transient: the array this unit belongs to, lazily (re)built and shared with all member anchors. */
    @Nullable
    private SolarArray array;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_PANEL.get(), pos, state);
        this.tier = state.getBlock() instanceof SolarPanelBlock panel ? panel.tier() : SolarTier.TIER_1;
        this.energy = new SolarEnergy(this.tier.buffer());
        this.anchorPos = pos;
    }

    public SolarTier tier() {
        return this.tier;
    }

    /** The multiblock anchor (min-corner) this cell belongs to. */
    public BlockPos anchorPos() {
        return this.anchorPos;
    }

    /** True when this cell is its unit's anchor — the only cell that ticks, generates and renders. */
    public boolean isAnchor() {
        return this.anchorPos.equals(this.worldPosition);
    }

    /** Point a filler cell at its anchor (set during placement). */
    public void setAnchor(BlockPos anchor) {
        this.anchorPos = anchor;
        setChanged();
    }

    /** The anchor's BE (this one if it is the anchor), or {@code null} if the anchor is gone. */
    @Nullable
    private SolarPanelBlockEntity anchorEntity() {
        if (isAnchor()) {
            return this;
        }
        return this.level != null && this.level.getBlockEntity(this.anchorPos) instanceof SolarPanelBlockEntity a
                ? a : null;
    }

    /**
     * Exposed to {@code Capabilities.Energy.BLOCK} on every face. Filler cells forward to the anchor's
     * buffer, so the whole multiblock reads as one extract-only pool from any side.
     */
    public EnergyHandler getEnergyHandler() {
        SolarPanelBlockEntity anchor = anchorEntity();
        return anchor != null ? anchor.energy : this.energy;
    }

    SolarEnergy energy() {
        return this.energy;
    }

    /** Number of unit anchors in this panel's array (1 if not yet resolved). */
    public int arraySize() {
        SolarArray net = this.array;
        return net == null ? 1 : net.size();
    }

    /** Called by {@link SolarArray#getOrBuild} so every member anchor shares the one array instance. */
    void adopt(SolarArray net) {
        this.array = net;
    }

    /** Drop the cached array so the next tick rebuilds it (placement / break / neighbour change). */
    public void invalidateArray() {
        this.array = null;
    }

    /** Raise the buffer by {@code amount} (generation path; bypasses the zero external receive limit). */
    void generate(int amount) {
        this.energy.generate(amount);
    }

    public int comparatorSignal() {
        EnergyHandler handler = getEnergyHandler();
        int cap = handler.getCapacityAsInt();
        int stored = handler.getAmountAsInt();
        return (cap <= 0 || stored <= 0) ? 0 : 1 + (int) (stored / (double) cap * 14.0D);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel server) || !isAnchor()) {
            return; // only the anchor drives the unit; filler cells are passive forwarders
        }
        SolarArray net = this.array; // local so the null/valid check holds for the analyzer
        if (net == null || !net.isValid()) {
            net = SolarArray.getOrBuild(server, pos, this.tier);
            this.array = net;
        }
        net.tick(server);
    }

    /** FE this whole unit adds this tick = its tier's peak output x the daylight/weather/dimension factor. */
    public int generationThisTick(ServerLevel level) {
        return Math.round(this.tier.fePerTick() * solarFactor(level, this.worldPosition));
    }

    /**
     * Daylight factor in [0, 2]: the sun-height curve (0 at night), gated on sky access, cut by weather,
     * and doubled in space/airless dimensions (which have a permanent sun and no weather).
     */
    private static float solarFactor(ServerLevel level, BlockPos pos) {
        boolean space = level.dimensionTypeRegistration().is(ModDimensionTypes.SPACE);
        BlockPos above = pos.above();

        float daylight;
        if (space) {
            daylight = 1.0F; // permanent sun in orbit / on an airless moon
        } else {
            if (!level.canSeeSky(above)) {
                return 0.0F; // roofed over — no sun reaches the panel
            }
            long tod = level.getOverworldClockTime() % 24000L; // 0 sunrise, 6000 noon, 18000 midnight
            float sun = Mth.cos((float) ((tod - 6000L) / 24000.0 * 2.0 * Math.PI)); // +1 noon, -1 midnight
            daylight = Math.max(0.0F, sun);
        }

        float weather = 1.0F;
        if (!space) {
            if (level.isThundering()) {
                weather = 0.25F;
            } else if (level.isRaining() && level.isRainingAt(above)) {
                weather = 0.4F;
            }
        }

        float dimensionBonus = space ? 2.0F : 1.0F;
        return daylight * weather * dimensionBonus;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.serialize(output.child("Energy"));
        output.putLong("Anchor", this.anchorPos.asLong());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.deserialize(input.childOrEmpty("Energy"));
        this.anchorPos = BlockPos.of(input.getLongOr("Anchor", this.worldPosition.asLong()));
    }

    /** Extract-only FE buffer (external receive = 0); {@link #generate}/{@link #setStored} are internal. */
    final class SolarEnergy extends SimpleEnergyHandler {
        SolarEnergy(int capacity) {
            super(capacity, 0, Tuning.energyPipeThroughput());
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            SolarPanelBlockEntity.this.setChanged();
        }

        void generate(int amount) {
            if (amount <= 0) {
                return;
            }
            int next = Math.min(getCapacityAsInt(), getAmountAsInt() + amount);
            if (next != getAmountAsInt()) {
                set(next);
            }
        }

        void setStored(int value) {
            set(Math.max(0, Math.min(getCapacityAsInt(), value)));
        }
    }
}
