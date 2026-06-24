package za.co.neroland.nerospace.fluid;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Trivial public subclass of {@link LiquidBlock} for the {@code rocket_fuel} world block. Vanilla's
 * {@code LiquidBlock} constructor is {@code protected}, so common (which compiles against vanilla on
 * both loaders) cannot {@code new} it directly — a subclass in this package can, giving one
 * cross-loader registration point with no per-loader access widener for the constructor.
 */
public class RocketFuelLiquidBlock extends LiquidBlock {

    public RocketFuelLiquidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }
}
