package za.co.neroland.nerospace;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.telemetry.NerospaceTelemetry;
import za.co.neroland.nerospace.gametest.NerospaceGameTests;
import za.co.neroland.nerospace.registry.ModAttachments;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModCreativeModeTabs;
import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.registry.ModDataComponents;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.registry.ModMenuTypes;
import za.co.neroland.nerospace.registry.ModSounds;

/**
 * Nerospace — a space-exploration / tech-progression mod for Minecraft (Java Edition),
 * NeoForge 26.1, built standalone (no third-party mod dependencies; see PROJECT_PLAN.md §4).
 *
 * <p>This class is the registry spine: it wires each {@code DeferredRegister} to the mod event
 * bus during construction. Content lives in the {@code registry/}, {@code world/} and
 * {@code datagen/} packages.</p>
 */
@Mod(Nerospace.MODID)
public final class Nerospace {

    public static final String MODID = "nerospace";

    public static final Logger LOGGER = LogUtils.getLogger();

    public Nerospace(IEventBus modEventBus, ModContainer modContainer) {
        // Order matters: fluids before blocks/items (the liquid block + bucket resolve the fluid),
        // and blocks before items (block items reference block holders).
        ModFluids.register(modEventBus);
        ModBlocks.register(modEventBus);
        za.co.neroland.nerospace.registry.ModFeatures.register(modEventBus);
        ModItems.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModSounds.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModAttachments.register(modEventBus);
        za.co.neroland.nerospace.registry.ModCriteria.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        // Gametest instance-type codec (sync-safe serialization of the mod's code-backed tests).
        NerospaceGameTests.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        // Register the common config spec so FML manages the config file for us.
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Telemetry (Sentry error reporting, PRIVACY.md): start/stop strictly follows the
        // telemetryEnabled config value, so nothing initialises before the player's choice
        // is loaded and an opt-out applies immediately on config reload.
        modEventBus.addListener((final ModConfigEvent.Loading event) -> {
            if (event.getConfig().getSpec() == Config.SPEC) {
                NerospaceTelemetry.onConfigChanged(modContainer);
            }
        });
        modEventBus.addListener((final ModConfigEvent.Reloading event) -> {
            if (event.getConfig().getSpec() == Config.SPEC) {
                NerospaceTelemetry.onConfigChanged(modContainer);
            }
        });
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Nerospace common setup complete.");
    }
}
