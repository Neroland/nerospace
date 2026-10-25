package za.co.neroland.nerospace.world.planet.mars.client;

import net.minecraft.client.renderer.entity.NoopRenderer;

import za.co.neroland.nerospace.client.ClientEntityRenderers;
import za.co.neroland.nerospace.world.planet.mars.MarsContent;

/**
 * Client renderers for Mars entities. The Dust Devil has no model: it draws itself as a column of red dust particles
 * from its client tick, so the renderer is a no-op placeholder until the art pass.
 */
public final class MarsEntityRenderers {

    private MarsEntityRenderers() {
    }

    public static void registerAll(ClientEntityRenderers.Sink sink) {
        sink.register(MarsContent.DUST_DEVIL.get(), NoopRenderer::new);
    }
}
