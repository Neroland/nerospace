package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;

/**
 * The pipe/hopper automation proxy for refuelling a docked rocket: a stateless {@link NerospaceFluidStorage}
 * exposed as the FLUID capability of a {@link RocketLaunchPadBlock}. Filling it routes {@code rocket_fuel}
 * into the {@link RocketEntity} standing on the pad cluster, so a pipe (or the Fuel Tank, or any fluid
 * source) adjacent to the pad fuels the rocket — closing the Refinery → pipe → pad → rocket loop without
 * needing a cross-loader <em>entity</em>-capability seam (the rocket is a moving entity; the pad block is a
 * fixed, cap-friendly forwarder). It is a sink only — a docked rocket never gives fuel back.
 *
 * <p>The proxy holds only the pad position and re-finds the rocket on every call, so it is safe to cache
 * per loader (no invalidation needed) and correct whether or not a rocket is currently docked (it just
 * reports an empty 0-capacity tank when none is).</p>
 */
public final class RocketPadFluidProxy implements NerospaceFluidStorage {

    private final Level level;
    private final BlockPos padPos;

    public RocketPadFluidProxy(Level level, BlockPos padPos) {
        this.level = level;
        this.padPos = padPos;
    }

    @Nullable
    private RocketEntity rocket() {
        return LaunchPadMultiblock.dockedRocket(this.level, this.padPos);
    }

    @Override
    public Fluid getFluid() {
        RocketEntity rocket = rocket();
        return rocket != null && rocket.getFuel() > 0 ? ModFluids.ROCKET_FUEL.get() : Fluids.EMPTY;
    }

    @Override
    public long getAmount() {
        RocketEntity rocket = rocket();
        return rocket == null ? 0 : rocket.getFuel();
    }

    @Override
    public long getCapacity() {
        RocketEntity rocket = rocket();
        return rocket == null ? 0 : rocket.getTier().fuelCapacity();
    }

    @Override
    public long fill(Fluid fluid, long amount, boolean simulate) {
        if (amount <= 0 || fluid != ModFluids.ROCKET_FUEL.get()) {
            return 0;
        }
        RocketEntity rocket = rocket();
        if (rocket == null || rocket.isLaunching()) {
            return 0;
        }
        long room = Math.max(0, (long) rocket.getTier().fuelCapacity() - rocket.getFuel());
        long accept = Math.min(amount, room);
        if (accept <= 0) {
            return 0;
        }
        if (!simulate) {
            int overflow = rocket.addFuel((int) accept);
            accept -= overflow;
        }
        return accept;
    }

    @Override
    public long drain(long amount, boolean simulate) {
        return 0; // a docked rocket is a fuel sink only
    }
}
