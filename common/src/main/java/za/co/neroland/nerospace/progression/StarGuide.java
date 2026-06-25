package za.co.neroland.nerospace.progression;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * The Star Guide content table (chapters → steps, in code). Completion is advancement-driven — each
 * step names the advancement that completes it, and the menu packs per-chapter completion bitmasks
 * from {@code ServerPlayer.getAdvancements()}. Icons are suppliers because the table is built before
 * registry objects exist.
 *
 * <p>Cross-loader port note: two steps reference content not yet ported (the station-founding
 * {@code STATION_CHARTER} and the meadow-loper {@code LOPER_HAUNCH}); their icons are substituted with
 * ported stand-ins (Station Floor / Drift Fleece). Those steps' advancements stay unresolved, so they
 * read as incomplete — forward-looking guidance until those systems land.</p>
 */
public final class StarGuide {

    /** One step of a chapter: icon + lang keys + the advancement that completes it. */
    public record Step(String id, Supplier<? extends ItemLike> icon,
            Identifier advancement) {

        public String titleKey() {
            return java.util.Objects.requireNonNull("gui.nerospace.star_guide.step." + this.id);
        }

        public String textKey() {
            return java.util.Objects.requireNonNull("gui.nerospace.star_guide.step." + this.id + ".text");
        }

        public ItemStack iconStack() {
            return java.util.Objects.requireNonNull(new ItemStack(StarGuide.icon(this.icon)));
        }
    }

    /** A chapter: lang key + ordered steps (≤ 16 so the completion bitmask fits a data slot). */
    public record Chapter(String id, List<Step> steps) {

        public String titleKey() {
            return java.util.Objects.requireNonNull("gui.nerospace.star_guide.chapter." + this.id);
        }
    }

    private static Identifier adv(String path) {
        return NerospaceCommon.id(path);
    }

    private static ItemLike icon(Supplier<? extends ItemLike> supplier) {
        return java.util.Objects.requireNonNull(supplier.get());
    }

    private static StarGuide.Step step(String id, Supplier<? extends ItemLike> icon,
            String advancementPath) {
        return new Step(id, icon, java.util.Objects.requireNonNull(adv(advancementPath)));
    }

