package za.co.neroland.nerospace.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.config.ConfigSchema;
import za.co.neroland.nerolandcore.config.ConfigValue;

/**
 * The 24 1.x keys keep their names and defaults (an old nerospace.properties loads
 * unchanged), and every 2.0 key exists with its stated default and is server-authoritative.
 */
class ConfigSchemaCompatTest {

    private static Map<String, String> legacy() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("telemetryEnabled", "true");
        m.put("energyRateMultiplier", "1.0");
        m.put("oxygenDrainMultiplier", "1.0");
        m.put("oxygenCapacityMultiplier", "1.0");
        m.put("fuelCostMultiplier", "1.0");
        m.put("machineSpeedMultiplier", "1.0");
        m.put("gravityMultiplier", "1.0");
        m.put("quarryMaxSide", "64");
        m.put("quarryFrameDecayTicks", "600");
        m.put("alienRaidsEnabled", "true");
        m.put("terraformerForceLoadEnabled", "false");
        m.put("linkModuleEnabled", "true");
        m.put("welcomeMessageEnabled", "true");
        m.put("cargoPadSlots", "18");
        m.put("cargoFuelCapacity", "16000");
        m.put("cargoTravelTimeMultiplier", "1.0");
        m.put("cargoHoldRetrySeconds", "30");
        m.put("cargoHoldTimeoutMinutes", "30");
        m.put("cargoFlightRetentionDays", "7");
        m.put("cargoMaxFlightsPerOwner", "16");
        m.put("cargoMaxPads", "256");
        m.put("economyFirstVisitReward", "0");
        m.put("economyLaunchFee", "0");
        m.put("economyDockingFee", "0");
        return m;
    }

    private static Map<String, String> added() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("blocks.nyxStoneLightAbsorb", "true");
        m.put("blocks.methaneIceBreaksBlocks", "false");
        m.put("blocks.sporeMossCap", "48");
        m.put("blocks.shadowLichenCap", "32");
        m.put("blocks.regolithDriftCap", "16");
        m.put("blocks.ashLayerCap", "4");
        m.put("blocks.frostCrystalCap", "64");
        m.put("blocks.dawnbandCap", "256");
        m.put("blocks.gravityOverrideCap", "64");
        m.put("blocks.methaneChainCap", "4");
        m.put("weather.emberStrikeIntervalTicks", "100");
        m.put("weather.enabled", "true");
        m.put("weather.frequencyMultiplier", "1.0");
        m.put("weather.durationMultiplier", "1.0");
        m.put("weather.particleBudget", "1000");
        m.put("weather.greenxertz.enabled", "true");
        m.put("weather.mars.enabled", "true");
        m.put("weather.cindara.enabled", "true");
        m.put("weather.glacira.enabled", "true");
        m.put("weather.nyxara.enabled", "true");
        m.put("clock.enabled", "true");
        m.put("solar.clockDriven", "true");
        m.put("landing.podSearchRadius", "24");
        m.put("stations.maxMembers", "16");
        m.put("stations.maxModules", "8");
        m.put("nyxara.twilightBandWidth", "2048");
        return m;
    }

    @Test
    @DisplayName("the 24 1.x keys keep names and defaults, and come first in the file")
    void legacyKeys() {
        ConfigSchema schema = NerospaceConfig.schema();
        Map<String, String> legacy = legacy();
        assertEquals(24, legacy.size());
        for (Map.Entry<String, String> e : legacy.entrySet()) {
            ConfigValue<?> v = schema.value(e.getKey());
            assertNotNull(v, "missing 1.x key " + e.getKey());
            assertEquals(e.getValue(), v.defaultAsString(), "default changed for " + e.getKey());
        }
        assertEquals(legacy.keySet().stream().toList(),
                schema.values().stream().map(ConfigValue::key).limit(24).toList());
    }

    @Test
    @DisplayName("every 2.0 key exists with its default and is server-authoritative")
    void addedKeys() {
        ConfigSchema schema = NerospaceConfig.schema();
        for (Map.Entry<String, String> e : added().entrySet()) {
            ConfigValue<?> v = schema.value(e.getKey());
            assertNotNull(v, "missing 2.0 key " + e.getKey());
            assertEquals(e.getValue(), v.defaultAsString(), "default for " + e.getKey());
            assertTrue(v.serverAuthoritative(), e.getKey() + " must be server-authoritative");
        }
    }

    @Test
    @DisplayName("the ambient particle, HUD and sky keys are client-side (never forced by a server)")
    void particleKeysAreClientSide() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("particles.enabled", "true");
        m.put("particles.dust.perPlayerPerTick", "0.6");
        m.put("particles.ash.perPlayerPerTick", "0.5");
        m.put("particles.spore.perPlayerPerTick", "0.4");
        m.put("particles.diamond_dust.perPlayerPerTick", "0.8");
        m.put("particles.ember.perPlayerPerTick", "0.2");
        m.put("particles.nebula_mote.perPlayerPerTick", "0.3");
        m.put("hud.clockDial", "true");
        m.put("hud.clockDialScale", "1");
        m.put("hud.weatherLine", "true");
        m.put("sky.aurora", "true");
        m.put("sky.shootingStars", "true");
        m.put("sky.quality", "full");
        for (Map.Entry<String, String> e : m.entrySet()) {
            ConfigValue<?> v = NerospaceConfig.schema().value(e.getKey());
            assertNotNull(v, "missing key " + e.getKey());
            assertEquals(e.getValue(), v.defaultAsString(), "default for " + e.getKey());
            assertTrue(!v.serverAuthoritative(), e.getKey() + " must stay client-side");
        }
    }

    @Test
    @DisplayName("every planet with weather has its weather.<planet>.enabled key")
    void weatherKeysCoverPlanets() {
        for (za.co.neroland.nerospace.world.planet.PlanetDefinition p : za.co.neroland.nerospace.world.planet.Planets.ALL) {
            if (p.weather().hasWeather()) {
                assertNotNull(NerospaceConfig.schema().value(p.weatherConfigKey()), p.weatherConfigKey());
            }
        }
    }
}
