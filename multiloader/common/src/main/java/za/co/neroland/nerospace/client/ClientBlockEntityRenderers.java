package za.co.neroland.nerospace.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Cross-loader block-entity-renderer wiring (the BER analogue of {@link ClientEntityRenderers}). The
 * renderer set is identical on both loaders, so it lives here once and each loader passes its own
 * registration function ({@link Sink}) — NeoForge's {@code RegisterRenderers} event
 * ({@code registerBlockEntityRenderer}), Fabric's {@code BlockEntityRenderers.register}.
 */
public final class ClientBlockEntityRenderers {

    /** A loader's BER-registration entry point. */
    public interface Sink {
        <T extends BlockEntity, S extends BlockEntityRenderState> void register(
                BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider);
    }

    private ClientBlockEntityRenderers() {
    }

    public static void registerAll(Sink sink) {
        // Star Guide pedestal: the floating, spinning next-step hologram above a loaded pedestal.
        sink.register(ModBlockEntities.STAR_GUIDE.get(), context -> new StarGuideHologramRenderer());
    }
}
