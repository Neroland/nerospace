package za.co.neroland.nerospace.world.planet.europa;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A bucket of brine. Used on a block it places one Brine block (in the clicked cell when that is replaceable, else
 * against the clicked face) and leaves the empty bucket. Brine is a block, not a fluid: tanks and pipes do not take
 * it. Crafting with it returns the bucket.
 */
public class BrineBucketItem extends Item {

    public BrineBucketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        Block brine = EuropaContent.BRINE.get();
        BlockState at = level.getBlockState(clicked);
        BlockPos target = at.canBeReplaced() && !at.is(brine) ? clicked : clicked.relative(context.getClickedFace());
        BlockState there = level.getBlockState(target);
        if (!there.canBeReplaced() || there.is(brine) || !level.isInsideBuildHeight(target.getY())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlock(target, brine.defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, target, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            Player player = context.getPlayer();
            if (player != null && !player.isCreative()) {
                player.setItemInHand(context.getHand(), new ItemStack(Items.BUCKET));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
