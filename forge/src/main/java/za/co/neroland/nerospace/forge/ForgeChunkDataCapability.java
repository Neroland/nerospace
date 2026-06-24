package za.co.neroland.nerospace.forge;

import net.minecraftforge.common.capabilities.AutoRegisterCapability;

/** Forge per-chunk persisted data backing terraform state in the platform-helper seam. */
@AutoRegisterCapability
public interface ForgeChunkDataCapability {

    boolean isTerraformed();

    void setTerraformed(boolean value);

    int getTerraformStage();

    void setTerraformStage(int value);
}
