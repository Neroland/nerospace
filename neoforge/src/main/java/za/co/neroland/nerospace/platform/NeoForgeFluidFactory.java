package za.co.neroland.nerospace.platform;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * NeoForge {@link FluidFactory}: the rocket-fuel fluid as a {@link BaseFlowingFluid} backed by a
 * registered {@link FluidType} (NeoForge's per-fluid metadata carrier). The {@code FluidType}
 * DeferredRegister is attached to the mod bus from the loader entry point via
 * {@link #registerFluidTypes(IEventBus)}. The {@link BaseFlowingFluid.Properties} reference the
 * common fluid/bucket/block holders (all lazily-resolved {@code Supplier}s), so registration order
 * across the separate registries is not a concern.
 */
public final class NeoForgeFluidFactory implements FluidFactory {

    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, NerospaceCommon.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> ROCKET_FUEL_TYPE = FLUID_TYPES.register(
            "rocket_fuel", () -> new FluidType(FluidType.Properties.create()
                    .density(1200)
                    .viscosity(1500)
                    .canConvertToSource(false)));

    private static final BaseFlowingFluid.Properties PROPERTIES = new BaseFlowingFluid.Properties(
            ROCKET_FUEL_TYPE, ModFluids.ROCKET_FUEL, ModFluids.ROCKET_FUEL_FLOWING)
            .bucket(ModItems.ROCKET_FUEL_BUCKET)
            .block(ModBlocks.ROCKET_FUEL_BLOCK)
            .slopeFindDistance(2)
            .levelDecreasePerBlock(2);

    /** Attach the FluidType DeferredRegister to the mod event bus (call from the entry point). */
    public static void registerFluidTypes(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
    }

    @Override
    public Fluid createSource() {
        return new BaseFlowingFluid.Source(PROPERTIES);
    }

    @Override
    public Fluid createFlowing() {
        return new BaseFlowingFluid.Flowing(PROPERTIES);
    }
}
