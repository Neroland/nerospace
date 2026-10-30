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

    private static final List<String> WORLDS_ORDER = List.of("worlds_luna", "worlds_greenxertz", "worlds_mars",
            "worlds_cindara", "worlds_glacira", "worlds_nyxara");

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
    @DisplayName("Worlds chapters are appended from index 10; the mask fits a data slot")
    void worldsAppended() {
        assertTrue(StarGuide.CHAPTER_COUNT <= 16, "CHAPTER_COUNT " + StarGuide.CHAPTER_COUNT);
        for (int i = 0; i < WORLDS_ORDER.size(); i++) {
            assertEquals(WORLDS_ORDER.get(i), StarGuide.CHAPTERS.get(10 + i).id());
        }
        for (StarGuide.Chapter chapter : StarGuide.CHAPTERS) {
            assertTrue(chapter.steps().size() <= 16, chapter.id());
        }
    }

    @Test
    @DisplayName("worlds_luna: arrival first (luna), the hidden big_cheese step last and the only hidden one")
    void lunaChapter() {
        StarGuide.Chapter luna = StarGuide.CHAPTERS.get(10);
        List<StarGuide.Step> steps = luna.steps();
        assertEquals("luna", steps.get(0).id());
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
        }
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
