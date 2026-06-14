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
        add(ModBlocks.FUEL_REFINERY.get(), "Fuel Refinery");

        // Phase 8c blocks.
        add(ModBlocks.OXYGEN_GENERATOR.get(), "Oxygen Generator");

        // Terraform design — terraformer.
        add(ModBlocks.TERRAFORMER.get(), "Terraformer");

        // Deeper terraforming (DEEPER_TERRAFORM_DESIGN.md) — hydration module + monitor.
        add(ModBlocks.HYDRATION_MODULE.get(), "Hydration Module");
        add(ModBlocks.TERRAFORM_MONITOR.get(), "Terraform Monitor");

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
        add(ModBlocks.TRASH_CAN.get(), "Trash Can");
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
        // Pipe filter + upgrades.
        add(ModItems.PIPE_FILTER.get(), "Pipe Filter");
        add(ModItems.SPEED_UPGRADE.get(), "Speed Upgrade");
        add(ModItems.CAPACITY_UPGRADE.get(), "Capacity Upgrade");
        add("item.nerospace.pipe_filter.set", "Filter set: %s");
        add("item.nerospace.pipe_filter.cleared", "Filter cleared");
        add("item.nerospace.pipe_filter.applied", "Face filter applied: %s on the %s face");
        add("item.nerospace.pipe_filter.cleared_face", "Face filter removed from the %s face");
        add("item.nerospace.pipe_upgrade.installed", "%s installed (%s/%s)");
        add("item.nerospace.pipe_upgrade.full", "This pipe segment has no room for that upgrade");
        add("block.nerospace.universal_pipe.upgrades_removed", "Upgrades popped out");
        add("block.nerospace.universal_pipe.no_upgrades", "No upgrades installed");
        // Configurator panel.
        add("screen.nerospace.pipe_config", "Pipe Configuration");
        add("pipe.nerospace.face.down", "Bottom");
        add("pipe.nerospace.face.up", "Top");
        add("pipe.nerospace.face.north", "North");
        add("pipe.nerospace.face.south", "South");
        add("pipe.nerospace.face.west", "West");
        add("pipe.nerospace.face.east", "East");
        // Generator GUIs.
        add("container.nerospace.combustion_generator", "Combustion Generator");
        add("container.nerospace.passive_generator", "Passive Generator");
        add("gui.nerospace.generator.output", "Power: %s%%");
        add("gui.nerospace.generator.burning", "Generating");
        add("gui.nerospace.generator.idle", "Idle");
        add("gui.nerospace.generator.core_active", "Core active");
        add("gui.nerospace.generator.core_empty", "No core");

        // Quarry / Miner (MINER_DESIGN).
        add(ModBlocks.QUARRY_CONTROLLER.get(), "Quarry Controller");
        add(ModBlocks.QUARRY_LANDMARK.get(), "Quarry Landmark");
        add(ModBlocks.QUARRY_FRAME.get(), "Quarry Frame");
        add(ModItems.FRAME_CASING.get(), "Frame Casing");
        add(ModItems.SPEED_MODULE.get(), "Speed Module");
        add(ModItems.EFFICIENCY_MODULE.get(), "Efficiency Module");
        add(ModItems.FORTUNE_MODULE.get(), "Fortune Module");
        add(ModItems.SILK_TOUCH_MODULE.get(), "Silk Touch Module");
        add("container.nerospace.quarry_controller", "Quarry Controller");
        add("gui.nerospace.quarry.state.idle", "Idle — place landmarks");
        add("gui.nerospace.quarry.state.building_frame", "Building frame");
        add("gui.nerospace.quarry.state.mining", "Mining");
        add("gui.nerospace.quarry.state.done", "Finished");
        add("gui.nerospace.quarry.state.paused", "Paused");

        // Phase 7 blocks.
        add(ModBlocks.CINDRITE_ORE.get(), "Cindrite Ore");
        add(ModBlocks.CINDRITE_BLOCK.get(), "Block of Cindrite");

        // Glacira blocks (NEW_DESTINATION_DESIGN.md).
        add(ModBlocks.GLACITE_ORE.get(), "Glacite Ore");
        add(ModBlocks.GLACITE_BLOCK.get(), "Block of Glacite");
        add(ModBlocks.STATION_FLOOR.get(), "Station Floor");
        add(ModBlocks.STATION_WALL.get(), "Station Wall");

        // Developer diagnostics — Sentry test block (hidden; /give nerospace:sentry_test).
        add(ModBlocks.SENTRY_TEST.get(), "Sentry Test Block");
        add("message.nerospace.sentry_test.sent",
                "Sentry test event dispatched — check your Sentry dashboard.");
        add("message.nerospace.sentry_test.disabled",
                "Telemetry is disabled (telemetryEnabled=false) — nothing sent.");

        // Multiple stations (MULTI_STATION_DESIGN.md).
        add(ModBlocks.STATION_CORE.get(), "Station Core");
        add(ModItems.STATION_CHARTER.get(), "Station Charter");
        add("block.nerospace.station_core.bound", "Station Core: %s");
        add("block.nerospace.station_core.unbound", "Station Core: not bound to a station");
        add("entity.nerospace.rocket.founded", "Station founded: %s");
        add("entity.nerospace.rocket.station_arrived", "Docked at %s");
        add("entity.nerospace.rocket.station_missing",
                "That station is no longer registered — trajectory reset");
        add("entity.nerospace.rocket.no_charter",
                "Founding a station needs a Station Charter in your inventory");
        add("entity.nerospace.rocket.stations_full",
                "The orbital lanes are full — no new station slots available");
        add("gui.nerospace.rocket.found", "FOUND");
        add("gui.nerospace.rocket.stations_none", "NO STATIONS");
        add("gui.nerospace.rocket.station_node", "STN: %s");

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
        add(ModItems.ROCKET_TIER_4.get(), "Tier 4 Rocket");

        // Phase 8d — oxygen suit.
        add(ModItems.OXYGEN_SUIT_HELMET.get(), "Oxygen Suit Helmet");
        add(ModItems.OXYGEN_SUIT_CHESTPLATE.get(), "Oxygen Suit Chestplate");
        add(ModItems.OXYGEN_SUIT_LEGGINGS.get(), "Oxygen Suit Leggings");
        add(ModItems.OXYGEN_SUIT_BOOTS.get(), "Oxygen Suit Boots");

        // Suit-and-station integration — Tier 2 (cindrite-upgraded) oxygen suit.
        add(ModItems.OXYGEN_SUIT_T2_HELMET.get(), "Tier 2 Oxygen Suit Helmet");
        add(ModItems.OXYGEN_SUIT_T2_CHESTPLATE.get(), "Tier 2 Oxygen Suit Chestplate");
        add(ModItems.OXYGEN_SUIT_T2_LEGGINGS.get(), "Tier 2 Oxygen Suit Leggings");
        add(ModItems.OXYGEN_SUIT_T2_BOOTS.get(), "Tier 2 Oxygen Suit Boots");

        // Hazard suit variants (SUIT_HAZARD_DESIGN.md).
        add(ModItems.OXYGEN_SUIT_HEAT_HELMET.get(), "Thermal Suit Helmet");
        add(ModItems.OXYGEN_SUIT_HEAT_CHESTPLATE.get(), "Thermal Suit Chestplate");
        add(ModItems.OXYGEN_SUIT_HEAT_LEGGINGS.get(), "Thermal Suit Leggings");
        add(ModItems.OXYGEN_SUIT_HEAT_BOOTS.get(), "Thermal Suit Boots");
        add(ModItems.OXYGEN_SUIT_COLD_HELMET.get(), "Cryo Suit Helmet");
        add(ModItems.OXYGEN_SUIT_COLD_CHESTPLATE.get(), "Cryo Suit Chestplate");
        add(ModItems.OXYGEN_SUIT_COLD_LEGGINGS.get(), "Cryo Suit Leggings");
        add(ModItems.OXYGEN_SUIT_COLD_BOOTS.get(), "Cryo Suit Boots");

        // Phase 7 items.
        add(ModItems.CINDRITE.get(), "Cindrite");
        add(ModItems.GLACITE.get(), "Glacite");
        add(ModItems.ROCKET_FUEL_BUCKET.get(), "Rocket Fuel Bucket");
        add(ModItems.STATION_COMPASS.get(), "Station Compass");
        add(ModItems.GREENXERTZ_COMPASS.get(), "Greenxertz Compass");
        add(ModItems.CINDARA_COMPASS.get(), "Cindara Compass");
        add(ModItems.GLACIRA_COMPASS.get(), "Glacira Compass");
        add("item.nerospace.destination_compass.travel", "Travelling to %s");

        // Phase 7b fluid.
        add(ModBlocks.ROCKET_FUEL_BLOCK.get(), "Rocket Fuel");
        add("fluid_type.nerospace.rocket_fuel", "Rocket Fuel");

        // Spawn eggs.
        add(ModItems.XERTZ_STALKER_SPAWN_EGG.get(), "Xertz Stalker Spawn Egg");
        add(ModItems.QUARTZ_CRAWLER_SPAWN_EGG.get(), "Quartz Crawler Spawn Egg");
        add(ModItems.GREENLING_SPAWN_EGG.get(), "Greenling Spawn Egg");
        add(ModItems.CINDER_STALKER_SPAWN_EGG.get(), "Cinder Stalker Spawn Egg");
        add(ModItems.FROST_STRIDER_SPAWN_EGG.get(), "Frost Strider Spawn Egg");
        add(ModItems.MEADOW_LOPER_SPAWN_EGG.get(), "Meadow Loper Spawn Egg");
        add(ModItems.EMBER_STRUTTER_SPAWN_EGG.get(), "Ember Strutter Spawn Egg");
        add(ModItems.WOOLLY_DRIFT_SPAWN_EGG.get(), "Woolly Drift Spawn Egg");

        // Terraform livestock drops (DEEPER_TERRAFORM_DESIGN.md §5).
        add(ModItems.LOPER_HAUNCH.get(), "Loper Haunch");
        add(ModItems.STRUTTER_DRUMSTICK.get(), "Strutter Drumstick");
        add(ModItems.DRIFT_FLEECE.get(), "Drift Fleece");

        // Entities.
        add("entity.nerospace.rocket", "Rocket");
        add("entity.nerospace.xertz_stalker", "Xertz Stalker");
        add("entity.nerospace.quartz_crawler", "Quartz Crawler");
        add("entity.nerospace.greenling", "Greenling");
        add("entity.nerospace.cinder_stalker", "Cinder Stalker");
        add("entity.nerospace.frost_strider", "Frost Strider");
        add("entity.nerospace.meadow_loper", "Meadow Loper");
        add("entity.nerospace.ember_strutter", "Ember Strutter");
        add("entity.nerospace.woolly_drift", "Woolly Drift");

        // Creature sound subtitles (Phase 10 ambience). The sounds.json placeholders alias vanilla
        // audio for now; these subtitles describe the creature, so they stay correct when real audio
        // is dropped in later.
        add("subtitles.nerospace.xertz_stalker.ambient", "Xertz Stalker chitters");
        add("subtitles.nerospace.xertz_stalker.hurt", "Xertz Stalker shrieks");
        add("subtitles.nerospace.xertz_stalker.death", "Xertz Stalker shatters");
        add("subtitles.nerospace.quartz_crawler.ambient", "Quartz Crawler skitters");
        add("subtitles.nerospace.quartz_crawler.hurt", "Quartz Crawler cracks");
        add("subtitles.nerospace.quartz_crawler.death", "Quartz Crawler crumbles");
        add("subtitles.nerospace.greenling.ambient", "Greenling chirps");
        add("subtitles.nerospace.greenling.hurt", "Greenling squeaks");
        add("subtitles.nerospace.greenling.death", "Greenling wilts");
        add("subtitles.nerospace.cinder_stalker.ambient", "Cinder Stalker smoulders");
        add("subtitles.nerospace.cinder_stalker.hurt", "Cinder Stalker roars");
        add("subtitles.nerospace.cinder_stalker.death", "Cinder Stalker cools");
        add("subtitles.nerospace.frost_strider.ambient", "Frost Strider creaks");
        add("subtitles.nerospace.frost_strider.hurt", "Frost Strider splinters");
        add("subtitles.nerospace.frost_strider.death", "Frost Strider shatters");
        add("subtitles.nerospace.meadow_loper.ambient", "Meadow Loper lows");
        add("subtitles.nerospace.meadow_loper.hurt", "Meadow Loper bellows");
        add("subtitles.nerospace.meadow_loper.death", "Meadow Loper falls");
        add("subtitles.nerospace.ember_strutter.ambient", "Ember Strutter clucks");
        add("subtitles.nerospace.ember_strutter.hurt", "Ember Strutter squawks");
        add("subtitles.nerospace.ember_strutter.death", "Ember Strutter goes quiet");
        add("subtitles.nerospace.woolly_drift.ambient", "Woolly Drift bleats");
        add("subtitles.nerospace.woolly_drift.hurt", "Woolly Drift cries");
        add("subtitles.nerospace.woolly_drift.death", "Woolly Drift falls still");
        // Machine sounds (placeholder vanilla aliases in sounds.json, same pattern as the creatures).
        add("subtitles.nerospace.fuel_tank.pump", "Fuel Tank pumps");

        // Join notice.
        add("message.nerospace.welcome.intro",
                "Welcome! Craft a Star Guide Book (book + raw nerosium) to chart your journey to the stars.");
        add("message.nerospace.welcome.link", "Feedback and bug reports are welcome: ");

        // Planet atmosphere.
        add("message.nerospace.greenxertz.no_air", "You are out of oxygen — reach a launch pad or an Oxygen Generator!");

        // Fuel tank status (action-bar readout).
        add("block.nerospace.fuel_tank.status", "Fuel Tank: %s / %s mB");

        // Oxygen generator GUI.
        add("container.nerospace.oxygen_generator", "Oxygen Generator");
        add("gui.nerospace.oxygen_generator.power", "Power: %s%%");
        add("gui.nerospace.oxygen_generator.oxygen", "Oxygen: %s / %s mB");
        add("gui.nerospace.oxygen_generator.producing", "Producing oxygen");
        add("gui.nerospace.oxygen_generator.starved", "No power");

        // Fuel tank GUI.
        add("container.nerospace.fuel_tank", "Fuel Tank");
        add("container.nerospace.fuel_refinery", "Fuel Refinery");
        add("gui.nerospace.fuel_tank.level", "Fuel: %s%% (%s / %s mB)");

        // Terraformer GUI.
        add("container.nerospace.terraformer", "Terraformer");
        add("gui.nerospace.terraformer.power", "Power: %s%%");
        add("gui.nerospace.terraformer.tier", "Tier %s");
        add("gui.nerospace.terraformer.radius", "Radius: %s");
        add("gui.nerospace.terraformer.stages", "Radii: %s / %s / %s");
        add("gui.nerospace.terraformer.hydration", "Water: %s");
        add("gui.nerospace.terraformer.needs_glacite", "Needs glacite");
        add("gui.nerospace.terraformer.working", "Terraforming");
        add("gui.nerospace.terraformer.idle", "Idle");

        // Hydration Module GUI (DEEPER_TERRAFORM_DESIGN.md §3.1).
        add("container.nerospace.hydration_module", "Hydration Module");
        add("gui.nerospace.hydration_module.buffer", "Hydration: %s / %s");
        add("gui.nerospace.hydration_module.linked", "Feeding Terraformer");
        add("gui.nerospace.hydration_module.no_link", "No Terraformer touching");

        // Terraform Monitor GUI (DEEPER_TERRAFORM_DESIGN.md §6).
        add("container.nerospace.terraform_monitor", "Terraform Monitor");
        add("gui.nerospace.terraform_monitor.stage.0", "This ground: Dead");
        add("gui.nerospace.terraform_monitor.stage.1", "This ground: Rooted");
        add("gui.nerospace.terraform_monitor.stage.2", "This ground: Hydrated");
        add("gui.nerospace.terraform_monitor.stage.3", "This ground: Living");
        add("gui.nerospace.terraform_monitor.radii", "Radii: %s / %s / %s");
        add("gui.nerospace.terraform_monitor.hydration", "Water: %s");
        add("gui.nerospace.terraform_monitor.no_link", "No Terraformer within 32 blocks");

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

        // Star Guide (progression block, 1.0).
        add(ModBlocks.STAR_GUIDE.get(), "Star Guide");
        add(ModItems.STAR_GUIDE_BOOK.get(), "Star Guide Book");
        add("container.nerospace.star_guide", "Star Guide");
        add("message.nerospace.star_guide.empty", "Place a Star Guide Book on the pedestal to open the guide");
        add("gui.nerospace.star_guide.complete", "COMPLETE");

        add("gui.nerospace.star_guide.chapter.nerosium", "Nerosium");
        add("gui.nerospace.star_guide.chapter.machines", "Machines");
        add("gui.nerospace.star_guide.chapter.power_grid", "Power Grid");
        add("gui.nerospace.star_guide.chapter.rocketry", "Rocketry");
        add("gui.nerospace.star_guide.chapter.new_worlds", "New Worlds");
        add("gui.nerospace.star_guide.chapter.vacuum", "Vacuum");
        add("gui.nerospace.star_guide.chapter.terraforming", "Terraforming");

        add("gui.nerospace.star_guide.step.raw_nerosium", "Strange Red Rock");
        add("gui.nerospace.star_guide.step.raw_nerosium.text",
                "Nerosium ore veins thread the overworld's stone. Mine them with an iron pickaxe "
                        + "or better — the raw red metal is the seed of everything that follows.");
        add("gui.nerospace.star_guide.step.nerosium_ingot", "First Smelt");
        add("gui.nerospace.star_guide.step.nerosium_ingot.text",
                "Smelt raw nerosium in any furnace. The ingot is your basic crafting metal for "
                        + "machines, rockets and tools.");
        add("gui.nerospace.star_guide.step.nerosium_pickaxe", "Tools of the Trade");
        add("gui.nerospace.star_guide.step.nerosium_pickaxe.text",
                "Nerosium tools sit at iron tier with better durability. You will be mining a lot "
                        + "— craft the pickaxe.");

        add("gui.nerospace.star_guide.step.nerosium_grinder", "Industrial Revolution");
        add("gui.nerospace.star_guide.step.nerosium_grinder.text",
                "The Nerosium Grinder doubles your ore yield by grinding raw nerosium into dust. "
                        + "It needs power from the grid — see the Power Grid chapter.");
        add("gui.nerospace.star_guide.step.nerosium_dust", "Finely Ground");
        add("gui.nerospace.star_guide.step.nerosium_dust.text",
                "Grind raw nerosium into dust, then smelt the dust into ingots — two for one.");
        add("gui.nerospace.star_guide.step.combustion_generator", "Burning Bright");
        add("gui.nerospace.star_guide.step.combustion_generator.text",
                "The Combustion Generator burns coal, charcoal or fuel canisters into energy. "
                        + "It is your first power source — pipe its output to your machines.");

        add("gui.nerospace.star_guide.step.universal_pipe", "Connect Everything");
        add("gui.nerospace.star_guide.step.universal_pipe.text",
                "One pipe carries everything: energy, fluids, gas and items. Pipes form networks "
                        + "that balance their contents and feed adjacent machines.");
        add("gui.nerospace.star_guide.step.battery", "Stored Potential");
        add("gui.nerospace.star_guide.step.battery.text",
                "Batteries buffer the grid: generators fill them, machines drain them through "
                        + "the pipe network. Build one before your first rocket launch window.");
        add("gui.nerospace.star_guide.step.passive_generator", "Slow and Steady");
        add("gui.nerospace.star_guide.step.passive_generator.text",
                "The Passive Generator trickles energy from a nerosium core for a long time — "
                        + "weaker than combustion but completely hands-off.");
        add("gui.nerospace.star_guide.step.configurator", "Fine Tuning");
        add("gui.nerospace.star_guide.step.configurator.text",
                "The Configurator sets per-face pipe modes (in / out / off) so you can route "
                        + "exactly what flows where.");

        add("gui.nerospace.star_guide.step.rocket_fuel_canister", "Highly Flammable");
        add("gui.nerospace.star_guide.step.rocket_fuel_canister.text",
                "Rockets burn rocket fuel. Craft canisters and fill them — or pump fuel straight "
                        + "into a pad-side rocket from a Fuel Tank.");
        add("gui.nerospace.star_guide.step.rocket_launch_pad", "Ground Control");
        add("gui.nerospace.star_guide.step.rocket_launch_pad.text",
                "A rocket deploys onto a full 3x3 of Launch Pad blocks. A Fuel Tank next to the "
                        + "pad auto-fuels the rocket — four times faster on the full 3x3.");
        add("gui.nerospace.star_guide.step.rocket_tier_1", "We Have Liftoff");
        add("gui.nerospace.star_guide.step.rocket_tier_1.text",
                "The Tier 1 rocket reaches the Orbital Station. Deploy it on the pad, board, "
                        + "fuel up and hit launch.");
        add("gui.nerospace.star_guide.step.station", "Orbital");
        add("gui.nerospace.star_guide.step.station.text",
                "The Orbital Station is your first destination — vacuum-cold and airless. Stay "
                        + "near the pad's safe zone until you have oxygen gear.");
        add("gui.nerospace.star_guide.step.station_charter", "Homestead in Orbit");
        add("gui.nerospace.star_guide.step.station_charter.text",
                "Craft a Station Charter (rename it in an anvil to name your station), pick the "
                        + "FOUND node in any rocket and launch. The Station Core anchors it — "
                        + "break the Core to unregister and reclaim the charter.");

        add("gui.nerospace.star_guide.step.rocket_tier_2", "Bigger Boosters");
        add("gui.nerospace.star_guide.step.rocket_tier_2.text",
                "Tier 2 adds Greenxertz to your destinations. It needs station materials, so "
                        + "establish yourself in orbit first.");
        add("gui.nerospace.star_guide.step.greenxertz", "A Whole New World");
        add("gui.nerospace.star_guide.step.greenxertz.text",
                "Greenxertz: a green-steel world of nerosteel and xertz quartz — and the "
                        + "creatures that guard them. The air is thin; bring a suit.");
        add("gui.nerospace.star_guide.step.nerosteel_ingot", "Alien Alloy");
        add("gui.nerospace.star_guide.step.nerosteel_ingot.text",
                "Nerosteel is Greenxertz's primary metal — mine the ore and smelt it. Higher-tier "
                        + "rockets and suits are built from it.");
        add("gui.nerospace.star_guide.step.rocket_tier_3", "To the Fire Moon");
        add("gui.nerospace.star_guide.step.rocket_tier_3.text",
                "Tier 3 reaches Cindara. Its launch pad must be ringed with Station Wall — the "
                        + "blast would melt anything less.");
        add("gui.nerospace.star_guide.step.cindara", "Into the Fire");
        add("gui.nerospace.star_guide.step.cindara.text",
                "Cindara is a volcanic moon: fire-immune stalkers, lava fields and cindrite. "
                        + "Heat-graded gear recommended.");
        add("gui.nerospace.star_guide.step.cindrite", "Heart of the Volcano");
        add("gui.nerospace.star_guide.step.cindrite.text",
                "Cindrite crystals upgrade your oxygen suit to Tier 2 and gate the deepest "
                        + "progression. Mine them from Cindara's stone.");
        add("gui.nerospace.star_guide.step.rocket_tier_4", "To the Ice Moon");
        add("gui.nerospace.star_guide.step.rocket_tier_4.text",
                "Tier 4 reaches Glacira, the frozen edge of the system. Built around a cindrite "
                        + "core, it launches only from a Heavy Launch Complex (5x5 pad + gantry).");
        add("gui.nerospace.star_guide.step.glacira", "Into the Cold");
        add("gui.nerospace.star_guide.step.glacira.text",
                "Glacira is an ice moon: pale frozen plains, stilt-legged striders and glacite. "
                        + "The last stop before you turn a world green.");
        add("gui.nerospace.star_guide.step.glacite", "Heart of the Glacier");
        add("gui.nerospace.star_guide.step.glacite.text",
                "Glacite is crystallised water-ice — the key to cold-weather suits and, one day, "
                        + "to giving a terraformed world its water. Mine it from Glacira's stone.");

        add("gui.nerospace.star_guide.step.oxygen_generator", "Something to Breathe");
        add("gui.nerospace.star_guide.step.oxygen_generator.text",
                "The Oxygen Generator electrolyses grid power into oxygen and pressurises the "
                        + "space around it. Sealed rooms fill completely; open air only holds a bubble.");
        add("gui.nerospace.star_guide.step.gas_tank", "Bottled Air");
        add("gui.nerospace.star_guide.step.gas_tank.text",
                "Gas Tanks store oxygen piped from a generator. A tank by your base door acts as "
                        + "an airlock — suits refill from it automatically.");
        add("gui.nerospace.star_guide.step.oxygen_suit", "Suit Up");
        add("gui.nerospace.star_guide.step.oxygen_suit.text",
                "A full four-piece Oxygen Suit is portable life support: a finite air tank that "
                        + "drains slowly off safe zones and refills at airlocks.");
        add("gui.nerospace.star_guide.step.oxygen_suit_t2", "Ember-Proof");
        add("gui.nerospace.star_guide.step.oxygen_suit_t2.text",
                "The cindrite-upgraded Tier 2 suit doubles your air and refills twice as fast. "
                        + "A mixed set counts as Tier 1 — wear all four pieces.");
        add("gui.nerospace.star_guide.step.thermal_suit", "Forged for the Fire");
        add("gui.nerospace.star_guide.step.thermal_suit.text",
                "Cindara's heat makes any other suit burn air four times faster. The Thermal Suit "
                        + "(Tier 2 piece + cindrite) shrugs it off — all four pieces, or no shield.");
        add("gui.nerospace.star_guide.step.cryo_suit", "Dressed for the Deep Cold");
        add("gui.nerospace.star_guide.step.cryo_suit.text",
                "Glacira's cold drains an unprotected suit four times faster and frosts your visor. "
                        + "The glacite-lined Cryo Suit keeps you warm — all four pieces, or no shield.");

        add("gui.nerospace.star_guide.step.terraformer", "World Engine");
        add("gui.nerospace.star_guide.step.terraformer.text",
                "The Terraformer converts dead ground into living, breathable terrain in an "
                        + "expanding circle. Energy is the throttle — feed it well.");
        add("gui.nerospace.star_guide.step.terraformed_ground", "Green Again");
        add("gui.nerospace.star_guide.step.terraformed_ground.text",
                "Terraformed ground is permanently breathable. Stand on land your machine "
                        + "reclaimed and breathe without a suit — the end of the beginning.");
        add("gui.nerospace.star_guide.step.hydration_module", "Meltwater");
        add("gui.nerospace.star_guide.step.hydration_module.text",
                "A Hydration Module touching your Terraformer melts glacite into water for the "
                        + "Hydrated stage — basins fill into lakes behind the green frontier.");
        add("gui.nerospace.star_guide.step.living_world", "World Awake");
        add("gui.nerospace.star_guide.step.living_world.text",
                "Behind the water, the land matures: natural colour, trees, rain — and the first "
                        + "herds. Stand on Living ground and watch a world breathe on its own.");
        add("gui.nerospace.star_guide.step.new_life", "New Life");
        add("gui.nerospace.star_guide.step.new_life.text",
                "Each planet wakes its own livestock: the Meadow Loper, the Ember Strutter, the "
                        + "Woolly Drift. Feed a pair their favourite crop and breed the first "
                        + "generation born off Earth.");

        // Rocket action feedback.
        add("item.nerospace.rocket.deployed", "Rocket deployed on the launch pad");
        add("item.nerospace.rocket.pad_incomplete",
                "The launch pad is incomplete — a rocket needs a full 3x3 of Launch Pad blocks");
        add("item.nerospace.rocket.pad_ring_required",
                "A Tier 3 rocket needs the 3x3 pad ringed with Station Wall — or a Heavy Launch "
                        + "Complex (full 5x5 pad with a Launch Gantry)");
        add("item.nerospace.rocket.pad_heavy_required",
                "A Tier 4 rocket launches only from a Heavy Launch Complex (full 5x5 pad with a "
                        + "Launch Gantry)");
        add("item.nerospace.rocket.pad_occupied",
                "There is already a rocket on this pad");

        // Heavy Launch Complex (LAUNCH_PAD_DESIGN.md).
        add(ModBlocks.LAUNCH_GANTRY.get(), "Launch Gantry");
        add("block.nerospace.launch_gantry.no_rocket", "No rocket on the pad to board");
        add("block.nerospace.launch_gantry.boarded", "Boarded the rocket — strap in");
        add("block.nerospace.rocket_launch_pad.report.none",
                "Pad cluster: %s block(s) — no complete square yet (a rocket needs a full 3x3)");
        add("block.nerospace.rocket_launch_pad.report.3x3",
                "Pad cluster: %s block(s) — 3x3 pad formed (rockets can deploy)");
        add("block.nerospace.rocket_launch_pad.report.5x5",
                "Pad cluster: %s block(s) — full 5x5 formed");
        add("block.nerospace.rocket_launch_pad.report.heavy",
                "Pad cluster: %s block(s) — HEAVY LAUNCH COMPLEX online (12x fuel feed)");
        add("block.nerospace.rocket_launch_pad.report.need_gantry",
                "Add a Launch Gantry beside the 5x5 to complete the Heavy Launch Complex");
        add("block.nerospace.rocket_launch_pad.report.t3_ready",
                "Tier 3 ready: Station Wall ring or Heavy complex present");
        add("block.nerospace.rocket_launch_pad.report.t3_not_ready",
                "Tier 3 needs a Station Wall ring or a Heavy Launch Complex");
        add("entity.nerospace.rocket.arrived", "You have arrived on the planet");
        add("entity.nerospace.rocket.docked", "Docked at the Orbital Station");

        // Greenxertz Navigator action feedback.
        add("item.nerospace.greenxertz_navigator.travel", "Transported to Greenxertz");
        add("item.nerospace.greenxertz_navigator.return", "Returned to the overworld");

        // JEI integration (compat/jei): category titles + stat lines.
        add("jei.nerospace.category.grinding", "Grinding");
        add("jei.nerospace.category.refining", "Fuel Refining");
        add("jei.nerospace.category.combustion", "Combustion Fuel");
        add("jei.nerospace.stat.energy_cost", "%s FE");
        add("jei.nerospace.stat.energy_generated", "+%s FE");
        add("jei.nerospace.stat.time", "%s s");
    }
}
