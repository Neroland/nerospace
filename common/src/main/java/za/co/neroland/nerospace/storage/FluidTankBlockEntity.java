package za.co.neroland.nerospace.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerospace.fluid.FluidTank;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/** Fluid Tank — a single-fluid buffer block entity, exposed via the mod's fluid capability/lookup. */
public class FluidTankBlockEntity extends BlockEntity {

    public static final int CAPACITY = 16_000; // mB (16 buckets)

    private final FluidTank tank = new FluidTank(CAPACITY, this::setChanged);

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_TANK.get(), pos, state);
    }

    public NerospaceFluidStorage getTank() {
        return this.tank;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Fluid", BuiltInRegistries.FLUID.getKey(this.tank.getRawFluid()).toString());
        output.putInt("Amount", this.tank.getRawAmount());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(input.getStringOr("Fluid", "minecraft:empty")));
        this.tank.setRaw(fluid, input.getIntOr("Amount", 0));
    }
}
