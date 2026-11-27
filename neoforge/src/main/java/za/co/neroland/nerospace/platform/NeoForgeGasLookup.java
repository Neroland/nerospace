package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.neoforge.NeoForgeCapabilities;

/** NeoForge query of the mod's gas capability. */
public final class NeoForgeGasLookup implements GasLookup {

    @Nullable
    @Override
    public NerospaceGasStorage find(Level level, BlockPos pos, @Nullable Direction side) {
        //? if >=1.20.3 {
        return level.getCapability(NeoForgeCapabilities.GAS, pos, side);
        //?} else {
        /*NerospaceGasStorage own = NeoForgeCapabilities.find(level, pos, NeoForgeCapabilities.GAS, side);
        if (own == null && level.getBlockState(pos).is(za.co.neroland.nerospace.registry.ModBlocks.ROCKET_LAUNCH_PAD.get())) {
            // NeoForge 20.2 attaches capabilities to block entities only; the pad has none.
            return new za.co.neroland.nerospace.rocket.RocketPadGasProxy(level, pos);
        }
        return own;
        *///?}
    }
}
