package za.co.neroland.nerospace.world.planet.nyxara.client;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.client.ClientEntityRenderers;
import za.co.neroland.nerospace.client.GreenlingModel;
import za.co.neroland.nerospace.client.GreenxertzCreatureRenderer;
import za.co.neroland.nerospace.world.planet.nyxara.NyxaraContent;

/**
 * Client renderers for Nyxara entities, called from the single {@link ClientEntityRenderers} call site on every
 * loader. The Dusk Moth is a placeholder (the Greenling body, small and flattened, violet-tinted, with a glow layer)
 * until the Stage 6 art pass.
 */
public final class NyxaraEntityRenderers {

    private NyxaraEntityRenderers() {
    }

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/entity/" + name + ".png");
    }

    public static void registerAll(ClientEntityRenderers.Sink sink) {
        sink.register(NyxaraContent.DUSK_MOTH.get(), context -> new GreenxertzCreatureRenderer(context,
                new GreenlingModel(GreenlingModel.createBodyLayer().bakeRoot()),
                tex("dusk_moth"), 0.7F, 0.5F, 0.7F, 0.2F, tex("dusk_moth_glow")));
    }
}
