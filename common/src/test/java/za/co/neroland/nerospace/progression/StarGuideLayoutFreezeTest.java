package za.co.neroland.nerospace.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The Star Guide completion bitmask is chapter index × step position, so the 1.x layout (chapters 0–9) is frozen
 * forever (001, append-only). 2.0 Worlds chapters start at index 10 with {@code worlds_luna}: arrival first, the
 * hidden cheese step last.
 */
class StarGuideLayoutFreezeTest {

    /** {@code chapter/step=advancement} for chapters 0–9, in order — the 1.6.0 layout. Never edit. */
    private static final List<String> FROZEN = List.of(
            "nerosium/raw_nerosium=guide/raw_nerosium",
            "nerosium/nerosium_ingot=root",
            "nerosium/nerosium_pickaxe=guide/nerosium_pickaxe",
            "machines/nerosium_grinder=nerosium_grinder",
            "machines/nerosium_dust=guide/nerosium_dust",
            "machines/combustion_generator=guide/combustion_generator",
            "power_grid/universal_pipe=guide/universal_pipe",
            "power_grid/battery=guide/battery",
            "power_grid/passive_generator=guide/passive_generator",
            "power_grid/configurator=guide/configurator",
            "rocketry/rocket_fuel_canister=guide/rocket_fuel_canister",
            "rocketry/rocket_launch_pad=guide/rocket_launch_pad",
            "rocketry/rocket_tier_1=rocket",
            "rocketry/station=station",
            "rocketry/station_charter=guide/station_charter",
            "new_worlds/rocket_tier_2=guide/rocket_tier_2",
            "new_worlds/greenxertz=greenxertz",
            "new_worlds/nerosteel_ingot=guide/nerosteel_ingot",
            "new_worlds/rocket_tier_3=guide/rocket_tier_3",
            "new_worlds/cindara=cindara",
            "new_worlds/cindrite=guide/cindrite",
            "new_worlds/rocket_tier_4=guide/rocket_tier_4",
            "new_worlds/glacira=glacira",
            "new_worlds/glacite=guide/glacite",
            "mining/quarry_landmark=guide/quarry_landmark",
            "mining/frame_casing=guide/frame_casing",
            "mining/quarry_controller=guide/quarry_controller",
            "mining/upgrade_module=guide/upgrade_module",
            "vacuum/oxygen_generator=guide/oxygen_generator",
            "vacuum/gas_tank=guide/gas_tank",
            "vacuum/oxygen_suit=guide/oxygen_suit",
            "vacuum/oxygen_suit_t2=guide/oxygen_suit_t2",
            "vacuum/thermal_suit=guide/thermal_suit",
            "vacuum/cryo_suit=guide/cryo_suit",
            "terraforming/terraformer=guide/terraformer",
            "terraforming/terraformed_ground=guide/terraformed_ground",
            "terraforming/hydration_module=guide/hydration_module",
            "terraforming/living_world=guide/living_world",
            "terraforming/new_life=guide/new_life",
            "meteor_events/meteor_site=guide/alien_fragment",
            "meteor_events/alien_tech=guide/alien_tech_scrap",
            "meteor_events/alien_core=guide/alien_core",
            "freight/cargo_pad=guide/cargo_pad",
            "freight/cargo_rocket=guide/cargo_rocket",
            "freight/first_freight=guide/first_freight");

