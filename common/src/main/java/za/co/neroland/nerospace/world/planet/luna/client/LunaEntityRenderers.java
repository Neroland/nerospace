package za.co.neroland.nerospace.world.planet.luna.client;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.client.ClientEntityRenderers;
import za.co.neroland.nerospace.client.GreenlingModel;
import za.co.neroland.nerospace.client.GreenxertzCreatureRenderer;
import za.co.neroland.nerospace.world.planet.luna.LunaContent;

/**
 * Client renderers for Luna entities, called from the single {@link ClientEntityRenderers} call site on every loader.
 * The Moon Hopper is a placeholder (the Greenling body, scaled, with a grey-tinted texture) until the Stage 6 art pass.
 */
public final class LunaEntityRenderers {

    private LunaEntityRenderers() {
    }

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/entity/" + name + ".png");
    }

    public static void registerAll(ClientEntityRenderers.Sink sink) {
        sink.register(LunaContent.MOON_HOPPER.get(), context -> new GreenxertzCreatureRenderer(context,
                new GreenlingModel(GreenlingModel.createBodyLayer().bakeRoot()),
                tex("moon_hopper"), 0.9F, 0.9F, 0.9F, 0.3F));
    }
}
