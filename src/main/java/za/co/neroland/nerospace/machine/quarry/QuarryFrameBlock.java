package za.co.neroland.nerospace.machine.quarry;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;

/**
 * The glowing structural frame the quarry materialises around its claimed region (MINER_DESIGN —
 * "build frame + drill head"). Built by the controller (one {@code frame_casing} per block) and
 * removed when the controller is removed; it carries no loot table, so breaking one by hand yields
 * nothing. A dedicated class so the mining loop can recognise and skip frames — its own and other
 * quarries' ("respect claims"). The translucent/emissive look is a client render-type choice; the
 * block is registered with {@code noOcclusion()} and an emissive light level.
 */
public class QuarryFrameBlock extends Block {

    public static final MapCodec<QuarryFrameBlock> CODEC = simpleCodec(QuarryFrameBlock::new);

    public QuarryFrameBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<QuarryFrameBlock> codec() {
        return CODEC;
    }
}
