package za.co.neroland.nerospace.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * en_us translations. Generated into {@code src/generated/resources}; there must be no
 * hand-written {@code en_us.json} in {@code src/main/resources} or Gradle will report a duplicate
 * resource.
 */
public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output) {
        super(output, Nerospace.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // Creative tab.
        add("itemGroup.nerospace", "Nerospace");

        // Blocks.
        add(ModBlocks.NEROSIUM_ORE.get(), "Nerosium Ore");
        add(ModBlocks.DEEPSLATE_NEROSIUM_ORE.get(), "Deepslate Nerosium Ore");
        add(ModBlocks.NEROSIUM_BLOCK.get(), "Block of Nerosium");
        add(ModBlocks.RAW_NEROSIUM_BLOCK.get(), "Block of Raw Nerosium");
        add(ModBlocks.NEROSIUM_GRINDER.get(), "Nerosium Grinder");

        // Phase 3 blocks.
        add(ModBlocks.NEROSTEEL_ORE.get(), "Nerosteel Ore");
        add(ModBlocks.XERTZ_QUARTZ_ORE.get(), "Xertz Quartz Ore");
        add(ModBlocks.NEROSTEEL_BLOCK.get(), "Block of Nerosteel");

        // Phase 4 blocks.
        add(ModBlocks.ROCKET_LAUNCH_PAD.get(), "Rocket Launch Pad");

        // Phase 8a blocks.
        add(ModBlocks.FUEL_TANK.get(), "Fuel Tank");

        // Phase 8c blocks.
        add(ModBlocks.OXYGEN_GENERATOR.get(), "Oxygen Generator");

        // Terraform design — terraformer.
        add(ModBlocks.TERRAFORMER.get(), "Terraformer");

        // Power grid.
        add(ModBlocks.UNIVERSAL_PIPE.get(), "Universal Pipe");
        add(ModBlocks.COMBUSTION_GENERATOR.get(), "Combustion Generator");
        add(ModBlocks.PASSIVE_GENERATOR.get(), "Passive Generator");
        add(ModItems.CONFIGURATOR.get(), "Configurator");
        add("block.nerospace.universal_pipe.energy", "Pipe energy: %s FE");
        add("block.nerospace.universal_pipe.fluid", "Pipe fluid: %s mB of %s");
        add("block.nerospace.universal_pipe.gas", "Pipe gas: %s mB of %s");
        add("block.nerospace.universal_pipe.items", "Items in transit: %s");
        // Gases.
        add("gas.nerospace.empty", "Empty");
        add("gas.nerospace.oxygen", "Oxygen");
        // Storage endpoints + creative sources.
        add(ModBlocks.BATTERY.get(), "Battery");
        add(ModBlocks.CREATIVE_BATTERY.get(), "Creative Battery");
        add(ModBlocks.FLUID_TANK.get(), "Fluid Tank");
        add(ModBlocks.CREATIVE_FLUID_TANK.get(), "Creative Fluid Tank");
        add(ModBlocks.GAS_TANK.get(), "Gas Tank");
        add(ModBlocks.CREATIVE_GAS_TANK.get(), "Creative Gas Tank");
        add(ModBlocks.ITEM_STORE.get(), "Item Store");
        add(ModBlocks.CREATIVE_ITEM_STORE.get(), "Creative Item Store");
        add("container.nerospace.item_store", "Item Store");
        add("block.nerospace.battery.readout", "Battery: %s / %s FE");
        add("block.nerospace.creative_battery.readout", "Creative Battery: endless energy");
        add("block.nerospace.tank.empty", "Tank: empty");
        add("block.nerospace.tank.readout", "Tank: %s / %s mB of %s");
        add("block.nerospace.creative_tank.set", "Endless source set: %s");
        add("block.nerospace.creative_tank.cleared", "Endless source cleared");
        add("block.nerospace.creative_tank.unset", "Right-click with a filled bucket to set the endless fluid");
        add("block.nerospace.creative_tank.readout", "Endless source: %s");
        add("block.nerospace.creative_store.set", "Endless source set: %s");
        add("block.nerospace.creative_store.cleared", "Endless source cleared");
        add("block.nerospace.creative_store.unset", "Right-click holding an item to set the endless item");
        add("block.nerospace.creative_store.readout", "Endless source: %s");
        add("item.nerospace.configurator.face", "%1$s — %2$s face: %3$s");
        add("item.nerospace.configurator.selected", "Configuring: %s");
        // Pipe resource layers.
        add("pipe.nerospace.type.energy", "Energy");
        add("pipe.nerospace.type.fluid", "Fluid");
        add("pipe.nerospace.type.gas", "Gas");
        add("pipe.nerospace.type.item", "Items");
        // Pipe face I/O modes.
        add("pipe.nerospace.mode.auto", "Auto");
        add("pipe.nerospace.mode.in", "In");
        add("pipe.nerospace.mode.out", "Out");
        add("pipe.nerospace.mode.off", "Off");
        // Generator GUIs.
        add("container.nerospace.combustion_generator", "Combustion Generator");
        add("container.nerospace.passive_generator", "Passive Generator");
        add("gui.nerospace.generator.output", "Power: %s%%");
        add("gui.nerospace.generator.burning", "Generating");
        add("gui.nerospace.generator.idle", "Idle");
        add("gui.nerospace.generator.core_active", "Core active");
        add("gui.nerospace.generator.core_empty", "No core");

        // Phase 7 blocks.
        add(ModBlocks.CINDRITE_ORE.get(), "Cindrite Ore");
        add(ModBlocks.CINDRITE_BLOCK.get(), "Block of Cindrite");
        add(ModBlocks.STATION_FLOOR.get(), "Station Floor");
        add(ModBlocks.STATION_WALL.get(), "Station Wall");

        // Items.
        add(ModItems.RAW_NEROSIUM.get(), "Raw Nerosium");
        add(ModItems.NEROSIUM_INGOT.get(), "Nerosium Ingot");
        add(ModItems.NEROSIUM_DUST.get(), "Nerosium Dust");
        add(ModItems.NEROSIUM_PICKAXE.get(), "Nerosium Pickaxe");

        // Phase 3 items.
        add(ModItems.RAW_NEROSTEEL.get(), "Raw Nerosteel");
        add(ModItems.NEROSTEEL_INGOT.get(), "Nerosteel Ingot");
        add(ModItems.XERTZ_QUARTZ.get(), "Xertz Quartz");
        add(ModItems.GREENXERTZ_NAVIGATOR.get(), "Greenxertz Navigator");

        // Phase 4 items.
        add(ModItems.ROCKET_FUEL_CANISTER.get(), "Rocket Fuel Canister");
        add(ModItems.ROCKET_TIER_1.get(), "Tier 1 Rocket");
        add(ModItems.ROCKET_TIER_2.get(), "Tier 2 Rocket");
        add(ModItems.ROCKET_TIER_3.get(), "Tier 3 Rocket");

        // Phase 8d — oxygen suit.
        add(ModItems.OXYGEN_SUIT_HELMET.get(), "Oxygen Suit Helmet");
        add(ModItems.OXYGEN_SUIT_CHESTPLATE.get(), "Oxygen Suit Chestplate");
        add(ModItems.OXYGEN_SUIT_LEGGINGS.get(), "Oxygen Suit Leggings");
        add(ModItems.OXYGEN_SUIT_BOOTS.get(), "Oxygen Suit Boots");

        // Phase 7 items.
        add(ModItems.CINDRITE.get(), "Cindrite");
        add(ModItems.ROCKET_FUEL_BUCKET.get(), "Rocket Fuel Bucket");
        add(ModItems.STATION_COMPASS.get(), "Station Compass");
        add(ModItems.GREENXERTZ_COMPASS.get(), "Greenxertz Compass");
        add(ModItems.CINDARA_COMPASS.get(), "Cindara Compass");
        add("item.nerospace.destination_compass.travel", "Travelling to %s");

        // Phase 7b fluid.
        add(ModBlocks.ROCKET_FUEL_BLOCK.get(), "Rocket Fuel");
        add("fluid_type.nerospace.rocket_fuel", "Rocket Fuel");

        // Spawn eggs.
        add(ModItems.XERTZ_STALKER_SPAWN_EGG.get(), "Xertz Stalker Spawn Egg");
        add(ModItems.QUARTZ_CRAWLER_SPAWN_EGG.get(), "Quartz Crawler Spawn Egg");
        add(ModItems.GREENLING_SPAWN_EGG.get(), "Greenling Spawn Egg");
        add(ModItems.CINDER_STALKER_SPAWN_EGG.get(), "Cinder Stalker Spawn Egg");

        // Entities.
        add("entity.nerospace.rocket", "Rocket");
        add("entity.nerospace.xertz_stalker", "Xertz Stalker");
        add("entity.nerospace.quartz_crawler", "Quartz Crawler");
        add("entity.nerospace.greenling", "Greenling");
        add("entity.nerospace.cinder_stalker", "Cinder Stalker");

        // Join / work-in-progress notice.
        add("message.nerospace.welcome.wip",
                "Nerospace is an early work in progress — features, balance, and art may all still change.");
        add("message.nerospace.welcome.link", "View the mod or report issues: ");

        // Planet atmosphere.
        add("message.nerospace.greenxertz.no_air", "You are out of oxygen — reach a launch pad or an Oxygen Generator!");

        // Fuel tank status (action-bar readout).
        add("block.nerospace.fuel_tank.status", "Fuel Tank: %s / %s mB");

        // Oxygen generator GUI.
        add("container.nerospace.oxygen_generator", "Oxygen Generator");
        add("gui.nerospace.oxygen_generator.power", "Power: %s%%");
        add("gui.nerospace.oxygen_generator.burning", "Burning");
        add("gui.nerospace.oxygen_generator.idle", "Idle");

        // Fuel tank GUI.
        add("container.nerospace.fuel_tank", "Fuel Tank");
        add("gui.nerospace.fuel_tank.level", "Fuel: %s%% (%s / %s mB)");

        // Terraformer GUI.
        add("container.nerospace.terraformer", "Terraformer");
        add("gui.nerospace.terraformer.power", "Power: %s%%");
        add("gui.nerospace.terraformer.tier", "Tier %s");
        add("gui.nerospace.terraformer.radius", "Radius: %s");
        add("gui.nerospace.terraformer.working", "Terraforming");
        add("gui.nerospace.terraformer.idle", "Idle");

        // Containers / GUI.
        add("container.nerospace.nerosium_grinder", "Nerosium Grinder");
        add("container.nerospace.rocket", "Rocket");
        add("gui.nerospace.rocket.launch", "Launch");
        add("gui.nerospace.rocket.pad", "Pad");
        add("gui.nerospace.rocket.tier", "Tier %s Rocket");
        add("gui.nerospace.rocket.fuel", "Fuel: %s / %s mB");
        add("gui.nerospace.rocket.fuel_pct", "Fuel: %s%% (%s / %s mB)");
        add("gui.nerospace.rocket.ready", "Ready for launch");
        add("gui.nerospace.rocket.not_ready", "Not ready: fuel up & board");

        // Rocket action feedback.
        add("item.nerospace.rocket.deployed", "Rocket deployed on the launch pad");
        add("entity.nerospace.rocket.arrived", "You have arrived on the planet");
        add("entity.nerospace.rocket.docked", "Docked at the Orbital Station");

        // Greenxertz Navigator action feedback.
        add("item.nerospace.greenxertz_navigator.travel", "Transported to Greenxertz");
        add("item.nerospace.greenxertz_navigator.return", "Returned to the overworld");
    }
}
