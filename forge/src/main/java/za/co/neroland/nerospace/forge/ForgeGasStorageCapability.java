package za.co.neroland.nerospace.forge;

import net.minecraftforge.common.capabilities.AutoRegisterCapability;

import za.co.neroland.nerospace.gas.NerospaceGasStorage;

/** Forge marker capability that exposes the shared Nerospace gas storage contract. */
@AutoRegisterCapability
public interface ForgeGasStorageCapability extends NerospaceGasStorage {
}
