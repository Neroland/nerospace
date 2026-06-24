package za.co.neroland.nerospace.platform;

import net.minecraft.world.level.material.Fluid;

import za.co.neroland.nerospace.fluid.RocketFuelFluid;

/** Fabric {@link FluidFactory}: hand-written vanilla {@link RocketFuelFluid} still + flowing. */
public final class FabricFluidFactory implements FluidFactory {

    @Override
    public Fluid createSource() {
        return new RocketFuelFluid.Source();
    }

    @Override
    public Fluid createFlowing() {
        return new RocketFuelFluid.Flowing();
    }
}
