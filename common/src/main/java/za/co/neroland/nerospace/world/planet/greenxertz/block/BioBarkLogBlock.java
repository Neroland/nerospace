package za.co.neroland.nerospace.world.planet.greenxertz.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.world.planet.greenxertz.GreenxertzContent;

/**
 * Bio-Bark Log: an axe strips it into the Stripped Bio-Bark Log, keeping the axis. Stripping is done here
 * (common code, every loader) instead of through a loader strippable registry. The unstripped log has no other
 * behaviour; the stripped one heals sleepers ({@code BioBarkHeal}).
 */
public class BioBarkLogBlock extends RotatedPillarBlock {

    public static final MapCodec<BioBarkLogBlock> CODEC = BlockCodecs.simple(BioBarkLogBlock::new);

    public BioBarkLogBlock(Properties properties) {
        super(properties);
    }

    public MapCodec<? extends RotatedPillarBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ItemTags.AXES)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        //? if >=26.3 {
        /*level.playSound(player, pos, SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
        *///?} else {
        level.playSound(player, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
        //?}
        if (!level.isClientSide()) {
            BlockState stripped = GreenxertzContent.STRIPPED_BIO_BARK_LOG.get().defaultBlockState()
                    .setValue(AXIS, state.getValue(AXIS));
            level.setBlock(pos, stripped, Block.UPDATE_ALL_IMMEDIATE);
            stack.hurtAndBreak(1, player, hand);
        }
        return InteractionResult.SUCCESS;
    }
}
