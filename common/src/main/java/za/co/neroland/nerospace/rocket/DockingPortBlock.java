package za.co.neroland.nerospace.rocket;

import com.mojang.serialization.MapCodec;

/** Station-side return-site crate/dock for rockets arriving in orbit. */
public class DockingPortBlock extends ReturnSiteBlock {

    public static final MapCodec<DockingPortBlock> CODEC = simpleCodec(DockingPortBlock::new);

    public DockingPortBlock(Properties properties) {
        super(properties, false);
    }

    @Override
    protected MapCodec<DockingPortBlock> codec() {
        return CODEC;
    }
}
