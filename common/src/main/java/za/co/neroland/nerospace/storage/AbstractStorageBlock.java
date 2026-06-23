package za.co.neroland.nerospace.storage;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared base for the passive storage endpoints (Battery / Fluid Tank / Gas Tank / Item Store and their
 * creative variants): plain model cubes with a block entity and no ticker — pipes and machines move
 * resources in and out through the mod's capabilities/lookups.
 */
public abstract class AbstractStorageBlock extends BaseEntityBlock {

    protected AbstractStorageBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
