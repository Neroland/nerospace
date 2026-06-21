package za.co.neroland.nerospace.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.platform.NeoForgeFluidFactory;
import za.co.neroland.nerospace.registry.ModEntityAttributes;
import za.co.neroland.nerospace.registry.ModSpawnPlacements;
import za.co.neroland.nerospace.registry.NeoForgeRegistrationFactory;
import za.co.neroland.nerospace.world.OxygenManager;

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
        NeoForgeAttachments.register(modEventBus);
        NeoForgeNetwork.register(modEventBus);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            NeoForgeClientSetup.init(modEventBus);
        }

        // Oxygen survival: tick each player on the game bus (airless-planet drain / suffocation).
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post event) -> {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                OxygenManager.tick(serverPlayer);
            }
        });
        // Creative-tab contents are defined once by the cross-loader ModCreativeTab (a dedicated
        // Nerospace tab registered via the vanilla CREATIVE_MODE_TAB registry), so no NeoForge-specific
        // BuildCreativeModeTabContentsEvent injection is needed.
        modEventBus.addListener(this::onCreateEntityAttributes);
        modEventBus.addListener(this::onRegisterSpawnPlacements);
    }

    private void onCreateEntityAttributes(EntityAttributeCreationEvent event) {
        ModEntityAttributes.forEach((type, builder) -> event.put(type, builder.build()));
    }

    private void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        ModSpawnPlacements.registerAll(new ModSpawnPlacements.Sink() {
            @Override
            public <T extends Mob> void register(EntityType<T> type, SpawnPlacementType placementType,
                    Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate) {
                event.register(type, placementType, heightmap, predicate,
                        RegisterSpawnPlacementsEvent.Operation.REPLACE);
            }
        });
    }
}
