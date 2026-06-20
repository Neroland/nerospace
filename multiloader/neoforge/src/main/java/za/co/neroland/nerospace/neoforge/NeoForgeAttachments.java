package za.co.neroland.nerospace.neoforge;

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

    private NeoForgeAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
