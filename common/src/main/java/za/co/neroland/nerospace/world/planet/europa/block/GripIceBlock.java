package za.co.neroland.nerospace.world.planet.europa.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.GripRules;

/**
 * Cut Europa Ice: scored ice that is quicker to walk on and does not slide. The behaviour is a registered property
 * ({@code speedFactor} from {@link GripRules#CUT_SPEED}, default friction); the class exists so the rule has an owner.
 */
public class GripIceBlock extends Block {

    public static final MapCodec<GripIceBlock> CODEC = BlockCodecs.simple(GripIceBlock::new);

    /** The speed factor to register the block with. */
    public static final float SPEED = GripRules.speedFactor(true);

    public GripIceBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends GripIceBlock> codec() {
        return CODEC;
    }
}
