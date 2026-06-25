package za.co.neroland.nerospace.forge;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.world.OxygenManager;

/** Forge entity/chunk capability data backing oxygen, terraform state, and Star Guide seen masks. */
@SuppressWarnings("null")
public final class ForgeAttachments {

    public static final Capability<ForgePlayerDataCapability> PLAYER_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<ForgeChunkDataCapability> CHUNK_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    private static final @org.jspecify.annotations.NonNull Identifier PLAYER_DATA_ID =
            NerospaceCommon.id("player_data");
    private static final @org.jspecify.annotations.NonNull Identifier CHUNK_DATA_ID =
            NerospaceCommon.id("chunk_data");

    private ForgeAttachments() {
    }

    public static void register() {
        AttachCapabilitiesEvent.Entities.BUS.addListener(ForgeAttachments::onAttachEntity);
        AttachCapabilitiesEvent.LevelChunks.BUS.addListener(ForgeAttachments::onAttachChunk);
        PlayerEvent.Clone.BUS.addListener(ForgeAttachments::onPlayerClone);
    }

    private static void onAttachEntity(AttachCapabilitiesEvent.Entities event) {
        if (event.getObject() instanceof Player) {
            PlayerDataProvider provider = new PlayerDataProvider();
            event.addCapability(PLAYER_DATA_ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    private static void onAttachChunk(AttachCapabilitiesEvent.LevelChunks event) {
        ChunkDataProvider provider = new ChunkDataProvider();
        event.addCapability(CHUNK_DATA_ID, provider);
        event.addListener(provider::invalidate);
    }

    private static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        original.getCapability(PLAYER_DATA).ifPresent(oldData ->
                event.getEntity().getCapability(PLAYER_DATA).ifPresent(newData -> newData.copyFrom(oldData)));
        original.invalidateCaps();
    }

    private static final class PlayerData implements ForgePlayerDataCapability {
        private int oxygen = OxygenManager.OXYGEN_MAX;
        private List<Integer> starGuideSeen = List.of();

        @Override
        public int getOxygen() {
            return oxygen;
        }

        @Override
        public void setOxygen(int value) {
            this.oxygen = value;
        }

        @Override
        public List<Integer> getStarGuideSeen() {
            return starGuideSeen;
        }

        @Override
        public void setStarGuideSeen(List<Integer> value) {
            this.starGuideSeen = List.copyOf(value);
        }

        @Override
        public void copyFrom(ForgePlayerDataCapability other) {
            setOxygen(other.getOxygen());
            setStarGuideSeen(other.getStarGuideSeen());
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("oxygen", oxygen);
            tag.putIntArray("star_guide_seen", starGuideSeen.stream().mapToInt(Integer::intValue).toArray());
            return tag;
        }

        void load(CompoundTag tag) {
            oxygen = tag.getIntOr("oxygen", OxygenManager.OXYGEN_MAX);
            int[] seen = tag.getIntArray("star_guide_seen").orElse(new int[0]);
            List<Integer> values = new ArrayList<>(seen.length);
            for (int value : seen) {
                values.add(value);
            }
            starGuideSeen = List.copyOf(values);
        }
    }

    private static final class ChunkData implements ForgeChunkDataCapability {
        private boolean terraformed;
        private int terraformStage;

        @Override
        public boolean isTerraformed() {
            return terraformed;
        }

        @Override
        public void setTerraformed(boolean value) {
            this.terraformed = value;
        }

        @Override
        public int getTerraformStage() {
            return terraformStage;
        }

        @Override
        public void setTerraformStage(int value) {
            this.terraformStage = value;
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("terraformed", terraformed);
            tag.putInt("terraform_stage", terraformStage);
            return tag;
        }

        void load(CompoundTag tag) {
            terraformed = tag.getBooleanOr("terraformed", false);
            terraformStage = tag.getIntOr("terraform_stage", 0);
        }
    }

    private static final class PlayerDataProvider implements ICapabilitySerializable<CompoundTag> {
        private final PlayerData data = new PlayerData();
        private final LazyOptional<ForgePlayerDataCapability> optional = LazyOptional.of(() -> data);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
            return cap == PLAYER_DATA ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            return data.save();
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            data.load(nbt);
        }

        void invalidate() {
            optional.invalidate();
        }
    }

    private static final class ChunkDataProvider implements ICapabilitySerializable<CompoundTag> {
        private final ChunkData data = new ChunkData();
        private final LazyOptional<ForgeChunkDataCapability> optional = LazyOptional.of(() -> data);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
            return cap == CHUNK_DATA ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            return data.save();
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            data.load(nbt);
        }

        void invalidate() {
            optional.invalidate();
        }
    }
}
