package za.co.neroland.nerospace.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.InfiniteResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Creative Fluid Tank: an endless source of one configured fluid (and a void for inserts). Right-click
 * with a filled bucket to choose the fluid; sneak-right-click to clear.
 */
public class CreativeFluidTankBlockEntity extends BlockEntity {

    private FluidResource source = FluidResource.EMPTY;
    /** Endless source of {@link #source}; empty handler until a fluid is configured. */
    private ResourceHandler<FluidResource> handler = EmptyResourceHandler.instance();

    public CreativeFluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_FLUID_TANK.get(), pos, state);
    }

    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.handler;
    }

    public FluidResource source() {
        return this.source;
    }

    public void setSource(FluidResource source) {
        this.source = source;
        // InfiniteResourceHandler rejects an empty resource — fall back to the empty handler.
        this.handler = source.isEmpty() ? EmptyResourceHandler.instance() : new InfiniteResourceHandler<>(source);
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.source.isEmpty()) {
            output.store("Source", FluidResource.CODEC, this.source);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        setSource(input.read("Source", FluidResource.CODEC).orElse(FluidResource.EMPTY));
    }
}
