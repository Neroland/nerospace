package za.co.neroland.nerospace.fluid;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * The Rocket Fuel fluid (Phase 7b): a real NeoForge {@link FluidType} with a still + flowing
 * {@link BaseFlowingFluid}, a {@link net.minecraft.world.level.block.LiquidBlock} (registered in
 * {@link ModBlocks}) and a bucket (registered in {@link ModItems}). This replaces the canister-only
 * placeholder with a genuine fluid that the rocket's tank holds and that can be moved by buckets /
 * the NeoForge fluid capability.
 *
 * <p>Must be registered to the mod bus BEFORE blocks/items, since the liquid block and bucket resolve
 * {@link #ROCKET_FUEL} at their own registration time.</p>
 */
public final class ModFluids {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Nerospace.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, Nerospace.MODID);

    public static final DeferredHolder<FluidType, FluidType> ROCKET_FUEL_TYPE = FLUID_TYPES.register(
            "rocket_fuel", () -> new FluidType(FluidType.Properties.create()
                    .density(1200)
                    .viscosity(1500)
                    .canConvertToSource(false)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> ROCKET_FUEL =
            FLUIDS.register("rocket_fuel", () -> new BaseFlowingFluid.Source(ModFluids.PROPERTIES));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> ROCKET_FUEL_FLOWING =
            FLUIDS.register("flowing_rocket_fuel", () -> new BaseFlowingFluid.Flowing(ModFluids.PROPERTIES));

    public static final BaseFlowingFluid.Properties PROPERTIES = new BaseFlowingFluid.Properties(
            ROCKET_FUEL_TYPE, ROCKET_FUEL, ROCKET_FUEL_FLOWING)
            .bucket(ModItems.ROCKET_FUEL_BUCKET)
            .block(ModBlocks.ROCKET_FUEL_BLOCK)
            .slopeFindDistance(2)
            .levelDecreasePerBlock(2);

    private ModFluids() {
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
    }
}