    /** The chapters (order = chapter index used by menu completion bitmasks). */
    public static final List<Chapter> CHAPTERS = List.of(
            new Chapter("nerosium", List.of(
                    step("raw_nerosium", () -> java.util.Objects.requireNonNull(ModItems.RAW_NEROSIUM.get()), "guide/raw_nerosium"),
                    step("nerosium_ingot", () -> java.util.Objects.requireNonNull(ModItems.NEROSIUM_INGOT.get()), "root"),
                    step("nerosium_pickaxe", () -> java.util.Objects.requireNonNull(ModItems.NEROSIUM_PICKAXE.get()), "guide/nerosium_pickaxe"))),
            new Chapter("machines", List.of(
                    step("nerosium_grinder", () -> java.util.Objects.requireNonNull(ModBlocks.NEROSIUM_GRINDER.get()), "nerosium_grinder"),
                    step("nerosium_dust", () -> java.util.Objects.requireNonNull(ModItems.NEROSIUM_DUST.get()), "guide/nerosium_dust"),
                    step("combustion_generator", () -> java.util.Objects.requireNonNull(ModBlocks.COMBUSTION_GENERATOR.get()), "guide/combustion_generator"))),
            new Chapter("power_grid", List.of(
                    step("universal_pipe", () -> java.util.Objects.requireNonNull(ModBlocks.UNIVERSAL_PIPE.get()), "guide/universal_pipe"),
                    step("battery", () -> java.util.Objects.requireNonNull(ModBlocks.BATTERY.get()), "guide/battery"),
                    step("passive_generator", () -> java.util.Objects.requireNonNull(ModBlocks.PASSIVE_GENERATOR.get()), "guide/passive_generator"),
                    step("configurator", () -> java.util.Objects.requireNonNull(ModItems.CONFIGURATOR.get()), "guide/configurator"))),
            new Chapter("rocketry", List.of(
                    step("rocket_fuel_canister", () -> java.util.Objects.requireNonNull(ModItems.ROCKET_FUEL_CANISTER.get()), "guide/rocket_fuel_canister"),
                    step("rocket_launch_pad", () -> java.util.Objects.requireNonNull(ModBlocks.ROCKET_LAUNCH_PAD.get()), "guide/rocket_launch_pad"),
                    step("rocket_tier_1", () -> java.util.Objects.requireNonNull(ModItems.ROCKET_TIER_1.get()), "rocket"),
                    step("station", () -> java.util.Objects.requireNonNull(ModBlocks.STATION_FLOOR.get()), "station"),
                    step("station_charter", () -> java.util.Objects.requireNonNull(ModItems.STATION_CHARTER.get()), "guide/station_charter"))),
            new Chapter("new_worlds", List.of(
                    step("rocket_tier_2", () -> java.util.Objects.requireNonNull(ModItems.ROCKET_TIER_2.get()), "guide/rocket_tier_2"),
                    step("greenxertz", () -> java.util.Objects.requireNonNull(ModItems.NEROSTEEL_INGOT.get()), "greenxertz"),
                    step("nerosteel_ingot", () -> java.util.Objects.requireNonNull(ModItems.RAW_NEROSTEEL.get()), "guide/nerosteel_ingot"),
                    step("rocket_tier_3", () -> java.util.Objects.requireNonNull(ModItems.ROCKET_TIER_3.get()), "guide/rocket_tier_3"),
                    step("cindara", () -> java.util.Objects.requireNonNull(ModItems.CINDRITE.get()), "cindara"),
                    step("cindrite", () -> java.util.Objects.requireNonNull(ModBlocks.CINDRITE_BLOCK.get()), "guide/cindrite"),
                    step("rocket_tier_4", () -> java.util.Objects.requireNonNull(ModItems.ROCKET_TIER_4.get()), "guide/rocket_tier_4"),
                    step("glacira", () -> java.util.Objects.requireNonNull(ModItems.GLACITE.get()), "glacira"),
                    step("glacite", () -> java.util.Objects.requireNonNull(ModBlocks.GLACITE_BLOCK.get()), "guide/glacite"))),
            new Chapter("mining", List.of(
                    step("quarry_landmark", () -> java.util.Objects.requireNonNull(ModBlocks.QUARRY_LANDMARK.get()), "guide/quarry_landmark"),
                    step("frame_casing", () -> java.util.Objects.requireNonNull(ModItems.FRAME_CASING.get()), "guide/frame_casing"),
                    step("quarry_controller", () -> java.util.Objects.requireNonNull(ModBlocks.QUARRY_CONTROLLER.get()), "guide/quarry_controller"),
                    step("upgrade_module", () -> java.util.Objects.requireNonNull(ModItems.SPEED_MODULE.get()), "guide/upgrade_module"))),
            new Chapter("vacuum", List.of(
                    step("oxygen_generator", () -> java.util.Objects.requireNonNull(ModBlocks.OXYGEN_GENERATOR.get()), "guide/oxygen_generator"),
                    step("gas_tank", () -> java.util.Objects.requireNonNull(ModBlocks.GAS_TANK.get()), "guide/gas_tank"),
                    step("oxygen_suit", () -> java.util.Objects.requireNonNull(ModItems.OXYGEN_SUIT_HELMET.get()), "guide/oxygen_suit"),
                    step("oxygen_suit_t2", () -> java.util.Objects.requireNonNull(ModItems.OXYGEN_SUIT_T2_HELMET.get()), "guide/oxygen_suit_t2"),
                    step("thermal_suit", () -> java.util.Objects.requireNonNull(ModItems.OXYGEN_SUIT_HEAT_HELMET.get()), "guide/thermal_suit"),
                    step("cryo_suit", () -> java.util.Objects.requireNonNull(ModItems.OXYGEN_SUIT_COLD_HELMET.get()), "guide/cryo_suit"))),
            new Chapter("terraforming", List.of(
                    step("terraformer", () -> java.util.Objects.requireNonNull(ModBlocks.TERRAFORMER.get()), "guide/terraformer"),
                    step("terraformed_ground", () -> java.util.Objects.requireNonNull(ModBlocks.TERRAFORMER.get()), "guide/terraformed_ground"),
                    step("hydration_module", () -> java.util.Objects.requireNonNull(ModBlocks.HYDRATION_MODULE.get()), "guide/hydration_module"),
                    step("living_world", () -> java.util.Objects.requireNonNull(ModItems.MEADOW_LOPER_SPAWN_EGG.get()), "guide/living_world"),
                    // LOPER_HAUNCH not yet ported — substitute the Drift Fleece icon.
                    step("new_life", () -> java.util.Objects.requireNonNull(ModItems.DRIFT_FLEECE.get()), "guide/new_life"))),
            new Chapter("meteor_events", List.of(
                    step("meteor_site", () -> java.util.Objects.requireNonNull(ModItems.ALIEN_FRAGMENT.get()), "guide/alien_fragment"),
                    step("alien_tech", () -> java.util.Objects.requireNonNull(ModItems.ALIEN_TECH_SCRAP.get()), "guide/alien_tech_scrap"),
                    step("alien_core", () -> java.util.Objects.requireNonNull(ModItems.ALIEN_CORE.get()), "guide/alien_core"))));

    public static final int CHAPTER_COUNT = CHAPTERS.size();

    private StarGuide() {
    }

    /** Total step count across all chapters (sanity bound for menu button ids). */
    public static int totalSteps() {
        return CHAPTERS.stream().mapToInt(c -> c.steps().size()).sum();
    }
}
