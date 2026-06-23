package za.co.neroland.nerospace.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.InfiniteResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;

import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/** Creative Gas Tank: an endless source of Oxygen (and a void for any inserted gas). */
public class CreativeGasTankBlockEntity extends BlockEntity {

    private static final InfiniteResourceHandler<GasResource> HANDLER =
            new InfiniteResourceHandler<>(GasResource.OXYGEN);

    public CreativeGasTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_GAS_TANK.get(), pos, state);
    }

    public ResourceHandler<GasResource> getGasHandler() {
        return HANDLER;
    }
}
