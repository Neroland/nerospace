package za.co.neroland.nerospace.forge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.command.NerospaceCommands;
import za.co.neroland.nerospace.gear.AlienGearAbilities;
import za.co.neroland.nerospace.meteor.MeteorEvents;
import za.co.neroland.nerospace.platform.ForgeFluidFactory;
import za.co.neroland.nerospace.progression.StarGuideGrants;
import za.co.neroland.nerospace.registry.ForgeRegistrationFactory;
import za.co.neroland.nerospace.registry.ModEntityAttributes;
import za.co.neroland.nerospace.registry.ModSpawnPlacements;
import za.co.neroland.nerospace.telemetry.NerospaceTelemetry;
import za.co.neroland.nerospace.world.OxygenFieldEvents;
import za.co.neroland.nerospace.world.OxygenManager;
import za.co.neroland.nerospace.world.PlayerJoinHandler;
import za.co.neroland.nerospace.world.TerraformDrift;
import za.co.neroland.nerospace.world.TerraformManager;
import za.co.neroland.nerospace.world.gravity.GravityManager;

/** MinecraftForge entry point for the Forge Stonecutter branch. */
@Mod(NerospaceCommon.MOD_ID)
public final class NerospaceForge {

    public NerospaceForge(FMLJavaModLoadingContext context) {
        NerospaceCommon.LOGGER.info("[Nerospace] Forge bootstrap");
        BusGroup modBusGroup = context.getModBusGroup();

        NerospaceCommon.init();
        NerospaceTelemetry.init();
        ForgeFluidFactory.registerFluidTypes(modBusGroup);
        ForgeRegistrationFactory.registerAll(modBusGroup);
        ForgeStorageAliases.register();
        ForgeCapabilities.register();
        ForgeAttachments.register();
        ForgeNetwork.register();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClientSetup.init(modBusGroup);
        }

        TickEvent.PlayerTickEvent.Post.BUS.addListener(event -> {
            if (event.player() instanceof ServerPlayer serverPlayer) {
                OxygenManager.tick(serverPlayer);
                StarGuideGrants.tick(serverPlayer);
            }
        });
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> {
            MeteorEvents.tick(event.server());
            OxygenFieldEvents.tick(event.server());
            TerraformDrift.tick(event.server());
            GravityManager.tick(event.server());
        });
        LivingFallEvent.BUS.addListener(event -> {
            if (AlienGearAbilities.negatesFall(event.getEntity())) {
                event.setDamageMultiplier(0.0F);
            }
        });
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                PlayerJoinHandler.onPlayerJoin(serverPlayer);
            }
        });
        RegisterCommandsEvent.BUS.addListener(event ->
                NerospaceCommands.register(event.getDispatcher()));
        ChunkEvent.Load.BUS.addListener(event -> {
            if (event.getLevel() instanceof ServerLevel serverLevel && event.getChunk() instanceof LevelChunk chunk) {
                TerraformManager.get(serverLevel).onChunkLoaded(serverLevel, chunk);
            }
        });
        EntityAttributeCreationEvent.BUS.addListener(this::onCreateEntityAttributes);
        SpawnPlacementRegisterEvent.BUS.addListener(this::onRegisterSpawnPlacements);
    }

    private void onCreateEntityAttributes(EntityAttributeCreationEvent event) {
        ModEntityAttributes.forEach((type, builder) -> event.put(type, builder.build()));
    }

    private void onRegisterSpawnPlacements(SpawnPlacementRegisterEvent event) {
        ModSpawnPlacements.registerAll(new ModSpawnPlacements.Sink() {
            @Override
            public <T extends Mob> void register(EntityType<T> type, SpawnPlacementType placementType,
                    Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate) {
                event.register(type, placementType, heightmap, predicate,
                        SpawnPlacementRegisterEvent.Operation.REPLACE);
            }
        });
    }
}
