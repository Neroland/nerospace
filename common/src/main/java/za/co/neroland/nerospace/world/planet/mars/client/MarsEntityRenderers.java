package za.co.neroland.nerospace.world.planet.mars.client;

import net.minecraft.client.renderer.entity.NoopRenderer;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.client.ClientEntityRenderers;
import za.co.neroland.nerospace.client.GreenxertzCreatureRenderer;
import za.co.neroland.nerospace.client.MeadowLoperModel;
import za.co.neroland.nerospace.client.QuartzCrawlerModel;
import za.co.neroland.nerospace.world.planet.mars.MarsContent;

/**
 * Client renderers for Mars entities. The Dust Devil has no model: it draws itself as a column of red dust particles
 * from its client tick, so the renderer is a no-op placeholder until the art pass.
 */
public final class MarsEntityRenderers {

    private MarsEntityRenderers() {
    }

    private static net.minecraft.resources.Identifier tex(String name) {
        return net.minecraft.resources.Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID,
                "textures/entity/" + name + ".png");
    }

    public static void registerAll(ClientEntityRenderers.Sink sink) {
        sink.register(MarsContent.DUST_DEVIL.get(), NoopRenderer::new);
        // Placeholder body (the crawler model, scaled down) until the Stage 6 art pass models the drone.
        sink.register(MarsContent.SALVAGE_DRONE.get(), context -> new GreenxertzCreatureRenderer(context,
                new QuartzCrawlerModel(QuartzCrawlerModel.createBodyLayer().bakeRoot()),
                tex("salvage_drone"), 0.7F, 0.7F, 0.7F, 0.3F, tex("salvage_drone_glow")));
        // Placeholder body (the Meadow Loper model, stretched tall, rust-tinted texture) until the Stage 6 art pass.
        sink.register(MarsContent.DUST_STRIDER.get(), context -> new GreenxertzCreatureRenderer(context,
                new MeadowLoperModel(MeadowLoperModel.createBodyLayer().bakeRoot()),
                tex("dust_strider"), 0.85F, 1.2F, 0.85F, 0.5F));
    }
}
