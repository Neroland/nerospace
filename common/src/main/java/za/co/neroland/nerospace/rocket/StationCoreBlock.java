package za.co.neroland.nerospace.rocket;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.menu.MenuOpener;

/**
 * The Station Core block: placed only by the founding flow (no recipe, no loot table — breaking it
 * pops a named charter via the block entity's remove hook and unregisters the station). Right-click
 * opens the Station Console through {@link MenuOpener}; comparator reads 15 while bound.
 *
 * <p>Cross-loader port: vanilla {@code BaseEntityBlock} interactions; identical to the standalone mod.</p>
 */
public class StationCoreBlock extends BaseEntityBlock {

    public static final MapCodec<StationCoreBlock> CODEC = BlockCodecs.simple(StationCoreBlock::new);

    public StationCoreBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<StationCoreBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StationCoreBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StationCoreBlockEntity core
                && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            StationRegistry.StationEntry entry = core.isBound()
                    ? StationRegistry.get(serverPlayer.level().getServer()).get(core.stationSlot()) : null;
            if (entry == null) {
                // Unbound, or its slot is no longer registered: never a blank console.
                player.sendSystemMessage(Component.translatable("block.nerospace.station_core.unbound"));
                return InteractionResult.SUCCESS;
            }
            final int slot = entry.slot();
            final BlockPos corePos = pos.immutable();
            // Everyone may open the console; a player who fails canUse gets the read-only visitor view.
            return MenuOpener.openOrConsume(serverPlayer, new net.minecraft.world.SimpleMenuProvider(
                    (id, inv, p) -> new za.co.neroland.nerospace.menu.StationConsoleMenu(id, inv, serverPlayer, slot,
                            corePos),
                    Component.translatable("gui.nerospace.station_console.title")));
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,
            BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, za.co.neroland.nerospace.registry.ModBlockEntities.STATION_CORE.get(),
                (lvl, p, st, be) -> be.serverTick(lvl));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof StationCoreBlockEntity core
                ? core.comparatorSignal() : 0;
    }

    //? if <1.21.5 {
    /*// Before 1.21.5 there is no BlockEntity.preRemoveSideEffects hook: run it from the block when it is replaced.
    @Override
    protected void onRemove(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof StationCoreBlockEntity be) {
            be.preRemoveSideEffects(pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
    *///?}
}
