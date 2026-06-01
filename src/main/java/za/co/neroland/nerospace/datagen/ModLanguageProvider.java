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

        // Entities.
        add("entity.nerospace.rocket", "Rocket");
        add("entity.nerospace.xertz_stalker", "Xertz Stalker");
        add("entity.nerospace.quartz_crawler", "Quartz Crawler");
        add("entity.nerospace.greenling", "Greenling");
        add("entity.nerospace.cinder_stalker", "Cinder Stalker");

        // Planet atmosphere.
        add("message.nerospace.greenxertz.no_air", "You are out of oxygen — reach a launch pad or an Oxygen Generator!");

        // Fuel tank status (action-bar readout).
        add("block.nerospace.fuel_tank.status", "Fuel Tank: %s / %s mB");

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
