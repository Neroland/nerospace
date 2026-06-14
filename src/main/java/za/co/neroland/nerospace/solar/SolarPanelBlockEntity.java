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
 * One physical solar panel. It carries its own FE buffer and, every tick, contributes daylight-scaled
 * energy to its {@link SolarArray} — the connected run of same-tier panels that behaves as a single
 * pooled machine. The buffer is extract-only on every face (it is a generator, not a sink), which makes
 * each side an output port a pipe or machine can pull from.
 *
 * <p>Generation scales with the sun's height (peaks at noon, zero at night), requires a clear view of
 * the sky, is cut by rain/thunder, and is doubled in the mod's space/airless dimensions (which have a
 * permanent sun). The night-time zero falls straight out of the daylight curve, matching the renderer
 * folding the panel flat.</p>
 */
public class SolarPanelBlockEntity extends BlockEntity {

    private final SolarTier tier;
    private final SolarEnergy energy;

    /** Transient: the array this panel belongs to, lazily (re)built and shared with all members. */
    @Nullable
    private SolarArray array;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_PANEL.get(), pos, state);
        this.tier = state.getBlock() instanceof SolarPanelBlock panel ? panel.tier() : SolarTier.TIER_1;
        this.energy = new SolarEnergy(this.tier.buffer());
    }

    public SolarTier tier() {
        return this.tier;
    }

    /** Exposed to {@code Capabilities.Energy.BLOCK} (every side) so pipes/machines pull the pooled power. */
    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    SolarEnergy energy() {
        return this.energy;
    }

    /** Number of panels in this panel's array (1 if not yet resolved). */
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

    /** Raise the buffer by {@code amount} (generation path; bypasses the zero external receive limit). */
    void generate(int amount) {
        this.energy.generate(amount);
    }

    public int comparatorSignal() {
        int cap = this.energy.getCapacityAsInt();
        int stored = this.energy.getAmountAsInt();
        return (cap <= 0 || stored <= 0) ? 0 : 1 + (int) (stored / (double) cap * 14.0D);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        SolarArray net = this.array; // local so the null/valid check holds for the analyzer
        if (net == null || !net.isValid()) {
            net = SolarArray.getOrBuild(server, pos, this.tier);
            this.array = net;
        }
        net.tick(server);
    }

    /** FE this panel adds this tick = peak output x the daylight/weather/dimension factor. */
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
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.deserialize(input.childOrEmpty("Energy"));
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
