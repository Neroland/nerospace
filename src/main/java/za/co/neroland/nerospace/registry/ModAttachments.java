package za.co.neroland.nerospace.registry;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.Nerospace;

/**
 * Per-entity data attachments (Phase 8c). {@link #OXYGEN} stores a player's remaining oxygen as an
 * {@code int} (0..{@code oxygenMax}); it persists across logout and copies on death. Only off-world
 * dimensions read/write it (see {@code GreenxertzAtmosphere}).
 */
public final class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Nerospace.MODID);

    public static final Supplier<AttachmentType<Integer>> OXYGEN = ATTACHMENT_TYPES.register(
            "oxygen",
            () -> AttachmentType.builder(() -> Config.OXYGEN_MAX.get())
                    .serialize(Codec.INT.fieldOf("oxygen"))
                    .copyOnDeath()
                    .build());

    /**
     * Per-{@code LevelChunk} terraformed flag (terraform design §3.4). Once a Terraformer converts a
     * chunk's ground it is permanently breathable at/above the surface — an O(1) flag test, no
     * simulation. This is ordinary gameplay save data, not analytics.
     */
    public static final Supplier<AttachmentType<Boolean>> TERRAFORMED = ATTACHMENT_TYPES.register(
            "terraformed",
            () -> AttachmentType.builder(() -> Boolean.FALSE)
                    .serialize(Codec.BOOL.fieldOf("terraformed"))
                    .build());

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
