package za.co.neroland.nerospace.neoforge;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

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
}
