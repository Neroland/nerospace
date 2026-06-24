package za.co.neroland.nerospace.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.command.NerospaceCommands;
import za.co.neroland.nerospace.gear.AlienGearAbilities;
import za.co.neroland.nerospace.meteor.MeteorEvents;
import za.co.neroland.nerospace.telemetry.NerospaceTelemetry;
import za.co.neroland.nerospace.platform.NeoForgeFluidFactory;
import za.co.neroland.nerospace.world.OxygenFieldEvents;
import za.co.neroland.nerospace.registry.ModEntityAttributes;
import za.co.neroland.nerospace.registry.ModSpawnPlacements;
import za.co.neroland.nerospace.registry.NeoForgeRegistrationFactory;
import za.co.neroland.nerospace.progression.StarGuideGrants;
import za.co.neroland.nerospace.world.OxygenManager;
import za.co.neroland.nerospace.world.PlayerJoinHandler;
import za.co.neroland.nerospace.world.TerraformDrift;
import za.co.neroland.nerospace.world.TerraformManager;

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
        // Anonymous, Nerospace-only crash reporting (opt-out via config/nerospace.properties; off in dev).
        NerospaceTelemetry.init();
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
                StarGuideGrants.tick(serverPlayer);
            }
        });
        // Natural meteor showers + oxygen-field diffusion + terraform drift: tick the per-level drivers once per server tick.
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            MeteorEvents.tick(event.getServer());
            OxygenFieldEvents.tick(event.getServer());
            TerraformDrift.tick(event.getServer());
        });
        // Artificer gear: Grav Striders cushion the wearer — negate fall damage while carried.
        NeoForge.EVENT_BUS.addListener((LivingFallEvent event) -> {
            if (AlienGearAbilities.negatesFall(event.getEntity())) {
                event.setDamageMultiplier(0.0F);
            }
        });
        // One-time welcome on join (Star Guide pointer + repo link).
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                PlayerJoinHandler.onPlayerJoin(serverPlayer);
            }
        });
        // Creative debug commands (/nerospace gallery) — game-bus command registration.
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                NerospaceCommands.register(event.getDispatcher()));
        // Terraform catch-up: convert any in-range columns on chunks that load after the frontier passed.
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Load event) -> {
            if (event.getLevel() instanceof ServerLevel serverLevel && event.getChunk() instanceof LevelChunk chunk) {
                TerraformManager.get(serverLevel).onChunkLoaded(serverLevel, chunk);
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
