package za.co.neroland.nerospace.platform;

import java.nio.file.Path;
import java.util.List;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.forge.ForgeChunkDataCapability;
import za.co.neroland.nerospace.forge.ForgeAttachments;
import za.co.neroland.nerospace.forge.ForgePlayerDataCapability;
import za.co.neroland.nerospace.world.OxygenManager;

/** Forge implementation of {@link IPlatformHelper}. */
@SuppressWarnings("null")
public final class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Forge";
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.production;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.isLoaded(modId);
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public String getModVersion() {
        return ModList.getModContainerById(NerospaceCommon.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    @Override
    public int getOxygen(Player player) {
        return player.getCapability(ForgeAttachments.PLAYER_DATA)
                .map(ForgePlayerDataCapability::getOxygen)
                .orElse(OxygenManager.OXYGEN_MAX);
    }

    @Override
    public void setOxygen(Player player, int value) {
        player.getCapability(ForgeAttachments.PLAYER_DATA).ifPresent(data -> data.setOxygen(value));
    }

    @Override
    public boolean isTerraformed(LevelChunk chunk) {
        return chunk.getCapability(ForgeAttachments.CHUNK_DATA, null)
                .map(ForgeChunkDataCapability::isTerraformed)
                .orElse(Boolean.FALSE);
    }

    @Override
    public void setTerraformed(LevelChunk chunk, boolean value) {
        chunk.getCapability(ForgeAttachments.CHUNK_DATA, null).ifPresent(data -> data.setTerraformed(value));
    }

    @Override
    public int getTerraformStage(LevelChunk chunk) {
        return chunk.getCapability(ForgeAttachments.CHUNK_DATA, null)
                .map(ForgeChunkDataCapability::getTerraformStage)
                .orElse(0);
    }

    @Override
    public void setTerraformStage(LevelChunk chunk, int value) {
        chunk.getCapability(ForgeAttachments.CHUNK_DATA, null).ifPresent(data -> data.setTerraformStage(value));
    }

    @Override
    public List<Integer> getStarGuideSeen(Player player) {
        return player.getCapability(ForgeAttachments.PLAYER_DATA)
                .map(ForgePlayerDataCapability::getStarGuideSeen)
                .orElse(List.of());
    }

    @Override
    public void setStarGuideSeen(Player player, List<Integer> value) {
        player.getCapability(ForgeAttachments.PLAYER_DATA).ifPresent(data -> data.setStarGuideSeen(value));
    }
}
