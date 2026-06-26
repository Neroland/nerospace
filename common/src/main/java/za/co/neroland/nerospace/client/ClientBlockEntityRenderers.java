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
        // Solar panels: the sun-tracking deck above each tier's housing (one big deck per multiblock).
        sink.register(ModBlockEntities.SOLAR_PANEL.get(), context -> new SolarPanelRenderer());
        // Universal Pipe: the item stacks visibly travelling through the segment.
        sink.register(ModBlockEntities.UNIVERSAL_PIPE.get(), context -> new UniversalPipeRenderer());
        // Quarry controller: the gantry crane + spinning drill head tracking the dig.
        sink.register(ModBlockEntities.QUARRY_CONTROLLER.get(), context -> new QuarryControllerRenderer());
        // Launch gantry: the service tower that reclines to release a launching rocket, then swings back.
        sink.register(ModBlockEntities.LAUNCH_GANTRY.get(), context -> new LaunchGantryRenderer());
    }
}
