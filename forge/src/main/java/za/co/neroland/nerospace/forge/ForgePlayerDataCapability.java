package za.co.neroland.nerospace.forge;

import java.util.List;

import net.minecraftforge.common.capabilities.AutoRegisterCapability;

/** Forge per-player persisted data backing the existing platform-helper seam. */
@AutoRegisterCapability
public interface ForgePlayerDataCapability {

    int getOxygen();

    void setOxygen(int value);

    List<Integer> getStarGuideSeen();

    void setStarGuideSeen(List<Integer> value);

    void copyFrom(ForgePlayerDataCapability other);
}
