package za.co.neroland.nerospace.forge;

import net.minecraftforge.common.capabilities.AutoRegisterCapability;

import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;

/** Forge marker capability that exposes the shared Nerospace energy storage contract. */
@AutoRegisterCapability
public interface ForgeEnergyStorageCapability extends NerospaceEnergyStorage {
}
