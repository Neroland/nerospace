package za.co.neroland.nerospace.village;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModBlocks;

/**
 * Village Core (ALIEN_VILLAGERS_DESIGN.md §4.1). Right-click to claim; feed it Nerosteel to stock its
 * construction store; right-click (as owner) to teach + raise the next building once the nearby
 * villagers trust you enough. It ticks construction via {@link VillageCoreBlockEntity#serverTick}.
 */
public class VillageCoreBlock extends BaseEntityBlock {

    public static final MapCodec<VillageCoreBlock> CODEC = simpleCodec(VillageCoreBlock::new);

    public VillageCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<VillageCoreBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VillageCoreBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.VILLAGE_CORE.get(),
                (lvl, pos, st, be) -> be.serverTick(lvl, pos, st));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        // Feed Nerosteel into the construction stockpile.
        if (stack.is(ModBlocks.NEROSTEEL_BLOCK.get().asItem())
                && level.getBlockEntity(pos) instanceof VillageCoreBlockEntity core) {
            if (!level.isClientSide()) {
                if (core.isClaimed() && !core.isOwner(player)) {
                    player.sendSystemMessage(Component.translatable(
                            "message.nerospace.village_core.owned", core.getOwnerName()));
                } else {
                    if (!core.isClaimed()) {
                        core.claim(player);
                    }
                    core.deposit(player, stack);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof VillageCoreBlockEntity core)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!core.isClaimed()) {
            core.claim(player);
            player.sendSystemMessage(Component.translatable("message.nerospace.village_core.claimed"));
        } else if (core.isOwner(player)) {
            core.onUse(player);
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.nerospace.village_core.owned", core.getOwnerName()));
        }
        return InteractionResult.SUCCESS;
    }
}
