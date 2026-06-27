package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;

/**
 * The pipe automation proxy for loading a docked rocket's onboard oxygen (life-support) tank: a stateless
 * {@link NerospaceGasStorage} exposed as the GAS capability of a {@link RocketLaunchPadBlock}. Filling it
 * routes {@link GasResource#OXYGEN} into the {@link RocketEntity} standing on the pad cluster, so an Oxygen
 * Generator (or any gas source) piped to the pad charges the rocket for flight — the gas analogue of
 * {@link RocketPadFluidProxy}. It is a sink only: a docked rocket never gives oxygen back through the pad.
 *
 * <p>Like the fluid proxy it holds only the pad position and re-finds the rocket on every call, so it is
 * safe to cache per loader and reports an empty 0-capacity tank when no rocket is docked.</p>
 */
public final class RocketPadGasProxy implements NerospaceGasStorage {

    private final Level level;
    private final BlockPos padPos;

    public RocketPadGasProxy(Level level, BlockPos padPos) {
        this.level = level;
        this.padPos = padPos;
    }

    @Nullable
    private RocketEntity rocket() {
        return LaunchPadMultiblock.dockedRocket(this.level, this.padPos);
    }

    @Override
    public GasResource getGas() {
        RocketEntity rocket = rocket();
        return rocket != null && rocket.getOxygen() > 0 ? GasResource.OXYGEN : GasResource.EMPTY;
    }

    @Override
    public long getAmount() {
        RocketEntity rocket = rocket();
        return rocket == null ? 0 : rocket.getOxygen();
    }

    @Override
    public long getCapacity() {
        RocketEntity rocket = rocket();
        return rocket == null ? 0 : rocket.getTier().oxygenCapacity();
    }

    @Override
    public long fill(GasResource gas, long amount, boolean simulate) {
        if (amount <= 0 || gas != GasResource.OXYGEN) {
            return 0;
        }
        RocketEntity rocket = rocket();
        if (rocket == null || rocket.isLaunching()) {
            return 0;
        }
        long room = Math.max(0, (long) rocket.getTier().oxygenCapacity() - rocket.getOxygen());
        long accept = Math.min(amount, room);
        if (accept <= 0) {
            return 0;
        }
        if (!simulate) {
            int overflow = rocket.addOxygen((int) accept);
            accept -= overflow;
        }
        return accept;
    }

    @Override
    public long drain(long amount, boolean simulate) {
        return 0; // a docked rocket is an oxygen sink only
    }
}
