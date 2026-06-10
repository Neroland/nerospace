package za.co.neroland.nerospace.registry;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.Tuning;

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
            () -> AttachmentType.builder(() -> Tuning.oxygenMax())
                    .serialize(Codec.INT.fieldOf("oxygen"))
                    .copyOnDeath()
                    .build());

    /**
     * Star Guide seen-state (STAR_GUIDE_DESIGN.md §3): per-player list of per-chapter bitmasks —
     * bit i of entry c = "the player has viewed step i of chapter c in the guide". Completion
     * itself lives in advancements; this only drives the completed-but-unseen pulse in the GUI.
     */
    public static final Supplier<AttachmentType<java.util.List<Integer>>> STAR_GUIDE_SEEN =
            ATTACHMENT_TYPES.register(
                    "star_guide_seen",
                    () -> AttachmentType.<java.util.List<Integer>>builder(() -> java.util.List.of())
                            .serialize(Codec.INT.listOf().fieldOf("seen"))
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

    /**
     * Per-{@code LevelChunk} terraform stage (DEEPER_TERRAFORM_DESIGN.md §2.2): the highest stage
     * (1 = Rooted, 2 = Hydrated, 3 = Living) any column in the chunk has completed. ADDITIVE next to
     * {@link #TERRAFORMED} (which stays the untouched breathability contract): legacy chunks carry the
     * boolean but no stage, so the effective stage is {@code max(stage, terraformed ? 1 : 0)} — see
     * {@code TerraformConversion.effectiveStage}.
     */
    public static final Supplier<AttachmentType<Integer>> TERRAFORM_STAGE = ATTACHMENT_TYPES.register(
            "terraform_stage",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT.fieldOf("stage"))
                    .build());

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
