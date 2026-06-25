package za.co.neroland.nerospace.platform;

import java.util.function.Supplier;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;

import org.jspecify.annotations.NonNull;

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
        return player.getData(attachment(NeoForgeAttachments.OXYGEN));
    }

    @Override
    public void setOxygen(Player player, int value) {
        player.setData(attachment(NeoForgeAttachments.OXYGEN), value);
    }

    @Override
    public boolean isTerraformed(LevelChunk chunk) {
        return chunk.getData(attachment(NeoForgeAttachments.TERRAFORMED));
    }

    @Override
    public void setTerraformed(LevelChunk chunk, boolean value) {
        chunk.setData(attachment(NeoForgeAttachments.TERRAFORMED), value);
    }

    @Override
    public int getTerraformStage(LevelChunk chunk) {
        return chunk.getData(attachment(NeoForgeAttachments.TERRAFORM_STAGE));
    }

    @Override
    public void setTerraformStage(LevelChunk chunk, int value) {
        chunk.setData(attachment(NeoForgeAttachments.TERRAFORM_STAGE), value);
    }

    @Override
    public java.util.List<Integer> getStarGuideSeen(Player player) {
        return player.getData(attachment(NeoForgeAttachments.STAR_GUIDE_SEEN));
    }

    @Override
    public void setStarGuideSeen(Player player, java.util.List<Integer> value) {
        player.setData(attachment(NeoForgeAttachments.STAR_GUIDE_SEEN), value);
    }

    private static <T> @NonNull AttachmentType<T> attachment(Supplier<AttachmentType<T>> supplier) {
        return NerospaceCommon.requireNonNull(supplier.get());
    }
}
