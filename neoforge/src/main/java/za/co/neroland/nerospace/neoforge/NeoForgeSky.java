package za.co.neroland.nerospace.neoforge;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
//? if >=1.21.2 {
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
//?}

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.client.sky.SkyProfiles;

/**
 * NeoForge client sky wiring: the sky-profile reload listener (all versions) and, on 26.3, the planet
 * {@code CustomSkyboxRenderer} ({@code NeoForgePlanetSky}) registered as {@code nerospace:planet}. Every Nerospace
 * dimension type selects it through the {@code neoforge:custom_skybox} attribute in the NeoForge-only 26.3 resource
 * overlay ({@code tools/gen_sky_overlays.py}); it falls back to the vanilla sky for a level without a planet profile.
 */
public final class NeoForgeSky {

    /** The custom skybox id a dimension type attribute refers to. */
    public static final Identifier SKY_ID = Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "planet");

    private NeoForgeSky() {
    }

    public static void init(IEventBus modEventBus) {
        //? if >=1.21.2 {
        modEventBus.addListener((AddClientReloadListenersEvent event) ->
                event.addListener(SkyProfiles.ID, SkyProfiles.INSTANCE));
        //?} else {
        /*// NeoForge 21.1 registers client reload listeners without an id.
        modEventBus.addListener((net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener(SkyProfiles.INSTANCE));
        *///?}
        //? if >=26.3 {
        /*modEventBus.addListener((net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent event) ->
                event.registerSkyboxRenderer(SKY_ID, new NeoForgePlanetSky()));
        *///?}
    }
}
