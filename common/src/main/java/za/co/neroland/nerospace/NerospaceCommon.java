package za.co.neroland.nerospace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import za.co.neroland.nerolandcore.meteor.MeteorPlanets;

import za.co.neroland.nerospace.data.NerospaceErasure;
import za.co.neroland.nerospace.link.NerospaceLinkModule;
import za.co.neroland.nerospace.platform.Services;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.registry.ModRegistries;

/**
 * Loader-agnostic entry point. Both {@code NerospaceFabric} and
 * {@code NerospaceNeoForge} call {@link #init()} during mod construction.
 * Loader-specific behaviour is reached only through {@link Services}, keeping
 * this module free of {@code net.neoforged.*} / {@code net.fabricmc.*} imports.
 */
public final class NerospaceCommon {

    public static final String MOD_ID = "nerospace";
    public static final Logger LOGGER = LoggerFactory.getLogger("Nerospace");

    private NerospaceCommon() {
    }

    /** Called once per loader during mod construction. */
    public static void init() {
        LOGGER.info("[Nerospace] common init on platform: {} (dev={})",
                Services.PLATFORM.getPlatformName(),
                Services.PLATFORM.isDevelopmentEnvironment());

        // Shared content registration via the RegistrationProvider seam. On
        // NeoForge this builds DeferredRegisters (the loader entry point then
        // attaches them to the mod bus); on Fabric it registers eagerly.
        ModRegistries.init();

        // Resolve the remaining platform seams while the mod jar is still guaranteed readable, rather
        // than mid-tick on first use (Sentry MC-NEROSPACE-F).
        Services.preload();

        registerDataErasers();
        // Optional NeroEconomy fees/rewards: claims its caller handle here (it must happen at common init).
        // Inert without NeroEconomy, and every amount defaults to 0 = off.
        za.co.neroland.nerospace.compat.neroeconomy.NerospaceEconomy.init();
        installMeteorPlanetProvider();
        // Hook gates (Star Guide hint, oxygen top-up): listens on Core's gate bus; no quest mod is imported.
        za.co.neroland.nerospace.progression.QuestHooks.init();

        // The NeroLink module goes LAST, so a companion client is never told about something before the
        // mod itself has finished reacting to it, and its own init swallows any failure — a broken link
        // module must never take rockets, stations or life support down with it.
        NerospaceLinkModule.init();
    }

    /**
     * Installs Nerospace as the planet-lookup provider for Neroland Core's Meteor Material Registry — the
     * seam Core's resolver uses to weight planet-bound materials and apply the planet-bias multiplier
     * (see Core's meteor material registry docs). When a grind happens on a Nerospace planet surface we report
     * that dimension's identifier as the "current planet"; anywhere else (Earth, another mod's dimension, the
     * orbital station void, or the dev gallery backdrop dimension) we return {@code null}, so Core treats the
     * grind as off-world and planet-bound entries simply drop out of the pool. The provider is server-side, loader-agnostic, and stores nothing per player (POPIA/GDPR).
     */
    private static void installMeteorPlanetProvider() {
        MeteorPlanets.setProvider(player -> {
            var planet = za.co.neroland.nerospace.world.planet.Planets.get(player.level().dimension());
            return planet != null
                    && planet.traits().archetype() != za.co.neroland.nerospace.api.PlanetArchetype.ORBITAL
                    ? planet.id().id() : null;
        });
    }

    /**
     * POPIA/GDPR: register Nerospace's player-keyed stores with Neroland Core's shared erasure hook so a
     * single {@code /neroland data eraseme} (or Core's retention sweep) purges Nerospace too.
     *
     * <p>Registered at construction, ahead of the stores it purges: registering late is the classic way an
     * erasure request silently misses a store. There are <b>six</b> such stores — station ownership, planet
     * visits, cargo routes, Alien Villager reputation (held in entity NBT), and the two per-player attachments — and
     * {@link za.co.neroland.nerospace.data.NerospaceErasure} documents exactly what each pass can and
     * cannot reach rather than claiming the job is fully done.</p>
     */
    private static void registerDataErasers() {
        NerospaceErasure.init();
    }
}
