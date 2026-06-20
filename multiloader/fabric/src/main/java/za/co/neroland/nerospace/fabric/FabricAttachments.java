package za.co.neroland.nerospace.fabric;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.world.OxygenManager;

/**
 * Fabric side of the data-attachment seam (the NeoForge side uses {@code DeferredRegister} over
 * {@code ATTACHMENT_TYPES}). Oxygen persists across logout and copies on death; it defaults to
 * {@link OxygenManager#OXYGEN_MAX}.
 */
public final class FabricAttachments {

    public static final AttachmentType<Integer> OXYGEN = AttachmentRegistry.<Integer>builder()
            .initializer(() -> OxygenManager.OXYGEN_MAX)
            .persistent(Codec.INT)
            .copyOnDeath()
            .buildAndRegister(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "oxygen"));

    private FabricAttachments() {
    }

    /** Touch to force class-load (and thus registration) at mod init. */
    public static void init() {
    }
}
