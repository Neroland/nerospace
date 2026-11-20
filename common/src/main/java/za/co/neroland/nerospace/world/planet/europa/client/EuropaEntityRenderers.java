package za.co.neroland.nerospace.world.planet.europa.client;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.client.ClientEntityRenderers;
import za.co.neroland.nerospace.client.GreenxertzCreatureRenderer;
import za.co.neroland.nerospace.world.planet.europa.EuropaContent;

/**
 * Client renderers for Europa entities, called from the single {@link ClientEntityRenderers} call site on every
 * loader: the Lantern Drifter (glowing core and ribbon tips, full-bright layer), the Vent Ray (ember gill lines in
 * the glow layer) and the Shell Skater (pale eyes in the glow layer). Models are baked inline, no layer registration.
 */
public final class EuropaEntityRenderers {

    private EuropaEntityRenderers() {
    }

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/entity/" + name + ".png");
    }

    public static void registerAll(ClientEntityRenderers.Sink sink) {
        sink.register(EuropaContent.LANTERN_DRIFTER.get(), context -> new GreenxertzCreatureRenderer(context,
                new LanternDrifterModel(LanternDrifterModel.createBodyLayer().bakeRoot()),
                tex("lantern_drifter"), 0.7F, 0.7F, 0.7F, 0.0F, tex("lantern_drifter_glow")));
        sink.register(EuropaContent.VENT_RAY.get(), context -> new GreenxertzCreatureRenderer(context,
                new VentRayModel(VentRayModel.createBodyLayer().bakeRoot()),
                tex("vent_ray"), 0.75F, 0.75F, 0.75F, 0.5F, tex("vent_ray_glow")));
        sink.register(EuropaContent.SHELL_SKATER.get(), context -> new GreenxertzCreatureRenderer(context,
                new ShellSkaterModel(ShellSkaterModel.createBodyLayer().bakeRoot()),
                tex("shell_skater"), 0.8F, 0.8F, 0.8F, 0.4F, tex("shell_skater_glow")));
    }
}
