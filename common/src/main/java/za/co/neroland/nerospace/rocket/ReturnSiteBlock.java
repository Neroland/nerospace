package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.registry.ModBlocks;

/**
 * Shared behavior for generated return sites. A Landing Pod starts inflated and deflates on first
 * opening; a Docking Port is the station-side variant.
 */
public abstract class ReturnSiteBlock extends BaseEntityBlock {

    public static final BooleanProperty INFLATED = BooleanProperty.create("inflated");

    private final boolean landingPod;

    protected ReturnSiteBlock(Properties properties, boolean landingPod) {
        super(properties);
        this.landingPod = landingPod;
        registerDefaultState(this.stateDefinition.any().setValue(INFLATED, landingPod));
    }

    public boolean isLandingPod() {
        return this.landingPod;
    }

    public static boolean isReturnSite(BlockState state) {
        return state.is(ModBlocks.LANDING_POD.get()) || state.is(ModBlocks.DOCKING_PORT.get());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(INFLATED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReturnSiteBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ReturnSiteBlockEntity site) {
            if (this.landingPod && state.getValue(INFLATED)) {
                level.setBlock(pos, state.setValue(INFLATED, false), 3);
            }
            player.openMenu(site);
        }
        return InteractionResult.SUCCESS;
    }
}
