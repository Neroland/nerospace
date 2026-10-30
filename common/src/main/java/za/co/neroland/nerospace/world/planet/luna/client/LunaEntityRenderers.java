package za.co.neroland.nerospace.world.planet.luna.client;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.client.ClientEntityRenderers;
import za.co.neroland.nerospace.client.GreenxertzCreatureRenderer;
import za.co.neroland.nerospace.world.planet.luna.LunaContent;

/**
 * Client renderers for Luna entities, called from the single {@link ClientEntityRenderers} call site on every loader:
 * the Moon Hopper on its own hopping body ({@link MoonHopperModel}).
 */
public final class LunaEntityRenderers {

    private LunaEntityRenderers() {
    }

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/entity/" + name + ".png");
    }

    public static void registerAll(ClientEntityRenderers.Sink sink) {
        sink.register(LunaContent.MOON_HOPPER.get(), context -> new GreenxertzCreatureRenderer(context,
                new MoonHopperModel(MoonHopperModel.createBodyLayer().bakeRoot()),
                tex("moon_hopper"), 0.9F, 0.9F, 0.9F, 0.3F));
    }
}
