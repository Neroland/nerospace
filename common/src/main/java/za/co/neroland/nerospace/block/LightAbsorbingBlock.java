package za.co.neroland.nerospace.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * Light-absorbing stone (Nyx Stone, 002 §1.6/§11.1). Block light spreading into any cell of its 3×3×3 loses 2
 * extra levels (the {@code BlockLightEngineNyxMixin}); sky light is untouched. When {@code blocks.nyxStoneLightAbsorb}
 * is false — or another mod replaced the light engine — it is a plain full light blocker with no emission, the
 * documented fallback. {@link #useShapeForLightOcclusion} is true so placing/removing it always re-runs the light
 * check (vanilla skips the check for two opaque cubes with equal light properties).
 */
public class LightAbsorbingBlock extends Block {

    public static final MapCodec<LightAbsorbingBlock> CODEC = BlockCodecs.simple(LightAbsorbingBlock::new);

    public LightAbsorbingBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends LightAbsorbingBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }
}
