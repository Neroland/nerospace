package za.co.neroland.nerospace.datagen;

import java.util.function.Consumer;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.criterion.ChangeDimensionTrigger;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * The Nerospace progression advancement tree (Phase 7d): mine → machine → rocket → planets → station.
 * Titles/descriptions are inline literals to avoid a separate lang file for advancements.
 */
public class ModAdvancements implements AdvancementSubProvider {

    private static final Identifier ROOT_BACKGROUND =
            Identifier.fromNamespaceAndPath("minecraft", "textures/gui/advancements/backgrounds/stone.png");

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver) {
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(ModItems.NEROSIUM_INGOT.get(),
                        Component.literal("Nerospace"),
                        Component.literal("Mine nerosium and reach for the stars"),
                        ROOT_BACKGROUND, AdvancementType.TASK, false, false, false)
                .addCriterion("has_nerosium", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.NEROSIUM_INGOT.get()))
                .save(saver, "nerospace:root");

        AdvancementHolder grinder = Advancement.Builder.advancement()
                .parent(root)
                .display(ModBlocks.NEROSIUM_GRINDER.get(),
                        Component.literal("Industrial Revolution"),
                        Component.literal("Build a Nerosium Grinder"),
                        null, AdvancementType.TASK, true, true, false)
                .addCriterion("has_grinder", InventoryChangeTrigger.TriggerInstance.hasItems(ModBlocks.NEROSIUM_GRINDER.get()))
                .save(saver, "nerospace:nerosium_grinder");

        AdvancementHolder rocket = Advancement.Builder.advancement()
                .parent(grinder)
                .display(ModItems.ROCKET_TIER_1.get(),
                        Component.literal("We Have Liftoff"),
                        Component.literal("Craft a Tier 1 Rocket"),
                        null, AdvancementType.TASK, true, true, false)
                .addCriterion("has_rocket", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.ROCKET_TIER_1.get()))
                .save(saver, "nerospace:rocket");

        AdvancementHolder greenxertz = Advancement.Builder.advancement()
                .parent(rocket)
                .display(ModItems.NEROSTEEL_INGOT.get(),
                        Component.literal("A Whole New World"),
                        Component.literal("Travel to Greenxertz"),
                        null, AdvancementType.GOAL, true, true, false)
                .addCriterion("reached_greenxertz",
                        ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(ModDimensions.GREENXERTZ_LEVEL))
                .save(saver, "nerospace:greenxertz");

        AdvancementHolder cindara = Advancement.Builder.advancement()
                .parent(greenxertz)
                .display(ModItems.CINDRITE.get(),
                        Component.literal("Into the Fire"),
                        Component.literal("Travel to the volcanic moon Cindara"),
                        null, AdvancementType.GOAL, true, true, false)
                .addCriterion("reached_cindara",
                        ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(ModDimensions.CINDARA_LEVEL))
                .save(saver, "nerospace:cindara");

        Advancement.Builder.advancement()
                .parent(cindara)
                .display(ModBlocks.STATION_FLOOR.get(),
                        Component.literal("Orbital"),
                        Component.literal("Dock at the Orbital Station"),
                        null, AdvancementType.CHALLENGE, true, true, false)
                .addCriterion("reached_station",
                        ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(ModDimensions.STATION_LEVEL))
                .save(saver, "nerospace:station");
    }
}
