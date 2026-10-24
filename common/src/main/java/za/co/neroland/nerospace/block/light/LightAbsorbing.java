package za.co.neroland.nerospace.block.light;

import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerospace.block.LightAbsorbingBlock;
import za.co.neroland.nerospace.config.NerospaceConfig;

/** Shared predicate for the light-absorption mixins. */
public final class LightAbsorbing {

    private LightAbsorbing() {
    }

    public static boolean isAbsorber(BlockState state) {
        return state != null && state.getBlock() instanceof LightAbsorbingBlock;
    }

    /** {@code blocks.nyxStoneLightAbsorb}; read when indexing, so toggling it applies to chunks lit afterwards. */
    public static boolean enabled() {
        try {
            return NerospaceConfig.nyxStoneLightAbsorb();
        } catch (RuntimeException e) {
            return true; // config not loaded yet (very early client start): use the default
        }
    }
}
