package za.co.neroland.nerospace.route;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerospace.menu.MenuOpener;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.rocket.RocketLaunchPadBlock;

/**
 * The Cargo Pad: a launch-pad plate with a block entity behind it. Because it <em>is a</em>
 * {@link RocketLaunchPadBlock}, it is a cell of an ordinary pad footprint — build the usual 3×3 around it
 * and the same tier validation crewed rockets use applies, an adjacent Fuel Tank pumps into the rocket
 * standing on it, and a Launch Controller can lay it out. Placing it registers it (owner = placer) in the
 * {@link RouteRegistry}; breaking it unregisters it and drops its cargo.
 *
 * <p>Interaction: empty hand opens the pad GUI; a Name Tag renames the pad (its route label); sneak +
 * empty hand claims an unowned pad. Comparator output follows the cargo fill level.</p>
 */
public class CargoPadBlock extends RocketLaunchPadBlock implements EntityBlock {

    public static final MapCodec<CargoPadBlock> CODEC = BlockCodecs.simple(CargoPadBlock::new);

    public CargoPadBlock(Properties properties) {
        super(properties);
    }

    // No @Override: 26.3 moved codec() off Block (matches LaunchGantryBlock / StarGuideBlock).
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CargoPadBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.CARGO_PAD.get()) {
            return null;
        }
        BlockEntityTicker<CargoPadBlockEntity> ticker = (lvl, pos, st, be) -> be.tick(lvl, pos, st);
        return (BlockEntityTicker<T>) ticker;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof CargoPadBlockEntity pad) {
            Component custom = stack.get(DataComponents.CUSTOM_NAME);
            pad.registerOnPlace(serverLevel, placer instanceof Player player ? player.getUUID() : null,
                    custom == null ? null : custom.getString());
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Items.NAME_TAG)) {
            if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof CargoPadBlockEntity pad) {
                Component custom = stack.get(DataComponents.CUSTOM_NAME);
                if (custom != null && pad.rename(serverLevel, player, custom.getString())) {
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    player.sendSystemMessage(Component.translatable("block.nerospace.cargo_pad.renamed", custom.getString()));
                } else {
                    player.sendSystemMessage(Component.translatable("block.nerospace.cargo_pad.not_owner"));
                }
            }
            return InteractionResult.SUCCESS;
        }
        // Everything else (fuel containers included) falls through to the empty-hand path / the item.
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS; // let a held rocket / fuel item act
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(level.getBlockEntity(pos) instanceof CargoPadBlockEntity pad)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown()) {
            if (pad.claim(serverLevel, serverPlayer)) {
                player.sendSystemMessage(Component.translatable("block.nerospace.cargo_pad.claimed"));
            } else {
                player.sendSystemMessage(Component.translatable("block.nerospace.cargo_pad.report",
                        pad.usedSlots(), pad.activeSlots(), pad.padTier()));
            }
            return InteractionResult.SUCCESS;
        }
        return MenuOpener.openOrConsume(serverPlayer, pad);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof CargoPadBlockEntity pad ? pad.comparatorSignal() : 0;
    }
}
