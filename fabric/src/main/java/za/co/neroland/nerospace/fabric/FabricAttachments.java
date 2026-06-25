package za.co.neroland.nerospace.fabric;

import java.util.List;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.world.OxygenManager;

/**
 * Fabric side of the data-attachment seam (the NeoForge side uses {@code DeferredRegister} over
 * {@code ATTACHMENT_TYPES}). Oxygen persists across logout and copies on death; it defaults to
 * {@link OxygenManager#OXYGEN_MAX}.
 */
public final class FabricAttachments {

    private static final @NonNull Codec<Integer> INT_CODEC = NerospaceCommon.requireNonNull(Codec.INT);
    private static final @NonNull Codec<Boolean> BOOL_CODEC = NerospaceCommon.requireNonNull(Codec.BOOL);
    private static final @NonNull Codec<List<Integer>> INT_LIST_CODEC =
            NerospaceCommon.requireNonNull(Codec.INT.listOf());

    public static final @NonNull AttachmentType<Integer> OXYGEN = AttachmentRegistry.<Integer>builder()
            .initializer(() -> OxygenManager.OXYGEN_MAX)
            .persistent(INT_CODEC)
            .copyOnDeath()
            .buildAndRegister(NerospaceCommon.id("oxygen"));

    /** Per-chunk: the converted chunk is permanently breathable at/above the surface. */
    public static final @NonNull AttachmentType<Boolean> TERRAFORMED = AttachmentRegistry.<Boolean>builder()
            .initializer(() -> Boolean.FALSE)
            .persistent(BOOL_CODEC)
            .buildAndRegister(NerospaceCommon.id("terraformed"));

    /** Per-chunk: highest terraform stage completed (0 none / 1 Rooted / 2 Hydrated / 3 Living). */
    public static final @NonNull AttachmentType<Integer> TERRAFORM_STAGE = AttachmentRegistry.<Integer>builder()
            .initializer(() -> 0)
            .persistent(INT_CODEC)
            .buildAndRegister(NerospaceCommon.id("terraform_stage"));

    /** Per-player: one Star Guide "seen" bitmask per chapter (bit i = step i acknowledged). */
    public static final @NonNull AttachmentType<List<Integer>> STAR_GUIDE_SEEN = AttachmentRegistry.<List<Integer>>builder()
            .initializer(List::of)
            .persistent(INT_LIST_CODEC)
            .copyOnDeath()
            .buildAndRegister(NerospaceCommon.id("star_guide_seen"));

    private FabricAttachments() {
    }

    /** Touch to force class-load (and thus registration) at mod init. */
    public static void init() {
    }
}
