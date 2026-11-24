package za.co.neroland.nerospace.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;

import za.co.neroland.nerospace.NerospaceCommon;
//? if >=1.20.4 || =1.20.1 {
import za.co.neroland.nerospace.fabric.FabricAttachments;
//?} else {
/*import za.co.neroland.nerospace.fabric.LegacyAttachments;
*///?}

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
    public java.nio.file.Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public String getModVersion() {
        return FabricLoader.getInstance().getModContainer(NerospaceCommon.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    @Override
    public java.util.List<String> getLoadedModIds() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(m -> m.getMetadata().getId() + " " + m.getMetadata().getVersion().getFriendlyString())
                .sorted()
                .toList();
    }

    //? if >=1.20.4 || =1.20.1 {
    @Override
    public int getOxygen(Player player) {
        return player.getAttachedOrCreate(FabricAttachments.OXYGEN);
    }

    @Override
    public void setOxygen(Player player, int value) {
        player.setAttached(FabricAttachments.OXYGEN, value);
    }

    @Override
    public boolean isTerraformed(LevelChunk chunk) {
        return chunk.getAttachedOrCreate(FabricAttachments.TERRAFORMED);
    }

    @Override
    public void setTerraformed(LevelChunk chunk, boolean value) {
        chunk.setAttached(FabricAttachments.TERRAFORMED, value);
    }

    @Override
    public int getTerraformStage(LevelChunk chunk) {
        return chunk.getAttachedOrCreate(FabricAttachments.TERRAFORM_STAGE);
    }

    @Override
    public void setTerraformStage(LevelChunk chunk, int value) {
        chunk.setAttached(FabricAttachments.TERRAFORM_STAGE, value);
    }

    @Override
    public java.util.List<Integer> getStarGuideSeen(Player player) {
        return player.getAttachedOrCreate(FabricAttachments.STAR_GUIDE_SEEN);
    }

    @Override
    public void setStarGuideSeen(Player player, java.util.List<Integer> value) {
        player.setAttached(FabricAttachments.STAR_GUIDE_SEEN, value);
    }
    //?} else {
    /*@Override
    public int getOxygen(Player player) {
        var data = ((LegacyAttachments) player).nerospace$data();
        return data.contains(LegacyAttachments.OXYGEN)
                ? data.getInt(LegacyAttachments.OXYGEN)
                : za.co.neroland.nerospace.world.OxygenManager.OXYGEN_MAX;
    }

    @Override
    public void setOxygen(Player player, int value) {
        ((LegacyAttachments) player).nerospace$data().putInt(LegacyAttachments.OXYGEN, value);
    }

    @Override
    public boolean isTerraformed(LevelChunk chunk) {
        return ((LegacyAttachments) chunk).nerospace$data().getBoolean(LegacyAttachments.TERRAFORMED);
    }

    @Override
    public void setTerraformed(LevelChunk chunk, boolean value) {
        ((LegacyAttachments) chunk).nerospace$data().putBoolean(LegacyAttachments.TERRAFORMED, value);
        chunk.setUnsaved(true);
    }

    @Override
    public int getTerraformStage(LevelChunk chunk) {
        return ((LegacyAttachments) chunk).nerospace$data().getInt(LegacyAttachments.TERRAFORM_STAGE);
    }

    @Override
    public void setTerraformStage(LevelChunk chunk, int value) {
        ((LegacyAttachments) chunk).nerospace$data().putInt(LegacyAttachments.TERRAFORM_STAGE, value);
        chunk.setUnsaved(true);
    }

    @Override
    public java.util.List<Integer> getStarGuideSeen(Player player) {
        var data = ((LegacyAttachments) player).nerospace$data();
        var tag = data.get(LegacyAttachments.STAR_GUIDE_SEEN);
        if (tag instanceof net.minecraft.nbt.CollectionTag<?> entries) {
            return entries.stream().filter(net.minecraft.nbt.NumericTag.class::isInstance)
                    .map(net.minecraft.nbt.NumericTag.class::cast)
                    .map(net.minecraft.nbt.NumericTag::getAsInt).toList();
        }
        return java.util.List.of();
    }

    @Override
    public void setStarGuideSeen(Player player, java.util.List<Integer> value) {
        ((LegacyAttachments) player).nerospace$data().putIntArray(LegacyAttachments.STAR_GUIDE_SEEN, value);
    }
    *///?}
}
