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

/**
 * The Station Core block: placed only by the founding flow (no recipe, no loot table — breaking it
 * pops a named charter via the block entity's remove hook and unregisters the station). Right-click
 * reads the station's name; comparator reads 15 while bound.
 *
 * <p>Cross-loader port: vanilla {@code BaseEntityBlock} interactions; identical to the standalone mod.</p>
 */
public class StationCoreBlock extends BaseEntityBlock {

    public static final MapCodec<StationCoreBlock> CODEC = simpleCodec(StationCoreBlock::new);

    public StationCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
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
            if (core.isBound()) {
                final int slot = core.stationSlot();
                StationRegistry.StationEntry entry =
                        StationRegistry.get(serverPlayer.level().getServer()).get(slot);
                if (!StationRegistry.canManage(entry, serverPlayer)) {
                    // Only the founder (or a server op) may rename a station.
                    serverPlayer.sendSystemMessage(
                            Component.translatable("item.nerospace.station_charter.not_owner"));
                } else {
                    serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                            (id, inv, p) -> new za.co.neroland.nerospace.menu.StationCharterMenu(
                                    id, inv, za.co.neroland.nerospace.menu.StationCharterMenu.MODE_RENAME, slot),
                            Component.translatable("gui.nerospace.station_charter.rename")));
                }
            } else {
                player.sendSystemMessage(Component.translatable("block.nerospace.station_core.unbound"));
            }
        }
        return InteractionResult.SUCCESS;
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
}
