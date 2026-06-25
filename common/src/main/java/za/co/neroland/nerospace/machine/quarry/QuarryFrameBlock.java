package za.co.neroland.nerospace.machine.quarry;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;

/**
 * The structural frame the quarry materialises around its claimed region. Built by the controller (one
 * {@code frame_casing} per block) and removed when the controller is removed; carries no loot table.
 * A dedicated class so the mining loop can recognise and skip frames.
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
