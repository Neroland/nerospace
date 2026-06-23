package za.co.neroland.nerospace.fluid;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.platform.FluidFactory;
import za.co.neroland.nerospace.registry.RegistrationProvider;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;

/**
 * The {@code rocket_fuel} fluid (Phase 7b), ported cross-loader. Registers a still + flowing
 * {@link Fluid} into the vanilla fluid registry through the {@link RegistrationProvider} seam; the
 * concrete instances come from the per-loader {@link FluidFactory} (NeoForge
 * {@code BaseFlowingFluid} + {@code FluidType}; Fabric {@link RocketFuelFluid}).
 *
 * <p>Registered BEFORE blocks/items (see {@code ModRegistries}) because the liquid block and bucket
 * resolve {@link #ROCKET_FUEL} at their own registration time on the eager (Fabric) loader.
 */
public final class ModFluids {

    public static final RegistrationProvider<Fluid> FLUIDS =
            RegistrationProvider.get(Registries.FLUID, NerospaceCommon.MOD_ID);

    public static final RegistryEntry<Fluid> ROCKET_FUEL =
            FLUIDS.register("rocket_fuel", key -> FluidFactory.INSTANCE.createSource());
    public static final RegistryEntry<Fluid> ROCKET_FUEL_FLOWING =
            FLUIDS.register("flowing_rocket_fuel", key -> FluidFactory.INSTANCE.createFlowing());

    private ModFluids() {
    }

    public static void init() {
    }
}
