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
import za.co.neroland.nerospace.block.rules.WetSeamRules;

/**
 * Brine Salt Ore: salt seams in the ice. Mined with brine in any of the 6 neighbours it gives twice the salt
 * ({@link WetSeamRules}, 8 per stack at most). Silk Touch lifts the block whole. Cost: on break only, 6 block reads.
 */
public class WetSeamOreBlock extends Block {

    public static final MapCodec<WetSeamOreBlock> CODEC = BlockCodecs.simple(WetSeamOreBlock::new);

    public WetSeamOreBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends WetSeamOreBlock> codec() {
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
        return EuropaDrops.apply(EuropaDropRules.Kind.BRINE_SALT_ORE, base, false, EuropaHeat.brineAdjacent(params.getLevel(), pos), 0);
    }
}
