package za.co.neroland.nerospace.world.planet.europa.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.BrineRules;
import za.co.neroland.nerospace.world.planet.europa.EuropaContent;

/**
 * Brine: a fluid-like block, not a fluid. It has no collision, never flows and never freezes. An entity whose feet are
 * in it is slowed (drag 0.7), pushed up a little (0.04 per tick at most), has its fall reset and is put out if it
 * burns. An empty bucket used on it takes the block and gives a Brine Bucket; pipes and tanks do not move it.
 * Cost: constant work per entity per tick, no scan.
 */
public class BrineBlock extends HalfTransparentBlock {

    public static final MapCodec<BrineBlock> CODEC = BlockCodecs.simple(BrineBlock::new);

    /** Height of the liquid surface inside the block. */
    private static final double SURFACE = 14.0 / 16.0;

    public BrineBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends BrineBlock> codec() {
        return CODEC;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effects, boolean isPrecise) {
        // An entity overlaps several brine cells; only the one at its feet acts, so the drag is applied once a tick.
        if (pos.equals(entity.blockPosition())) {
            if (BrineRules.resetsFall()) {
                entity.resetFallDistance();
            }
            double lift = BrineRules.buoyancy(
                    BrineRules.submergedFraction(entity.getY(), entity.getBbHeight(), pos.getY() + SURFACE));
            Vec3 v = entity.getDeltaMovement();
            entity.setDeltaMovement(v.x * BrineRules.drag(), v.y * BrineRules.drag() + lift, v.z * BrineRules.drag());
            if (BrineRules.extinguishes() && entity.isOnFire()) {
                entity.clearFire();
            }
        }
        super.entityInside(state, level, pos, entity, effects, isPrecise);
    }

    //? if >=1.21.2 {
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
    //?} else if >=1.20.5 {
    /*// Before 1.21.2 useItemOn returns an ItemInteractionResult; the 26.x body below runs as a helper.
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return za.co.neroland.nerospace.compat.mc.UseCompat.toItem(
                useItemOnResult(stack, state, level, pos, player, hand, hit));
    }

    private InteractionResult useItemOnResult(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
    *///?} else {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = useItemOnResult(player.getItemInHand(hand), state, level, pos, player, hand, hit);
        return result == null ? InteractionResult.PASS : result;
    }

    private InteractionResult useItemOnResult(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
    *///?}
        if (!stack.is(Items.BUCKET)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player,
                    new ItemStack(EuropaContent.BRINE_BUCKET.get())));
        }
        return InteractionResult.SUCCESS;
    }
}
