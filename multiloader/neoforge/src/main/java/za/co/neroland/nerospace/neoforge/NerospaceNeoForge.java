package za.co.neroland.nerospace.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.platform.NeoForgeFluidFactory;
import za.co.neroland.nerospace.registry.ModEntityAttributes;
import za.co.neroland.nerospace.registry.NeoForgeRegistrationFactory;

/**
 * NeoForge entry point. Runs shared init (building the DeferredRegisters via the
 * RegistrationProvider seam), attaches them to the mod bus, then fills creative
 * tabs from the common grouping.
 */
@Mod(NerospaceCommon.MOD_ID)
public final class NerospaceNeoForge {

    public NerospaceNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        NerospaceCommon.LOGGER.info("[Nerospace] NeoForge bootstrap");
        NerospaceCommon.init();
        NeoForgeFluidFactory.registerFluidTypes(modEventBus);
        NeoForgeRegistrationFactory.registerAll(modEventBus);
        NeoForgeCapabilities.register(modEventBus);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            NeoForgeClientSetup.init(modEventBus);
        }
        // Creative-tab contents are defined once by the cross-loader ModCreativeTab (a dedicated
        // Nerospace tab registered via the vanilla CREATIVE_MODE_TAB registry), so no NeoForge-specific
        // BuildCreativeModeTabContentsEvent injection is needed.
        modEventBus.addListener(this::onCreateEntityAttributes);
    }

    private void onCreateEntityAttributes(EntityAttributeCreationEvent event) {
        ModEntityAttributes.forEach((type, builder) -> event.put(type, builder.build()));
    }
}
