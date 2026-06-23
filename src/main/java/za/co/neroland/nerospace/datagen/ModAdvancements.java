package za.co.neroland.nerospace.datagen;

import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.ChangeDimensionTrigger;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;

import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModCriteria;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * The Nerospace progression advancement tree — the Star Guide's source of truth
 * (STAR_GUIDE_DESIGN.md §3): every guide step names one of these advancements, so the tree mirrors
 * the guide by construction. The original six ids (root, nerosium_grinder, rocket, greenxertz,
 * cindara, station) are preserved; the expansion lives under {@code nerospace:guide/*}. Titles and
 * descriptions are inline literals to avoid a separate lang pass for advancements.
 */
public class ModAdvancements implements AdvancementSubProvider {

    private static final Identifier ROOT_BACKGROUND =
            Identifier.fromNamespaceAndPath("minecraft", "textures/gui/advancements/backgrounds/stone.png");

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver) {
        // --- Chapter 1: Nerosium -------------------------------------------------------------
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(ModItems.NEROSIUM_INGOT.get(),
                        Component.literal("Nerospace"),
                        Component.literal("Mine nerosium and reach for the stars"),
                        ROOT_BACKGROUND, AdvancementType.TASK, false, false, false)
                .addCriterion("has_nerosium", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.NEROSIUM_INGOT.get()))
                .save(saver, "nerospace:root");

        item(saver, root, "guide/raw_nerosium", ModItems.RAW_NEROSIUM.get(),
                "Strange Red Rock", "Mine raw nerosium from nerosium ore");
        item(saver, root, "guide/nerosium_pickaxe", ModItems.NEROSIUM_PICKAXE.get(),
                "Tools of the Trade", "Craft a nerosium pickaxe");

        // --- Chapter 2: Machines -------------------------------------------------------------
        AdvancementHolder grinder = Advancement.Builder.advancement()
                .parent(root)
                .display(ModBlocks.NEROSIUM_GRINDER.get(),
                        Component.literal("Industrial Revolution"),
                        Component.literal("Build a Nerosium Grinder"),
                        null, AdvancementType.TASK, true, true, false)
                .addCriterion("has_grinder", InventoryChangeTrigger.TriggerInstance.hasItems(ModBlocks.NEROSIUM_GRINDER.get()))
                .save(saver, "nerospace:nerosium_grinder");

        item(saver, grinder, "guide/nerosium_dust", ModItems.NEROSIUM_DUST.get(),
                "Finely Ground", "Grind nerosium into dust");
        AdvancementHolder combustion = item(saver, grinder, "guide/combustion_generator",
                ModBlocks.COMBUSTION_GENERATOR.get(),
                "Burning Bright", "Build a Combustion Generator");

        // --- Chapter 3: Power Grid -----------------------------------------------------------
        AdvancementHolder pipe = item(saver, combustion, "guide/universal_pipe",
                ModBlocks.UNIVERSAL_PIPE.get(),
                "Connect Everything", "Craft a Universal Pipe");
        item(saver, pipe, "guide/battery", ModBlocks.BATTERY.get(),
                "Stored Potential", "Craft a Battery");
        item(saver, pipe, "guide/passive_generator", ModBlocks.PASSIVE_GENERATOR.get(),
                "Slow and Steady", "Build a Passive Generator");
        item(saver, pipe, "guide/configurator", ModItems.CONFIGURATOR.get(),
                "Fine Tuning", "Craft a Configurator");

        // --- Chapter 4: Rocketry -------------------------------------------------------------
        AdvancementHolder canister = item(saver, combustion, "guide/rocket_fuel_canister",
                ModItems.ROCKET_FUEL_CANISTER.get(),
                "Highly Flammable", "Fill a Rocket Fuel Canister");
        AdvancementHolder pad = item(saver, canister, "guide/rocket_launch_pad",
                ModBlocks.ROCKET_LAUNCH_PAD.get(),
                "Ground Control", "Craft a Rocket Launch Pad (you'll want a 3x3)");

        AdvancementHolder rocket = Advancement.Builder.advancement()
                .parent(pad)
                .display(ModItems.ROCKET_TIER_1.get(),
                        Component.literal("We Have Liftoff"),
                        Component.literal("Craft a Tier 1 Rocket"),
                        null, AdvancementType.TASK, true, true, false)
                .addCriterion("has_rocket", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.ROCKET_TIER_1.get()))
                .save(saver, "nerospace:rocket");

        AdvancementHolder station = Advancement.Builder.advancement()
                .parent(rocket)
                .display(ModBlocks.STATION_FLOOR.get(),
                        Component.literal("Orbital"),
                        Component.literal("Dock at the Orbital Station"),
                        null, AdvancementType.GOAL, true, true, false)
                .addCriterion("reached_station",
                        ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(ModDimensions.STATION_LEVEL))
                .save(saver, "nerospace:station");

        // Multiple stations (MULTI_STATION_DESIGN.md §7): found your own station.
        Advancement.Builder.advancement()
                .parent(station)
                .display(ModItems.STATION_CHARTER.get(),
                        Component.literal("Homestead in Orbit"),
                        Component.literal("Found your own station with a Station Charter"),
                        null, AdvancementType.GOAL, true, true, false)
                .addCriterion("founded_station", new Criterion<>(
                        ModCriteria.FOUNDED_STATION.get(),
                        new PlayerTrigger.TriggerInstance(Optional.empty())))
                .save(saver, "nerospace:guide/station_charter");

        // --- Chapter 5: New Worlds -----------------------------------------------------------
        AdvancementHolder tier2 = item(saver, station, "guide/rocket_tier_2",
                ModItems.ROCKET_TIER_2.get(),
                "Bigger Boosters", "Craft a Tier 2 Rocket");

        AdvancementHolder greenxertz = Advancement.Builder.advancement()
                .parent(tier2)
                .display(ModItems.NEROSTEEL_INGOT.get(),
                        Component.literal("A Whole New World"),
                        Component.literal("Travel to Greenxertz"),
                        null, AdvancementType.GOAL, true, true, false)
                .addCriterion("reached_greenxertz",
                        ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(ModDimensions.GREENXERTZ_LEVEL))
                .save(saver, "nerospace:greenxertz");

        item(saver, greenxertz, "guide/nerosteel_ingot", ModItems.NEROSTEEL_INGOT.get(),
                "Alien Alloy", "Smelt a nerosteel ingot from Greenxertz ore");

        // --- Mining automation (MINER_DESIGN): the quarry, gated behind nerosteel ----------------
        AdvancementHolder frameCasing = item(saver, greenxertz, "guide/frame_casing",
                ModItems.FRAME_CASING.get(),
                "Frameworks", "Craft Frame Casing from nerosteel — the quarry builds its frame from these");
        AdvancementHolder quarryLandmark = item(saver, frameCasing, "guide/quarry_landmark",
                ModBlocks.QUARRY_LANDMARK.get(),
                "Stake a Claim", "Craft Quarry Landmarks to mark out a rectangular mining region");
        AdvancementHolder quarryController = item(saver, quarryLandmark, "guide/quarry_controller",
                ModBlocks.QUARRY_CONTROLLER.get(),
                "Strip Miner", "Build a Quarry Controller and automate your digging");
        Advancement.Builder.advancement()
                .parent(quarryController)
                .display(ModItems.SPEED_MODULE.get(),
                        Component.literal("Tune It Up"),
                        Component.literal("Craft an upgrade module — speed, efficiency, fortune or silk touch"),
                        null, AdvancementType.TASK, true, true, false)
                .addCriterion("speed", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.SPEED_MODULE.get()))
                .addCriterion("efficiency", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.EFFICIENCY_MODULE.get()))
                .addCriterion("fortune", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.FORTUNE_MODULE.get()))
                .addCriterion("silk_touch", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.SILK_TOUCH_MODULE.get()))
                .requirements(net.minecraft.advancements.AdvancementRequirements.Strategy.OR)
                .save(saver, "nerospace:guide/upgrade_module");
        AdvancementHolder tier3 = item(saver, greenxertz, "guide/rocket_tier_3",
                ModItems.ROCKET_TIER_3.get(),
                "To the Fire Moon", "Craft a Tier 3 Rocket (its pad needs a Station Wall ring)");

        AdvancementHolder cindara = Advancement.Builder.advancement()
                .parent(tier3)
                .display(ModItems.CINDRITE.get(),
                        Component.literal("Into the Fire"),
                        Component.literal("Travel to the volcanic moon Cindara"),
                        null, AdvancementType.GOAL, true, true, false)
                .addCriterion("reached_cindara",
                        ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(ModDimensions.CINDARA_LEVEL))
                .save(saver, "nerospace:cindara");

        item(saver, cindara, "guide/cindrite", ModItems.CINDRITE.get(),
                "Heart of the Volcano", "Mine cindrite on Cindara");

        AdvancementHolder tier4 = item(saver, cindara, "guide/rocket_tier_4",
                ModItems.ROCKET_TIER_4.get(),
                "To the Ice Moon", "Craft a Tier 4 Rocket (it launches only from a Heavy Launch Complex)");

        AdvancementHolder glacira = Advancement.Builder.advancement()
                .parent(tier4)
                .display(ModItems.GLACITE.get(),
                        Component.literal("Into the Cold"),
                        Component.literal("Travel to the frozen moon Glacira"),
                        null, AdvancementType.GOAL, true, true, false)
                .addCriterion("reached_glacira",
                        ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(ModDimensions.GLACIRA_LEVEL))
                .save(saver, "nerospace:glacira");

        item(saver, glacira, "guide/glacite", ModItems.GLACITE.get(),
                "Heart of the Glacier", "Mine glacite on Glacira");

        // --- Chapter 6: Surviving Vacuum -------------------------------------------------------
        AdvancementHolder oxygenGen = item(saver, station, "guide/oxygen_generator",
                ModBlocks.OXYGEN_GENERATOR.get(),
                "Something to Breathe", "Build an Oxygen Generator");
        item(saver, oxygenGen, "guide/gas_tank", ModBlocks.GAS_TANK.get(),
                "Bottled Air", "Craft a Gas Tank for oxygen storage");

        AdvancementHolder suit = Advancement.Builder.advancement()
                .parent(oxygenGen)
                .display(ModItems.OXYGEN_SUIT_HELMET.get(),
                        Component.literal("Suit Up"),
                        Component.literal("Assemble a full Oxygen Suit"),
                        null, AdvancementType.GOAL, true, true, false)
                .addCriterion("has_suit", InventoryChangeTrigger.TriggerInstance.hasItems(
                        ModItems.OXYGEN_SUIT_HELMET.get(), ModItems.OXYGEN_SUIT_CHESTPLATE.get(),
                        ModItems.OXYGEN_SUIT_LEGGINGS.get(), ModItems.OXYGEN_SUIT_BOOTS.get()))
                .save(saver, "nerospace:guide/oxygen_suit");

        AdvancementHolder suitT2 = Advancement.Builder.advancement()
                .parent(suit)
                .display(ModItems.OXYGEN_SUIT_T2_HELMET.get(),
                        Component.literal("Ember-Proof"),
                        Component.literal("Assemble a full Tier 2 (cindrite) Oxygen Suit"),
                        null, AdvancementType.GOAL, true, true, false)
                .addCriterion("has_suit_t2", InventoryChangeTrigger.TriggerInstance.hasItems(
                        ModItems.OXYGEN_SUIT_T2_HELMET.get(), ModItems.OXYGEN_SUIT_T2_CHESTPLATE.get(),
                        ModItems.OXYGEN_SUIT_T2_LEGGINGS.get(), ModItems.OXYGEN_SUIT_T2_BOOTS.get()))
                .save(saver, "nerospace:guide/oxygen_suit_t2");

        // Hazard suit variants (SUIT_HAZARD_DESIGN.md): sidegrades of T2, both parented on it.
        Advancement.Builder.advancement()
                .parent(suitT2)
                .display(ModItems.OXYGEN_SUIT_HEAT_HELMET.get(),
                        Component.literal("Forged for the Fire"),
                        Component.literal("Assemble a full Thermal Suit — Cindara's heat stops draining your air"),
                        null, AdvancementType.GOAL, true, true, false)
                .addCriterion("has_thermal_suit", InventoryChangeTrigger.TriggerInstance.hasItems(
                        ModItems.OXYGEN_SUIT_HEAT_HELMET.get(), ModItems.OXYGEN_SUIT_HEAT_CHESTPLATE.get(),
                        ModItems.OXYGEN_SUIT_HEAT_LEGGINGS.get(), ModItems.OXYGEN_SUIT_HEAT_BOOTS.get()))
                .save(saver, "nerospace:guide/thermal_suit");

        Advancement.Builder.advancement()
                .parent(suitT2)
                .display(ModItems.OXYGEN_SUIT_COLD_HELMET.get(),
                        Component.literal("Dressed for the Deep Cold"),
                        Component.literal("Assemble a full Cryo Suit — Glacira's cold stops draining your air"),
                        null, AdvancementType.GOAL, true, true, false)
                .addCriterion("has_cryo_suit", InventoryChangeTrigger.TriggerInstance.hasItems(
                        ModItems.OXYGEN_SUIT_COLD_HELMET.get(), ModItems.OXYGEN_SUIT_COLD_CHESTPLATE.get(),
                        ModItems.OXYGEN_SUIT_COLD_LEGGINGS.get(), ModItems.OXYGEN_SUIT_COLD_BOOTS.get()))
                .save(saver, "nerospace:guide/cryo_suit");

        // --- Chapter 7: Terraforming ------------------------------------------------------------
        AdvancementHolder terraformer = item(saver, cindara, "guide/terraformer",
                ModBlocks.TERRAFORMER.get(),
                "World Engine", "Build a Terraformer");

        AdvancementHolder terraformedGround = Advancement.Builder.advancement()
                .parent(terraformer)
                .display(ModBlocks.TERRAFORMER.get(),
                        Component.literal("Green Again"),
                        Component.literal("Stand on ground your Terraformer made breathable"),
                        null, AdvancementType.CHALLENGE, true, true, false)
                .addCriterion("terraformed_ground", new Criterion<>(
                        ModCriteria.TERRAFORMED_GROUND.get(),
                        new PlayerTrigger.TriggerInstance(Optional.empty())))
                .save(saver, "nerospace:guide/terraformed_ground");

        // --- Deeper terraforming (DEEPER_TERRAFORM_DESIGN.md §11) ------------------------------
        AdvancementHolder hydrationModule = item(saver, terraformedGround, "guide/hydration_module",
                ModBlocks.HYDRATION_MODULE.get(),
                "Meltwater", "Build a Hydration Module and feed your Terraformer glacite");

        AdvancementHolder livingWorld = Advancement.Builder.advancement()
                .parent(hydrationModule)
                .display(ModItems.MEADOW_LOPER_SPAWN_EGG.get(),
                        Component.literal("World Awake"),
                        Component.literal("Stand on land your Terraformer brought fully to life"),
                        null, AdvancementType.CHALLENGE, true, true, false)
                .addCriterion("living_ground", new Criterion<>(
                        ModCriteria.LIVING_GROUND.get(),
                        new PlayerTrigger.TriggerInstance(Optional.empty())))
                .save(saver, "nerospace:guide/living_world");

        // Any of the three livestock species satisfies "New Life" (OR requirements).
        Advancement.Builder.advancement()
                .parent(livingWorld)
                .display(ModItems.LOPER_HAUNCH.get(),
                        Component.literal("New Life"),
                        Component.literal("Breed a creature born of a terraformed world"),
                        null, AdvancementType.GOAL, true, true, false)
                .addCriterion("bred_meadow_loper", bredLivestock(registries, ModEntities.MEADOW_LOPER.get()))
                .addCriterion("bred_ember_strutter", bredLivestock(registries, ModEntities.EMBER_STRUTTER.get()))
                .addCriterion("bred_woolly_drift", bredLivestock(registries, ModEntities.WOOLLY_DRIFT.get()))
                .requirements(net.minecraft.advancements.AdvancementRequirements.Strategy.OR)
                .save(saver, "nerospace:guide/new_life");
    }

    /** A bred-animals criterion matching one of the terraform livestock species as a parent. */
    private static Criterion<?> bredLivestock(HolderLookup.Provider registries,
            net.minecraft.world.entity.EntityType<?> type) {
        return net.minecraft.advancements.criterion.BredAnimalsTrigger.TriggerInstance.bredAnimals(
                net.minecraft.advancements.criterion.EntityPredicate.Builder.entity().of(
                        registries.lookupOrThrow(net.minecraft.core.registries.Registries.ENTITY_TYPE),
                        type));
    }

    /** A simple has-item TASK advancement under {@code nerospace:<path>}. */
    private static AdvancementHolder item(Consumer<AdvancementHolder> saver, AdvancementHolder parent,
            String path, ItemLike icon, String title, String description) {
        return Advancement.Builder.advancement()
                .parent(parent)
                .display(icon,
                        Component.literal(title),
                        Component.literal(description),
                        null, AdvancementType.TASK, true, true, false)
                .addCriterion("has_item", InventoryChangeTrigger.TriggerInstance.hasItems(icon))
                .save(saver, "nerospace:" + path);
    }
}
