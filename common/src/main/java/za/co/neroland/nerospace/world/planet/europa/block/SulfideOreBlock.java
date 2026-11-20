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
import za.co.neroland.nerospace.block.rules.SulfideRules;

/**
 * Vent Sulfide Ore: metal sulfides of the vent fields. Mined wet (water or brine in any of the 6 neighbours) it gives
 * its raw ore; mined dry the sulfide oxidises and one sulfur is all that is left. Silk Touch lifts the block whole.
 * Cost: on break only, 6 block reads.
 */
public class SulfideOreBlock extends Block {

    public static final MapCodec<SulfideOreBlock> CODEC = BlockCodecs.simple(SulfideOreBlock::new);

    public SulfideOreBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends SulfideOreBlock> codec() {
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
        return EuropaDrops.apply(EuropaDropRules.Kind.VENT_SULFIDE_ORE, base, false, SulfideRules.wet(EuropaHeat.liquidAdjacent(params.getLevel(), pos), false), 0);
    }
}
