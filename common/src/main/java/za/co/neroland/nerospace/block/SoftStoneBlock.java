package za.co.neroland.nerospace.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.SoftStoneRules;

/** Soft stone (Ashstone, 002 §1.3): instant-mine with any pickaxe; normal hardness otherwise. */
public class SoftStoneBlock extends Block {

    public static final MapCodec<SoftStoneBlock> CODEC = BlockCodecs.simple(SoftStoneBlock::new);

    public SoftStoneBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends SoftStoneBlock> codec() {
        return CODEC;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return SoftStoneRules.destroyProgress(player.getMainHandItem().is(ItemTags.PICKAXES),
                super.getDestroyProgress(state, player, level, pos));
    }
}
