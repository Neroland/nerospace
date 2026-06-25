package za.co.neroland.nerospace.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Creative Fluid Tank — an endless source (and void sink) of one configured fluid for testing fluid
 * logistics. Defaults to {@code rocket_fuel}; right-click the block with a filled bucket to switch the
 * source fluid, sneak-empty-hand to clear, empty-hand to read it out. The chosen fluid persists in NBT.
 */
public class CreativeFluidTankBlockEntity extends BlockEntity {

    /** The endless source fluid (default rocket_fuel); {@link Fluids#EMPTY} = unset (drains nothing). */
    private @org.jspecify.annotations.NonNull Fluid source = NerospaceCommon.requireNonNull(ModFluids.ROCKET_FUEL.get());

    /** Endless source/sink of the configured {@link #source} — inserts are voided, drains are endless. */
    private final NerospaceFluidStorage infinite = new NerospaceFluidStorage() {
        @Override
        public Fluid getFluid() {
            return source;
        }

        @Override
        public long getAmount() {
            return source == Fluids.EMPTY ? 0 : Integer.MAX_VALUE;
        }

        @Override
        public long getCapacity() {
            return Integer.MAX_VALUE;
        }

        @Override
        public long fill(Fluid fluid, long amount, boolean simulate) {
            return Math.max(0, amount); // accepts (voids) anything
        }

        @Override
        public long drain(long amount, boolean simulate) {
            return source == Fluids.EMPTY ? 0 : Math.max(0, amount); // endless source of the chosen fluid
        }
    };

    public CreativeFluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_FLUID_TANK.get(), pos, state);
    }

    public NerospaceFluidStorage getTank() {
        return this.infinite;
    }

    /** The fluid this tank endlessly supplies (or {@link Fluids#EMPTY} when cleared). */
    public @org.jspecify.annotations.NonNull Fluid source() {
        return this.source;
    }

    /** Choose the endless source fluid (e.g. from a bucket); {@link Fluids#EMPTY} clears it. */
    public void setSource(@org.jetbrains.annotations.Nullable Fluid source) {
        this.source = source == null ? Fluids.EMPTY : source;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Source", BuiltInRegistries.FLUID.getKey(NerospaceCommon.requireNonNull(this.source)).toString());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.source = NerospaceCommon.requireNonNull(BuiltInRegistries.FLUID.getValue(
                Identifier.parse(input.getStringOr("Source", "minecraft:empty"))));
    }
}
