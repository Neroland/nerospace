package za.co.neroland.nerospace.platform;

import net.minecraft.world.level.material.Fluid;

/**
 * Creation side of the fluid seam: builds the still + flowing {@link Fluid} instances for
 * {@code rocket_fuel}. The loaders diverge here because NeoForge requires every {@code Fluid} to
 * carry a {@code FluidType} (so it uses {@code BaseFlowingFluid} built from a registered type),
 * whereas Fabric/vanilla has no such concept (so it uses a hand-written {@code FlowingFluid}
 * subclass). Common only ever sees plain {@link Fluid}, registered through
 * {@link za.co.neroland.nerospace.fluid.ModFluids}. Resolved via {@link Services}.
 */
public interface FluidFactory {

    FluidFactory INSTANCE = Services.load(FluidFactory.class);

    Fluid createSource();

    Fluid createFlowing();
}
