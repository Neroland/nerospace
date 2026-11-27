package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
//? if >=1.20.3 {
import net.neoforged.neoforge.capabilities.Capabilities;
//?}
//? if >=1.21.9 {
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
//?}

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.neoforge.NeoForgeCapabilities;

/**
 * NeoForge query of the mod's fluid capability, falling back to the PLATFORM-STANDARD one.
 *
 * <p>Order matters and is not negotiable: the mod-private {@code nerospace:fluid} capability is asked
 * first, so every Nerospace block keeps the exact behaviour it has today — including the side-config
 * gating that returns {@code null} on a DISABLED face. Only when that comes back {@code null} do we ask
 * {@code Capabilities.Fluid.BLOCK} and wrap whatever a FOREIGN mod exposes there in
 * {@link NeoForgeFluidStorageAdapter}. That fallback is the half of the bridge that lets a Universal Pipe
 * fill another mod's tank; {@link NeoForgeFluidResourceHandler} is the half that lets another mod's pipe
 * fill ours.</p>
 *
 * <p>One consequence of the ordering is worth spelling out: Nerospace's own block entities are now
 * registered on BOTH capabilities, so a gated face that returns {@code null} from the mod-private
 * capability also returns {@code null} from the standard one (the adapter propagates {@code null}) and
 * the fallback finds nothing — the gate cannot be walked around through the standard capability.</p>
 */
public final class NeoForgeFluidLookup implements FluidLookup {

    @Nullable
    @Override
    public NerospaceFluidStorage find(Level level, BlockPos pos, @Nullable Direction side) {
        //? if >=1.20.3 {
        NerospaceFluidStorage own = level.getCapability(NeoForgeCapabilities.FLUID, pos, side);
        //?} else {
        /*NerospaceFluidStorage own = NeoForgeCapabilities.find(level, pos, NeoForgeCapabilities.FLUID, side);
        *///?}
        if (own != null) {
            return own;
        }
        // Five-arg query: the null state/block-entity let NeoForge resolve them itself.
        //? if >=1.21.9 {
        ResourceHandler<FluidResource> standard =
                Capabilities.Fluid.BLOCK.getCapability(level, pos, null, null, side);
        //?} else if >=1.20.3 {
        /*net.neoforged.neoforge.fluids.capability.IFluidHandler standard =
                Capabilities.FluidHandler.BLOCK.getCapability(level, pos, null, null, side);
        *///?} else {
        /*net.neoforged.neoforge.fluids.capability.IFluidHandler standard = NeoForgeCapabilities.find(level, pos,
                net.neoforged.neoforge.common.capabilities.Capabilities.FLUID_HANDLER, side);
        if (standard == null && level.getBlockState(pos).is(za.co.neroland.nerospace.registry.ModBlocks.ROCKET_LAUNCH_PAD.get())) {
            // NeoForge 20.2 attaches capabilities to block entities only; the pad has none.
            return new za.co.neroland.nerospace.rocket.RocketPadFluidProxy(level, pos);
        }
        *///?}
        return NeoForgeFluidStorageAdapter.of(standard);
    }
}
