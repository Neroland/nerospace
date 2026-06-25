package za.co.neroland.nerospace.machine.quarry;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * The quarry controller block. Holds its {@link MinerTier}; backed by {@link QuarryControllerBlockEntity}.
 * Right-click opens the menu. Activation is automatic: with valid landmarks nearby and power available,
 * it builds its frame and starts mining.
 */
public class QuarryControllerBlock extends BaseEntityBlock {

    public static final @org.jspecify.annotations.NonNull MapCodec<QuarryControllerBlock> CODEC =
            simpleCodec(props -> new QuarryControllerBlock(props, MinerTier.TIER_1));

    private final MinerTier tier;

    public QuarryControllerBlock(Properties properties) {
        this(properties, MinerTier.TIER_1);
    }

    public QuarryControllerBlock(Properties properties, MinerTier tier) {
        super(properties);
        this.tier = tier;
    }

    public MinerTier tier() {
        return this.tier;
    }

    @Override
    protected MapCodec<QuarryControllerBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuarryControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.QUARRY_CONTROLLER.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof QuarryControllerBlockEntity controller) {
            serverPlayer.openMenu(controller);
        }
        return InteractionResult.SUCCESS;
    }
}
