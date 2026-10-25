package za.co.neroland.nerospace.world.station.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SideConfigured;
import za.co.neroland.nerolandcore.sideconfig.SideMode;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerospace.block.rules.SolarWingRules;
import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.machine.MachineSideConfig;
import za.co.neroland.nerospace.machine.SolarTier;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.rocket.StationRegistry;
import za.co.neroland.nerospace.world.station.StationContent;

/**
 * One Solar Wing: a small extract-only buffer that fills at {@link SolarWingRules#output} and auto-ejects into
 * adjacent receivers (the same Core side-config GENERATOR preset a solar panel uses). Exposure, the station class
 * multiplier and the module power switch are re-read every {@link #RECHECK_TICKS} ticks and cached.
 */
public class SolarWingBlockEntity extends BlockEntity implements SideConfigured {

    /** Cache refresh interval for exposure / class / module switch. */
    public static final int RECHECK_TICKS = 100;

    private final EnergyBuffer energy;
    private final SideConfigComponent sideConfig;
    private int output;
    private long nextCheck = Long.MIN_VALUE;

    public SolarWingBlockEntity(BlockPos pos, BlockState state) {
        super(StationContent.SOLAR_WING_BE.get(), pos, state);
        int peak = SolarTier.TIER_2.fePerTick();
        this.energy = new EnergyBuffer(peak * 60, 0, peak * 6, this::setChanged);
        this.sideConfig = new SideConfigComponent(SideConfig.builder()
                .channel(Channel.ENERGY)
                .allow(Channel.ENERGY, SideMode.INPUT, false)
                .allow(Channel.ENERGY, SideMode.IO, false)
                .autoEject(Channel.ENERGY, true)
                .defaultPreset(SidePreset.GENERATOR)
                .build(), this).withEnergy(this::getEnergy);
    }

    @Override
    public SideConfigComponent sideConfig() {
        return this.sideConfig;
    }

    public NerospaceEnergyStorage getEnergy() {
        return this.energy;
    }

    /** FE/t this wing currently produces (cached). */
    public int output() {
        return this.output;
    }

    public void tick(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        long now = server.getGameTime();
        if (now >= this.nextCheck) {
            this.nextCheck = now + RECHECK_TICKS;
            this.output = computeOutput(server, pos);
        }
        if (this.output > 0) {
            this.energy.generate(this.output);
        }
        this.sideConfig.serverTick(level, pos, MachineSideConfig.TRANSFER_RATE);
    }

    /** The wing's output now: station dimension only, exposed to the void, class multiplier, module switch. */
    public static int computeOutput(ServerLevel level, BlockPos pos) {
        if (!ModDimensions.STATION_LEVEL.equals(level.dimension())) {
            return 0;
        }
        boolean exposed = false;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(d)).isAir()) {
                exposed = true;
                break;
            }
        }
        StationRegistry registry = StationRegistry.get(level.getServer());
        StationRegistry.StationEntry entry = registry.entryAt(level.dimension(), pos);
        double multiplier = entry == null ? 1.0 : entry.stationClass().traits().solarWingMultiplier();
        boolean enabled = registry.moduleEnabledAt(level.dimension(), pos);
        return SolarWingRules.output(SolarTier.TIER_2.fePerTick(), exposed, multiplier, enabled);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", this.energy.getRaw());
        this.sideConfig.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.setRaw(input.getIntOr("Energy", 0));
        this.sideConfig.load(input);
    }
}