    /**
     * {@code chapter/step=advancement} for chapters 10-16, in order, a trailing {@code !} marking a hidden step: the
     * 2.x Worlds chapters as released and Europa (2.4.0). The per-chapter bit of a step is its position, so a reorder
     * here would move saved progress. Never edit; append new chapters only.
     */
    private static final List<String> FROZEN_WORLDS = List.of(
            "worlds_luna/worlds_luna_arrive=luna",
            "worlds_luna/worlds_luna_ore=worlds/luna/ore",
            "worlds_luna/worlds_luna_block=worlds/luna/block",
            "worlds_luna/worlds_luna_weather=worlds/luna/weather",
            "worlds_luna/worlds_luna_terraform=worlds/luna/terraform",
            "worlds_luna/big_cheese=luna/the_big_cheese!",
            "worlds_mars/worlds_mars_arrive=mars",
            "worlds_mars/worlds_mars_ore=worlds/mars/ore",
            "worlds_mars/worlds_mars_block=worlds/mars/block",
            "worlds_mars/worlds_mars_weather=worlds/mars/weather",
            "worlds_mars/worlds_mars_terraform=worlds/mars/terraform",
            "worlds_cindara/worlds_cindara_arrive=cindara",
            "worlds_cindara/worlds_cindara_ore=worlds/cindara/ore",
            "worlds_cindara/worlds_cindara_block=worlds/cindara/block",
            "worlds_cindara/worlds_cindara_weather=worlds/cindara/weather",
            "worlds_cindara/worlds_cindara_terraform=worlds/cindara/terraform",
            "worlds_glacira/worlds_glacira_arrive=glacira",
            "worlds_glacira/worlds_glacira_ore=worlds/glacira/ore",
            "worlds_glacira/worlds_glacira_block=worlds/glacira/block",
            "worlds_glacira/worlds_glacira_weather=worlds/glacira/weather",
            "worlds_glacira/worlds_glacira_terraform=worlds/glacira/terraform",
            "worlds_greenxertz/worlds_greenxertz_arrive=greenxertz",
            "worlds_greenxertz/worlds_greenxertz_ore=worlds/greenxertz/ore",
            "worlds_greenxertz/worlds_greenxertz_block=worlds/greenxertz/block",
            "worlds_greenxertz/worlds_greenxertz_weather=worlds/greenxertz/weather",
            "worlds_greenxertz/worlds_greenxertz_terraform=worlds/greenxertz/terraform",
            "worlds_nyxara/worlds_nyxara_arrive=nyxara",
            "worlds_nyxara/worlds_nyxara_ore=worlds/nyxara/ore",
            "worlds_nyxara/worlds_nyxara_block=worlds/nyxara/block",
            "worlds_nyxara/worlds_nyxara_weather=worlds/nyxara/weather",
            "worlds_nyxara/worlds_nyxara_terraform=worlds/nyxara/terraform",
            "worlds_europa/worlds_europa_arrive=europa",
            "worlds_europa/worlds_europa_ore=worlds/europa/ore",
            "worlds_europa/worlds_europa_block=worlds/europa/block",
            "worlds_europa/worlds_europa_weather=worlds/europa/weather",
            "worlds_europa/worlds_europa_terraform=worlds/europa/terraform",
            "worlds_europa/rare_earth=europa/rare_earth!");

    private static final List<String> WORLDS_ORDER = List.of("worlds_luna", "worlds_mars", "worlds_cindara",
            "worlds_glacira", "worlds_greenxertz", "worlds_nyxara", "worlds_europa");

    @Test
    @DisplayName("chapters 0-9: ids, step positions and advancements are the 1.6.0 layout")
    void frozenChapters() {
        List<String> actual = new ArrayList<>();
        for (int c = 0; c < 10; c++) {
            StarGuide.Chapter chapter = StarGuide.CHAPTERS.get(c);
            for (StarGuide.Step step : chapter.steps()) {
                assertFalse(step.hidden(), "no hidden step in a 1.x chapter");
                actual.add(chapter.id() + "/" + step.id() + "=" + step.advancement().getPath());
            }
        }
        assertEquals(FROZEN, actual);
    }

    @Test
    @DisplayName("chapters 10-16: the Worlds chapters' step ids, positions, advancements and hidden flags are frozen")
    void frozenWorldsChapters() {
        List<String> actual = new ArrayList<>();
        for (int c = 10; c < 17; c++) {
            StarGuide.Chapter chapter = StarGuide.CHAPTERS.get(c);
            for (StarGuide.Step step : chapter.steps()) {
                actual.add(chapter.id() + "/" + step.id() + "=" + step.advancement().getPath()
                        + (step.hidden() ? "!" : ""));
            }
        }
        assertEquals(FROZEN_WORLDS, actual);
    }

    @Test
    @DisplayName("Worlds chapters are appended from index 10; each chapter's mask fits a data slot")
    void worldsAppended() {
        // 2.4.0: Europa is chapter 16, so there are 17. The per-chapter mask (<= 16 steps) is what must fit a
        // 16-bit data slot; the chapter count only sizes the data array and the varint button id (chapter * 16 + step).
        assertEquals(17, StarGuide.CHAPTER_COUNT);
        for (int i = 0; i < WORLDS_ORDER.size(); i++) {
            assertEquals(WORLDS_ORDER.get(i), StarGuide.CHAPTERS.get(10 + i).id());
        }
        for (StarGuide.Chapter chapter : StarGuide.CHAPTERS) {
            assertTrue(chapter.steps().size() <= 16, chapter.id());
        }
    }

