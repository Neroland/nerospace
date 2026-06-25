package za.co.neroland.nerospace.neoforge;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.world.OxygenManager;

/**
 * NeoForge side of the data-attachment seam. The common {@code IPlatformHelper#getOxygen/setOxygen}
 * delegate here (the Fabric side uses the fabric data-attachment API). Oxygen persists across logout and
 * copies on death; it defaults to {@link OxygenManager#OXYGEN_MAX}.
 */
public final class NeoForgeAttachments {

    private static final @NonNull Codec<Integer> INT_CODEC = NerospaceCommon.requireNonNull(Codec.INT);
    private static final @NonNull Codec<Boolean> BOOL_CODEC = NerospaceCommon.requireNonNull(Codec.BOOL);
    private static final @NonNull Codec<List<Integer>> INT_LIST_CODEC =
            NerospaceCommon.requireNonNull(Codec.INT.listOf());
    private static final @NonNull MapCodec<Integer> OXYGEN_CODEC =
            NerospaceCommon.requireNonNull(INT_CODEC.fieldOf("oxygen"));
    private static final @NonNull MapCodec<Boolean> TERRAFORMED_CODEC =
            NerospaceCommon.requireNonNull(BOOL_CODEC.fieldOf("terraformed"));
    private static final @NonNull MapCodec<Integer> TERRAFORM_STAGE_CODEC =
            NerospaceCommon.requireNonNull(INT_CODEC.fieldOf("terraform_stage"));
    private static final @NonNull MapCodec<List<Integer>> STAR_GUIDE_SEEN_CODEC =
            NerospaceCommon.requireNonNull(INT_LIST_CODEC.fieldOf("star_guide_seen"));

    public static final @NonNull DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, NerospaceCommon.MOD_ID);

    public static final @NonNull Supplier<@NonNull AttachmentType<Integer>> OXYGEN = ATTACHMENT_TYPES.register(
            "oxygen",
            () -> AttachmentType.builder(() -> OxygenManager.OXYGEN_MAX)
                    .serialize(OXYGEN_CODEC)
                    .copyOnDeath()
                    .build());

    /** Per-chunk: the converted chunk is permanently breathable at/above the surface. */
    public static final @NonNull Supplier<@NonNull AttachmentType<Boolean>> TERRAFORMED = ATTACHMENT_TYPES.register(
            "terraformed",
            () -> AttachmentType.builder(() -> Boolean.FALSE)
                    .serialize(TERRAFORMED_CODEC)
                    .build());

    /** Per-chunk: highest terraform stage completed (0 none / 1 Rooted / 2 Hydrated / 3 Living). */
    public static final @NonNull Supplier<@NonNull AttachmentType<Integer>> TERRAFORM_STAGE = ATTACHMENT_TYPES.register(
            "terraform_stage",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(TERRAFORM_STAGE_CODEC)
                    .build());

    /** Per-player: one Star Guide "seen" bitmask per chapter (bit i = step i acknowledged). */
    public static final @NonNull Supplier<@NonNull AttachmentType<List<Integer>>> STAR_GUIDE_SEEN = ATTACHMENT_TYPES.register(
            "star_guide_seen",
            () -> AttachmentType.<List<Integer>>builder(() -> List.of())
                    .serialize(STAR_GUIDE_SEEN_CODEC)
                    .copyOnDeath()
                    .build());

    private NeoForgeAttachments() {
    }

    public static void register(@NonNull IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
