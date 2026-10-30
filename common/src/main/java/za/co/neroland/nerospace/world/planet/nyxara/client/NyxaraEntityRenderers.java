package za.co.neroland.nerospace.world.planet.nyxara.client;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.client.ClientEntityRenderers;
import za.co.neroland.nerospace.client.GreenxertzCreatureRenderer;
import za.co.neroland.nerospace.world.planet.nyxara.NyxaraContent;

/**
 * Client renderers for Nyxara entities, called from the single {@link ClientEntityRenderers} call site on every
 * loader: the Dusk Moth on its own winged body ({@link DuskMothModel}) with the glow layer for its eye-spots.
 */
public final class NyxaraEntityRenderers {

    private NyxaraEntityRenderers() {
    }

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/entity/" + name + ".png");
    }

    public static void registerAll(ClientEntityRenderers.Sink sink) {
        sink.register(NyxaraContent.DUSK_MOTH.get(), context -> new GreenxertzCreatureRenderer(context,
                new DuskMothModel(DuskMothModel.createBodyLayer().bakeRoot()),
                tex("dusk_moth"), 0.7F, 0.7F, 0.7F, 0.2F, tex("dusk_moth_glow")));
    }
}
