package za.co.neroland.nerospace.world.planet.europa.block;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.EuropaDropRules;
import za.co.neroland.nerospace.block.rules.SublimeRules;

/**
 * Deuterium Ice: a fuel ore of the ice shell. Mined cold it gives deuterium shards; mined with a heat source within 4
 * blocks it boils off and gives nothing. Silk Touch lifts the block whole. The rule runs in {@code getDrops}, so a
 * quarry and an explosion follow it too. Cost: on break only, 729 block reads at most with an early exit.
 */
public class DeuteriumIceBlock extends Block {

    public static final MapCodec<DeuteriumIceBlock> CODEC = BlockCodecs.simple(DeuteriumIceBlock::new);

    public DeuteriumIceBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends DeuteriumIceBlock> codec() {
        return CODEC;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> base = super.getDrops(state, params);
        boolean silk = EuropaDrops.silk(params, state, base);
        BlockPos pos = EuropaDrops.origin(params);
        if (silk || pos == null || base.isEmpty()) {
            return base;
        }
        return EuropaDrops.apply(EuropaDropRules.Kind.DEUTERIUM_ICE, base, false, heatNear(params.getLevel(), pos), 0);
    }

    private static boolean heatNear(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int r = SublimeRules.RANGE;
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    p.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (EuropaHeat.isHeat(level.getBlockState(p))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
