package za.co.neroland.nerospace.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * Opaque airtight hull (Hull Plating). The airtight behaviour is data: the block must be in
 * {@code nerospace:airtight} (and therefore {@code nerospace:oxygen_sealing}); {@code OxygenField} and the Station
 * Console read the tag. This class exists so the planet and station content has one named type to register and to test against.
 */
public class AirtightBlock extends Block {

    public static final MapCodec<AirtightBlock> CODEC = BlockCodecs.simple(AirtightBlock::new);

    public AirtightBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends AirtightBlock> codec() {
        return CODEC;
    }
}
