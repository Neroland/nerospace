package za.co.neroland.nerospace.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.command.NerospaceCommands;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.gear.AlienGearAbilities;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.meteor.MeteorEvents;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.world.OxygenFieldEvents;
import za.co.neroland.nerospace.world.TerraformDrift;
import za.co.neroland.nerospace.world.TerraformManager;
import za.co.neroland.nerospace.registry.ModEntityAttributes;
import za.co.neroland.nerospace.registry.ModSpawnPlacements;
import za.co.neroland.nerospace.telemetry.NerospaceTelemetry;
import za.co.neroland.nerospace.progression.StarGuideGrants;
import za.co.neroland.nerospace.world.OxygenManager;

/**
 * Fabric entry point. Shared init registers content eagerly, then Fabric-side
 * wiring: creative-tab fill (Fabric API creative-tab module) and biome injection
 * of the ore placed-features (Fabric API biome module — the counterpart to the
 * NeoForge {@code biome_modifier} JSON).
 */
public final class NerospaceFabric implements ModInitializer {

    /** Mod-owned energy lookup; mirrors the NeoForge energy BlockCapability of the same id. */
    public static final BlockApiLookup<NerospaceEnergyStorage, Direction> ENERGY =
            BlockApiLookup.get(
                    Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "energy"),
                    NerospaceEnergyStorage.class, Direction.class);

    /** Mod-owned fluid lookup; mirrors the NeoForge fluid BlockCapability of the same id. */
    public static final BlockApiLookup<NerospaceFluidStorage, Direction> FLUID =
            BlockApiLookup.get(
                    Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "fluid"),
                    NerospaceFluidStorage.class, Direction.class);

    /** Mod-owned gas lookup; mirrors the NeoForge gas BlockCapability of the same id. */
    public static final BlockApiLookup<NerospaceGasStorage, Direction> GAS =
            BlockApiLookup.get(
                    Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "gas"),
                    NerospaceGasStorage.class, Direction.class);

    @Override
    public void onInitialize() {
        NerospaceCommon.LOGGER.info("[Nerospace] Fabric bootstrap");
        NerospaceCommon.init();
        // Anonymous, Nerospace-only crash reporting (opt-out via config/nerospace.properties; off in dev).
        NerospaceTelemetry.init();

        // Creative-tab contents are defined once by the cross-loader ModCreativeTab (a dedicated
        // Nerospace tab), so no per-loader creative-tab injection is needed here.

        addOverworldOre("nerosium_ore_placed");

        // Default attributes for the ported mobs (counterpart to NeoForge's EntityAttributeCreationEvent).
        ModEntityAttributes.forEach(FabricDefaultAttributeRegistry::register);

        // Natural-spawn placement rules (counterpart to NeoForge's RegisterSpawnPlacementsEvent).
        ModSpawnPlacements.registerAll(new ModSpawnPlacements.Sink() {
            @Override
            public <T extends Mob> void register(EntityType<T> type, SpawnPlacementType placementType,
                    Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate) {
                SpawnPlacements.register(type, placementType, heightmap, predicate);
            }
        });

        // Oxygen survival: register the attachment + tick each player per world tick (airless-planet drain).
        FabricAttachments.init();
        FabricNetwork.registerCommon();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            server.getPlayerList().getPlayers().forEach(player -> {
                OxygenManager.tick(player);
                StarGuideGrants.tick(player);
            });
            MeteorEvents.tick(server);
            OxygenFieldEvents.tick(server);
            TerraformDrift.tick(server);
        });
        // Creative debug commands (/nerospace gallery).
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                NerospaceCommands.register(dispatcher));
        // Terraform catch-up: convert any in-range columns on chunks that load after the frontier passed.
        // (Fabric's Load SAM passes a third "newly generated" flag, which we don't need.)
        ServerChunkEvents.CHUNK_LOAD.register((serverLevel, chunk, newlyGenerated) ->
                TerraformManager.get(serverLevel).onChunkLoaded(serverLevel, chunk));
        // Artificer gear: Grav Striders cushion the wearer — cancel fall damage while carried
        // (counterpart to NeoForge's LivingFallEvent; returning false vetoes the damage).
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(source.is(DamageTypes.FALL) && AlienGearAbilities.negatesFall(entity)));

        // Item-storage capability (Fabric Transfer API) — counterpart to NeoForge
        // Capabilities.Item.BLOCK; lets mod pipes move items in/out of the item store.
        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.ITEM_STORE.get());

        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.BATTERY.get());

        FLUID.registerForBlockEntity(
                (be, direction) -> be.getTank(),
                ModBlockEntities.FLUID_TANK.get());

        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.COMBUSTION_GENERATOR.get());
        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.COMBUSTION_GENERATOR.get());

        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.NEROSIUM_GRINDER.get());
        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.NEROSIUM_GRINDER.get());

        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.PASSIVE_GENERATOR.get());
        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.PASSIVE_GENERATOR.get());

        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.UNIVERSAL_PIPE.get());
        GAS.registerForBlockEntity(
                (be, direction) -> be.getGas(),
                ModBlockEntities.UNIVERSAL_PIPE.get());
        FLUID.registerForBlockEntity(
                (be, direction) -> be.getFluidTank(),
                ModBlockEntities.UNIVERSAL_PIPE.get());
        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.UNIVERSAL_PIPE.get());

        GAS.registerForBlockEntity(
                (be, direction) -> be.getTank(),
                ModBlockEntities.GAS_TANK.get());

        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.OXYGEN_GENERATOR.get());
        GAS.registerForBlockEntity(
                (be, direction) -> be.getGas(),
                ModBlockEntities.OXYGEN_GENERATOR.get());

        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.SOLAR_PANEL.get());

        // Terraformer: grid power in, upgrade slot in.
        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.TERRAFORMER.get());
        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.TERRAFORMER.get());

        // Hydration Module: glacite in (no energy of its own).
        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.HYDRATION_MODULE.get());

        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.TRASH_CAN.get());
        FLUID.registerForBlockEntity(
                (be, direction) -> be.getFluid(),
                ModBlockEntities.TRASH_CAN.get());

        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.CREATIVE_BATTERY.get());

        // Creative storage: endless sources/sinks for testing logistics.
        FLUID.registerForBlockEntity(
                (be, direction) -> be.getTank(),
                ModBlockEntities.CREATIVE_FLUID_TANK.get());
        GAS.registerForBlockEntity(
                (be, direction) -> be.getTank(),
                ModBlockEntities.CREATIVE_GAS_TANK.get());
        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.CREATIVE_ITEM_STORE.get());

        // Fuel Tank: fluid out (pipes), canister in (hoppers/pipes).
        FLUID.registerForBlockEntity(
                (be, direction) -> be.getTank(),
                ModBlockEntities.FUEL_TANK.get());
        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.FUEL_TANK.get());

        // Fuel Refinery: grid power in, refined fuel out, coal + blaze powder in.
        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.FUEL_REFINERY.get());
        FLUID.registerForBlockEntity(
                (be, direction) -> be.getTank(),
                ModBlockEntities.FUEL_REFINERY.get());
        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.FUEL_REFINERY.get());

        // Quarry controller: grid power in, mined output + sucked fluid out, frame casings in.
        ENERGY.registerForBlockEntity(
                (be, direction) -> be.getEnergy(),
                ModBlockEntities.QUARRY_CONTROLLER.get());
        FLUID.registerForBlockEntity(
                (be, direction) -> be.getTank(),
                ModBlockEntities.QUARRY_CONTROLLER.get());
        ItemStorage.SIDED.registerForBlockEntity(
                (be, direction) -> ContainerStorage.of(be, direction),
                ModBlockEntities.QUARRY_CONTROLLER.get());
    }

    private static void addOverworldOre(String placedFeatureName) {
        ResourceKey<PlacedFeature> key = ResourceKey.create(
                Registries.PLACED_FEATURE,
                Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, placedFeatureName));
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                key);
    }
}
