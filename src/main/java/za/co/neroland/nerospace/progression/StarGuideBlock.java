package za.co.neroland.nerospace.progression;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import za.co.neroland.nerospace.registry.ModItems;

/**
 * The Star Guide pedestal block (STAR_GUIDE_DESIGN.md §4). Lectern-style: install a Star Guide
 * Book to load it (the hologram lights up and right-click opens the progression tree);
 * sneak-right-click returns the book; breaking a loaded pedestal drops both.
 */
public class StarGuideBlock extends BaseEntityBlock {

    public static final MapCodec<StarGuideBlock> CODEC = simpleCodec(StarGuideBlock::new);

    public StarGuideBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<StarGuideBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StarGuideBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.STAR_GUIDE.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        // Install a Star Guide Book on the bare pedestal.
        if (stack.is(ModItems.STAR_GUIDE_BOOK.get())
                && level.getBlockEntity(pos) instanceof StarGuideBlockEntity guide && !guide.hasBook()) {
            if (!level.isClientSide() && guide.installBook(stack)) {
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BOOK_PUT,
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof StarGuideBlockEntity guide)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!guide.hasBook()) {
            player.sendSystemMessage(Component.translatable("message.nerospace.star_guide.empty"));
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            // Return the installed book.
            ItemStack book = guide.removeBook();
            if (!book.isEmpty() && !player.addItem(book)) {
                player.drop(book, false);
            }
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BOOK_PUT,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(guide);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof StarGuideBlockEntity guide ? guide.comparatorSignal() : 0;
    }
}
