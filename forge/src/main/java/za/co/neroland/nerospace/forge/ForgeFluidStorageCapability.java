package za.co.neroland.nerospace.forge;

import net.minecraftforge.common.capabilities.AutoRegisterCapability;

import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;

/** Forge marker capability that exposes the shared Nerospace fluid storage contract. */
@AutoRegisterCapability
public interface ForgeFluidStorageCapability extends NerospaceFluidStorage {
}
