package za.co.neroland.nerospace.neoforge;

//? if >=1.20.3 {
import java.util.List;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.world.OxygenManager;

/**
 * NeoForge side of the data-attachment seam. The common {@code IPlatformHelper#getOxygen/setOxygen}
 * delegate here (the Fabric side uses the fabric data-attachment API). Oxygen persists across logout and
 * copies on death; it defaults to {@link OxygenManager#OXYGEN_MAX}.
 */
public final class NeoForgeAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, NerospaceCommon.MOD_ID);

    public static final Supplier<AttachmentType<Integer>> OXYGEN = ATTACHMENT_TYPES.register(
            "oxygen",
            () -> AttachmentType.builder(() -> OxygenManager.OXYGEN_MAX)
                    .serialize(Codec.INT.fieldOf("oxygen"))
                    .copyOnDeath()
                    .build());

    /** Per-chunk: the converted chunk is permanently breathable at/above the surface. */
    public static final Supplier<AttachmentType<Boolean>> TERRAFORMED = ATTACHMENT_TYPES.register(
            "terraformed",
            () -> AttachmentType.builder(() -> Boolean.FALSE)
                    .serialize(Codec.BOOL.fieldOf("terraformed"))
                    .build());

    /** Per-chunk: highest terraform stage completed (0 none / 1 Rooted / 2 Hydrated / 3 Living). */
    public static final Supplier<AttachmentType<Integer>> TERRAFORM_STAGE = ATTACHMENT_TYPES.register(
            "terraform_stage",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT.fieldOf("terraform_stage"))
                    .build());

    /** Per-player: one Star Guide "seen" bitmask per chapter (bit i = step i acknowledged). */
    public static final Supplier<AttachmentType<List<Integer>>> STAR_GUIDE_SEEN = ATTACHMENT_TYPES.register(
            "star_guide_seen",
            () -> AttachmentType.<List<Integer>>builder(() -> List.of())
                    .serialize(Codec.INT.listOf().fieldOf("star_guide_seen"))
                    .copyOnDeath()
                    .build());

    private NeoForgeAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    // Accessors used by the platform helper; the NeoForge 20.2 branch below backs the same calls with capabilities.
    public static int getOxygen(Player player) {
        return player.getData(OXYGEN.get());
    }

    public static void setOxygen(Player player, int value) {
        player.setData(OXYGEN.get(), value);
    }

    public static boolean isTerraformed(LevelChunk chunk) {
        return chunk.getData(TERRAFORMED.get());
    }

    public static void setTerraformed(LevelChunk chunk, boolean value) {
        chunk.setData(TERRAFORMED.get(), value);
    }

    public static int getTerraformStage(LevelChunk chunk) {
        return chunk.getData(TERRAFORM_STAGE.get());
    }

    public static void setTerraformStage(LevelChunk chunk, int value) {
        chunk.setData(TERRAFORM_STAGE.get(), value);
    }

    public static List<Integer> getStarGuideSeen(Player player) {
        return player.getData(STAR_GUIDE_SEEN.get());
    }

    public static void setStarGuideSeen(Player player, List<Integer> value) {
        player.setData(STAR_GUIDE_SEEN.get(), value);
    }
}
//?} else {
/*import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.capabilities.CapabilityManager;
import net.neoforged.neoforge.common.capabilities.CapabilityToken;
import net.neoforged.neoforge.common.capabilities.ICapabilitySerializable;
import net.neoforged.neoforge.common.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.util.LazyOptional;
import net.neoforged.neoforge.event.AttachCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.world.OxygenManager;

// NeoForge 20.2 has no data attachments: the same per-player (oxygen, Star Guide seen masks) and per-chunk
// (terraform state) data rides on serialisable capabilities, as on Forge. Same NBT keys as the Forge cells.
public final class NeoForgeAttachments {

    public static final Capability<PlayerData> PLAYER_DATA = CapabilityManager.get(new CapabilityToken<>() { });
    public static final Capability<ChunkData> CHUNK_DATA = CapabilityManager.get(new CapabilityToken<>() { });

    private static final ResourceLocation PLAYER_DATA_ID = new ResourceLocation(NerospaceCommon.MOD_ID, "player_data");
    private static final ResourceLocation CHUNK_DATA_ID = new ResourceLocation(NerospaceCommon.MOD_ID, "chunk_data");

    private NeoForgeAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener((RegisterCapabilitiesEvent event) -> {
            event.register(PlayerData.class);
            event.register(ChunkData.class);
        });
        NeoForge.EVENT_BUS.addGenericListener(Entity.class, NeoForgeAttachments::onAttachEntity);
        NeoForge.EVENT_BUS.addGenericListener(LevelChunk.class, NeoForgeAttachments::onAttachChunk);
        NeoForge.EVENT_BUS.addListener(NeoForgeAttachments::onPlayerClone);
    }

    public static int getOxygen(Player player) {
        return player.getCapability(PLAYER_DATA).map(data -> data.oxygen).orElse(OxygenManager.OXYGEN_MAX);
    }

    public static void setOxygen(Player player, int value) {
        player.getCapability(PLAYER_DATA).ifPresent(data -> data.oxygen = value);
    }

    public static boolean isTerraformed(LevelChunk chunk) {
        return chunk.getCapability(CHUNK_DATA).map(data -> data.terraformed).orElse(Boolean.FALSE);
    }

    public static void setTerraformed(LevelChunk chunk, boolean value) {
        chunk.getCapability(CHUNK_DATA).ifPresent(data -> data.terraformed = value);
        chunk.setUnsaved(true);
    }

    public static int getTerraformStage(LevelChunk chunk) {
        return chunk.getCapability(CHUNK_DATA).map(data -> data.terraformStage).orElse(0);
    }

    public static void setTerraformStage(LevelChunk chunk, int value) {
        chunk.getCapability(CHUNK_DATA).ifPresent(data -> data.terraformStage = value);
        chunk.setUnsaved(true);
    }

    public static List<Integer> getStarGuideSeen(Player player) {
        return player.getCapability(PLAYER_DATA).map(data -> data.starGuideSeen).orElse(List.of());
    }

    public static void setStarGuideSeen(Player player, List<Integer> value) {
        player.getCapability(PLAYER_DATA).ifPresent(data -> data.starGuideSeen = List.copyOf(value));
    }

    private static void onAttachEntity(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            Provider<PlayerData> provider = new Provider<>(PLAYER_DATA, new PlayerData());
            event.addCapability(PLAYER_DATA_ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    private static void onAttachChunk(AttachCapabilitiesEvent<LevelChunk> event) {
        Provider<ChunkData> provider = new Provider<>(CHUNK_DATA, new ChunkData());
        event.addCapability(CHUNK_DATA_ID, provider);
        event.addListener(provider::invalidate);
    }

    // Oxygen and the Star Guide masks survive death, like copyOnDeath() on the attachment cells.
    private static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        original.getCapability(PLAYER_DATA).ifPresent(old -> event.getEntity().getCapability(PLAYER_DATA)
                .ifPresent(fresh -> {
                    fresh.oxygen = old.oxygen;
                    fresh.starGuideSeen = old.starGuideSeen;
                }));
        original.invalidateCaps();
    }

    private interface Data {
        CompoundTag save();

        void load(CompoundTag tag);
    }

    public static final class PlayerData implements Data {
        private int oxygen = OxygenManager.OXYGEN_MAX;
        private List<Integer> starGuideSeen = List.of();

        @Override
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("oxygen", oxygen);
            tag.putIntArray("star_guide_seen", starGuideSeen.stream().mapToInt(Integer::intValue).toArray());
            return tag;
        }

        @Override
        public void load(CompoundTag tag) {
            oxygen = tag.contains("oxygen") ? tag.getInt("oxygen") : OxygenManager.OXYGEN_MAX;
            int[] seen = tag.getIntArray("star_guide_seen");
            List<Integer> values = new ArrayList<>(seen.length);
            for (int value : seen) {
                values.add(value);
            }
            starGuideSeen = List.copyOf(values);
        }
    }

    public static final class ChunkData implements Data {
        private boolean terraformed;
        private int terraformStage;

        @Override
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("terraformed", terraformed);
            tag.putInt("terraform_stage", terraformStage);
            return tag;
        }

        @Override
        public void load(CompoundTag tag) {
            terraformed = tag.getBoolean("terraformed");
            terraformStage = tag.getInt("terraform_stage");
        }
    }

    private static final class Provider<D extends Data> implements ICapabilitySerializable<CompoundTag> {
        private final Capability<D> capability;
        private final D data;
        private final LazyOptional<D> optional;

        Provider(Capability<D> capability, D data) {
            this.capability = capability;
            this.data = data;
            this.optional = LazyOptional.of(() -> data);
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            return cap == capability ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.save();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.load(nbt);
        }

        void invalidate() {
            optional.invalidate();
        }
    }
}
*///?}
