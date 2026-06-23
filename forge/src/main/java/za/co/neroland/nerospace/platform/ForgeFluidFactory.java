package za.co.neroland.nerospace.platform;

import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;

/** Forge {@link FluidFactory}: rocket fuel backed by ForgeFlowingFluid + FluidType. */
public final class ForgeFluidFactory implements FluidFactory {

    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.FLUID_TYPES, NerospaceCommon.MOD_ID);

    public static final RegistryObject<FluidType> ROCKET_FUEL_TYPE = FLUID_TYPES.register(
            "rocket_fuel", () -> new FluidType(FluidType.Properties.create()
                    .density(1200)
                    .viscosity(1500)
                    .canConvertToSource(false)));

    private static final ForgeFlowingFluid.Properties PROPERTIES = new ForgeFlowingFluid.Properties(
            ROCKET_FUEL_TYPE, ModFluids.ROCKET_FUEL, ModFluids.ROCKET_FUEL_FLOWING)
            .bucket(ModItems.ROCKET_FUEL_BUCKET)
            .block(ModBlocks.ROCKET_FUEL_BLOCK)
            .slopeFindDistance(2)
            .levelDecreasePerBlock(2);

    public static void registerFluidTypes(BusGroup modBusGroup) {
        FLUID_TYPES.register(modBusGroup);
    }

    @Override
    public Fluid createSource() {
        return new ForgeFlowingFluid.Source(PROPERTIES);
    }

    @Override
    public Fluid createFlowing() {
        return new ForgeFlowingFluid.Flowing(PROPERTIES);
    }
}
