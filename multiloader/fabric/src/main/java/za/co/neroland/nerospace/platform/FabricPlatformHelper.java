package za.co.neroland.nerospace.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;

import za.co.neroland.nerospace.fabric.FabricAttachments;

/**
 * Fabric implementation of {@link IPlatformHelper}. Registered via
 * {@code META-INF/services/za.co.neroland.nerospace.platform.IPlatformHelper}.
 */
public final class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public int getOxygen(Player player) {
        return player.getAttachedOrCreate(FabricAttachments.OXYGEN);
    }

    @Override
    public void setOxygen(Player player, int value) {
        player.setAttached(FabricAttachments.OXYGEN, value);
    }
}
