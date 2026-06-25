package za.co.neroland.nerospace.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Fabric implementation of {@code rocket_fuel} as a vanilla {@link FlowingFluid} subclass (NeoForge
 * uses {@code BaseFlowingFluid} instead). The override set mirrors vanilla {@code WaterFluid}; the
 * fluid/bucket/block references resolve lazily through the common {@code ModFluids}/{@code ModItems}/
 * {@code ModBlocks} holders, so this works regardless of registration order. NeoForge's per-fluid
 * {@code getFluidType()} requirement does not exist here, which is exactly why this class is
 * Fabric-only and common never sees it.
 */
public abstract class RocketFuelFluid extends FlowingFluid {

    @Override
    public Fluid getFlowing() {
        return ModFluids.ROCKET_FUEL_FLOWING.get();
    }

    @Override
    public Fluid getSource() {
        return ModFluids.ROCKET_FUEL.get();
    }

    @Override
    public Item getBucket() {
        return ModItems.ROCKET_FUEL_BUCKET.get();
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == ModFluids.ROCKET_FUEL.get() || fluid == ModFluids.ROCKET_FUEL_FLOWING.get();
    }

    @Override
    protected boolean canConvertToSource(ServerLevel level) {
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return 2;
    }

    @Override
    protected int getDropOff(LevelReader level) {
        return 2;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return 20;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
        return direction == Direction.DOWN && !isSame(fluid);
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return NerospaceCommon.requireNonNull(ModBlocks.ROCKET_FUEL_BLOCK.get().defaultBlockState()
                .setValue(LiquidBlock.LEVEL, getLegacyLevel(state)));
    }

    public static final class Source extends RocketFuelFluid {
        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static final class Flowing extends RocketFuelFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
