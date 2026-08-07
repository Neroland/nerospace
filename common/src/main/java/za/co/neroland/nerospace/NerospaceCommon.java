package za.co.neroland.nerospace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.meteor.MeteorPlanets;
import za.co.neroland.nerolandcore.registry.CoreCreativeTab;

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
        installMeteorPlanetProvider();
        contributeToSharedTab();

        // The NeroLink module goes LAST, so a companion client is never told about something before the
        // mod itself has finished reacting to it, and its own init swallows any failure — a broken link
        // module must never take rockets, stations or life support down with it.
        NerospaceLinkModule.init();
    }

    /**
     * Installs Nerospace as the planet-lookup provider for Neroland Core's Meteor Material Registry — the
     * seam Core's resolver uses to weight planet-bound materials and apply the planet-bias multiplier
     * (see {@code ../neroland-core/docs/METEOR-MATERIAL-REGISTRY.md}). When a grind happens in a Nerospace
     * dimension (any {@code nerospace:*} dimension is a planet/space body), we report that dimension's
     * identifier as the "current planet"; anywhere else (Earth / another mod's dimension) we return
     * {@code null}, so Core treats the grind as off-world and planet-bound entries simply drop out of the
     * pool. The provider is server-side, loader-agnostic, and stores nothing per player (POPIA/GDPR).
     */
    private static void installMeteorPlanetProvider() {
        MeteorPlanets.setProvider(player -> {
            Identifier dim = player.level().dimension().identifier();
            return MOD_ID.equals(dim.getNamespace()) ? dim : null;
        });
    }

    /**
     * Surfaces Nerospace's signature materials + key progression items in Neroland Core's shared
     * "Neroland" creative tab (so a multi-mod pack shows one organised cross-mod tab), while Nerospace's
     * full catalogue stays in its own dedicated {@code ModCreativeTab}. The suppliers are lazy — resolved
     * by the tab at display time, never during this (pre-registration) init call — so they are safe on
     * every loader. Nerospace already publishes the {@code c:} convention tags for these materials, so
     * recipe/automation interop needs no further wiring.
     */
    private static void contributeToSharedTab() {
        CoreCreativeTab.add(() -> ModItems.RAW_NEROSIUM.get());
        CoreCreativeTab.add(() -> ModItems.NEROSIUM_INGOT.get());
        CoreCreativeTab.add(() -> ModItems.NEROSIUM_DUST.get());
        CoreCreativeTab.add(() -> ModItems.NEROSTEEL_INGOT.get());
        CoreCreativeTab.add(() -> ModItems.XERTZ_QUARTZ.get());
        CoreCreativeTab.add(() -> ModItems.CINDRITE.get());
        CoreCreativeTab.add(() -> ModItems.GLACITE.get());
        CoreCreativeTab.add(() -> ModItems.STAR_GUIDE_BOOK.get());
        CoreCreativeTab.add(() -> ModItems.STATION_CHARTER.get());
    }

    /**
     * POPIA/GDPR: register Nerospace's player-keyed stores with Neroland Core's shared erasure hook so a
     * single {@code /neroland data eraseme} (or Core's retention sweep) purges Nerospace too.
     *
     * <p>Registered at construction, ahead of the stores it purges: registering late is the classic way an
     * erasure request silently misses a store. There are <b>four</b> such stores — station ownership,
     * Alien Villager reputation (held in entity NBT), and the two per-player attachments — and
     * {@link za.co.neroland.nerospace.data.NerospaceErasure} documents exactly what each pass can and
     * cannot reach rather than claiming the job is fully done.</p>
     */
    private static void registerDataErasers() {
        NerospaceErasure.init();
    }
}
