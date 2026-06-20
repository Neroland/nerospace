package za.co.neroland.nerospace.platform;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.world.entity.player.Player;

import za.co.neroland.nerospace.neoforge.NeoForgeAttachments;

/**
 * NeoForge implementation of {@link IPlatformHelper}. Registered via
 * {@code META-INF/services/za.co.neroland.nerospace.platform.IPlatformHelper}.
 */
public final class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        // 26.1.x exposes these as methods (the old `FMLEnvironment.production`
        // / `.dist` fields were removed) — matches the root project's usage.
        return !FMLEnvironment.isProduction();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.getDist() == Dist.CLIENT;
    }

    @Override
    public int getOxygen(Player player) {
        return player.getData(NeoForgeAttachments.OXYGEN.get());
    }

    @Override
    public void setOxygen(Player player, int value) {
        player.setData(NeoForgeAttachments.OXYGEN.get(), value);
    }
}
