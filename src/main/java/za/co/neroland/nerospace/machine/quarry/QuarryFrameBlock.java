package za.co.neroland.nerospace.machine.quarry;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;

/**
 * The glowing structural frame the quarry materialises around its claimed region (MINER_DESIGN —
 * "build frame + drill head"). Built by the controller (one {@code frame_casing} per block) and
 * removed when the controller is removed; it carries no loot table, so breaking one by hand yields
 * nothing. A dedicated class so the mining loop can recognise and skip frames — its own and other
 * quarries' ("respect claims").
 *
 * <p>Renders as a real 3-D open structural frame — four corner posts + edge rails with a see-through
 * centre (BuildCraft-style), built by {@code ModModelProvider#registerQuarryFrame} from the same beam
 * layout as the tanks; emissive via the strut texture + the block's light level, and {@code
 * .noOcclusion()} so the open gaps don't cull the world behind. The controller's {@code
 * QuarryControllerRenderer} adds only the MOVING gantry parts (drill head + bridge) on top.</p>
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