    @Test
    @DisplayName("worlds_luna: arrival first (worlds_luna_arrive), the hidden big_cheese step last and the only hidden one")
    void lunaChapter() {
        StarGuide.Chapter luna = StarGuide.CHAPTERS.get(10);
        List<StarGuide.Step> steps = luna.steps();
        assertEquals("worlds_luna_arrive", steps.get(0).id());
        assertEquals("luna", steps.get(0).advancement().getPath());
        StarGuide.Step last = steps.get(steps.size() - 1);
        assertEquals("big_cheese", last.id());
        assertEquals("luna/the_big_cheese", last.advancement().getPath());
        assertTrue(last.hidden());
        for (int i = 0; i < steps.size() - 1; i++) {
            assertFalse(steps.get(i).hidden(), steps.get(i).id());
        }
        assertFalse(luna.isShown(steps.size() - 1, 0), "the cheese step is never hinted");
    }

    @Test
    @DisplayName("worlds_europa: chapter 16, arrival first, the hidden rare_earth step last and the only hidden one")
    void europaChapter() {
        StarGuide.Chapter europa = StarGuide.CHAPTERS.get(16);
        assertEquals("worlds_europa", europa.id());
        List<StarGuide.Step> steps = europa.steps();
        assertEquals(6, steps.size());
        assertEquals("worlds_europa_arrive", steps.get(0).id());
        StarGuide.Step last = steps.get(steps.size() - 1);
        assertEquals("rare_earth", last.id());
        assertEquals("europa/rare_earth", last.advancement().getPath());
        assertTrue(last.hidden());
        for (int i = 0; i < steps.size() - 1; i++) {
            assertFalse(steps.get(i).hidden(), steps.get(i).id());
        }
        assertFalse(europa.isShown(steps.size() - 1, 0), "the hidden step is never hinted");
        assertEquals(List.of("worlds_europa_arrive", "worlds_europa_ore", "worlds_europa_block",
                "worlds_europa_weather", "worlds_europa_terraform", "rare_earth"),
                steps.stream().map(StarGuide.Step::id).toList());
    }

    @Test
    @DisplayName("each Worlds chapter: arrive, ore, block, weather, terraform")
    void worldsSteps() {
        for (int i = 0; i < WORLDS_ORDER.size(); i++) {
            StarGuide.Chapter chapter = StarGuide.CHAPTERS.get(10 + i);
            String planet = chapter.id().substring("worlds_".length());
            List<String> paths = chapter.steps().stream().filter(s -> !s.hidden())
                    .map(s -> s.advancement().getPath()).toList();
            assertEquals(5, paths.size(), chapter.id());
            for (String s : List.of("ore", "block", "weather", "terraform")) {
                assertTrue(paths.contains("worlds/" + planet + "/" + s), chapter.id() + " " + s);
            }
            assertTrue(WorldsProgress.WORLDS.contains(planet), planet);
            StarGuide.Step arrive = chapter.steps().get(0);
            assertEquals("worlds_" + planet + "_arrive", arrive.id(), "arrival step id");
            assertEquals(planet, arrive.advancement().getPath(), "arrival is the top-level planet advancement");
        }
        assertEquals(WorldsProgress.WORLDS, WORLDS_ORDER.stream().map(c -> c.substring("worlds_".length())).toList(),
                "WorldsProgress follows the chapter (roster) order");
    }

    @Test
    @DisplayName("weather survival: two minutes under open sky during the trial; Luna's trial is the night")
    void weatherRules() {
        assertTrue(WeatherSurvivalRules.trialActive(
                za.co.neroland.nerospace.world.planet.WeatherEvent.DUST_STORM, false, 1.0, true));
        assertFalse(WeatherSurvivalRules.trialActive(
                za.co.neroland.nerospace.world.planet.WeatherEvent.DUST_STORM, false, 1.0, false));
        assertFalse(WeatherSurvivalRules.trialActive(
                za.co.neroland.nerospace.world.planet.WeatherEvent.NONE, false, 0.0, true));
        assertTrue(WeatherSurvivalRules.trialActive(
                za.co.neroland.nerospace.world.planet.WeatherEvent.NONE, true, 0.01, true));
        assertFalse(WeatherSurvivalRules.trialActive(
                za.co.neroland.nerospace.world.planet.WeatherEvent.NONE, true, 0.5, true));
        assertFalse(WeatherSurvivalRules.survived(0, WeatherSurvivalRules.SURVIVE_TICKS - 1));
        assertTrue(WeatherSurvivalRules.survived(0, WeatherSurvivalRules.SURVIVE_TICKS));
    }
}
