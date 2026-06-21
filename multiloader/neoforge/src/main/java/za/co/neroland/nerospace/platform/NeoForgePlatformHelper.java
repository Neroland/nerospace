package za.co.neroland.nerospace.platform;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;

import za.co.neroland.nerospace.NerospaceCommon;
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
    public java.nio.file.Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public String getModVersion() {
        return ModList.get().getModContainerById(NerospaceCommon.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    @Override
    public int getOxygen(Player player) {
        return player.getData(NeoForgeAttachments.OXYGEN.get());
    }

    @Override
    public void setOxygen(Player player, int value) {
        player.setData(NeoForgeAttachments.OXYGEN.get(), value);
    }

    @Override
    public boolean isTerraformed(LevelChunk chunk) {
        return chunk.getData(NeoForgeAttachments.TERRAFORMED.get());
    }

    @Override
    public void setTerraformed(LevelChunk chunk, boolean value) {
        chunk.setData(NeoForgeAttachments.TERRAFORMED.get(), value);
    }

    @Override
    public int getTerraformStage(LevelChunk chunk) {
        return chunk.getData(NeoForgeAttachments.TERRAFORM_STAGE.get());
    }

    @Override
    public void setTerraformStage(LevelChunk chunk, int value) {
        chunk.setData(NeoForgeAttachments.TERRAFORM_STAGE.get(), value);
    }
}
