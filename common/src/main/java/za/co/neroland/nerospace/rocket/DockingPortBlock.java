package za.co.neroland.nerospace.rocket;

import com.mojang.serialization.MapCodec;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

/** Station-side return-site crate/dock for rockets arriving in orbit. */
public class DockingPortBlock extends ReturnSiteBlock {

    public static final MapCodec<DockingPortBlock> CODEC = BlockCodecs.simple(DockingPortBlock::new);

    public DockingPortBlock(Properties properties) {
        super(properties, false);
    }

    protected MapCodec<DockingPortBlock> codec() {
        return CODEC;
    }
}
