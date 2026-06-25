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

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModBlocks;

/**
 * Village Core (ALIEN_VILLAGERS_DESIGN.md §4.1) — the glowing centerpiece of alien hamlets / ruins /
 * mega-cities, and the village's controller. Right-click to claim / teach the next building;
 * right-click with Nerosteel to stock materials, or with a quest item to hand it in; sneak-right-click
 * to collect produced goods and read the village's current task. It ticks construction, production and
 * raids via {@link VillageCoreBlockEntity#serverTick}.
 *
 * <p>Cross-loader port: the root's interactive controller block, on vanilla interactions
 * ({@code useItemOn}/{@code useWithoutItem}) + the shared {@code BaseEntityBlock} ticker seam. Replaces
 * the earlier decorative-only stub; structures keep placing it as their anchor and it is now live.</p>
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VillageCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, java.util.Objects.requireNonNull(ModBlockEntities.VILLAGE_CORE.get()),
                (lvl, pos, st, be) -> be.serverTick(lvl, pos, st));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof VillageCoreBlockEntity core)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        // Consume the interaction on the client (server is authoritative for deposits / quest hand-ins).
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (stack.is(java.util.Objects.requireNonNull(ModBlocks.NEROSTEEL_BLOCK.get()).asItem())) {
            if (core.isClaimed() && !core.isOwner(player)) {
                player.sendSystemMessage(Component.translatable(
                        "message.nerospace.village_core.owned", core.getOwnerName()));
            } else {
                if (!core.isClaimed()) {
                    core.claim(player);
                }
                core.deposit(player, stack);
            }
            return InteractionResult.SUCCESS;
        }
        if (core.tryCompleteQuest(player, stack)) {
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
        if (player.isShiftKeyDown()) {
            core.collectAndStatus(player);
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
