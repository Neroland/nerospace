package za.co.neroland.nerospace.client.ui;

import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.api.TemperatureBand;
import za.co.neroland.nerospace.world.OxygenManager;
import za.co.neroland.nerospace.world.planet.HazardOrderRules;
import za.co.neroland.nerospace.world.planet.RadiationRules;
import za.co.neroland.nerospace.world.planet.WeatherEvent;

/**
 * Everything a {@link PlanetCard} shows, filled by the host screen from client state only — never from
 * the server directly — so the card reads the same on the Rocket screen, Star Guide and Station Console.
 *
 * @param requiredTier the rocket tier a {@link State#LOCKED_TIER} card names
 */
public record PlanetCardData(ResourceKey<Level> dimension, Component name, double gravity, Hazard hazard,
        OxygenManager.SuitTier requiredSuit, boolean needsRadLining, WeatherEvent weatherNow, float weatherIntensity,
        float clockPhase, boolean fixedClock, boolean hasDayLength, Optional<Component> chosenPad, State state,
        int requiredTier) {

    public enum State { AVAILABLE, SELECTED, LOCKED_TIER, LOCKED_GATE, HERE }

    public PlanetCardData {
        chosenPad = chosenPad == null ? Optional.empty() : chosenPad;
        hazard = hazard == null ? Hazard.NONE : hazard;
        weatherNow = weatherNow == null ? WeatherEvent.NONE : weatherNow;
        state = state == null ? State.AVAILABLE : state;
    }

    /** Pure state rule ({@code PlanetCardStateTest}): here beats gate beats tier beats selection. */
    public static State stateFor(boolean here, boolean gateLocked, boolean tierLocked, boolean selected) {
        if (here) {
            return State.HERE;
        }
        if (gateLocked) {
            return State.LOCKED_GATE;
        }
        if (tierLocked) {
            return State.LOCKED_TIER;
        }
        return selected ? State.SELECTED : State.AVAILABLE;
    }

    /**
     * The suit a planet asks for on its card: Tier 2 on a cold (not frigid) world, whose cold any Tier 2 class suit
     * keeps out; Tier 1 on any other airless world; none on a breathable one.
     */
    public static OxygenManager.SuitTier requiredSuit(boolean airless, Hazard hazard, TemperatureBand band) {
        if (hazard == Hazard.COLD && !HazardOrderRules.frigidCold(band, false)) {
            return OxygenManager.SuitTier.TIER_2;
        }
        return airless ? OxygenManager.SuitTier.TIER_1 : OxygenManager.SuitTier.NONE;
    }

    /**
     * Whether a planet's card asks for the Rad Lining: its radiation can hurt (ambient level 2 or more, or a
     * radiation world), or its weather raises it (Mars dust storms). Luna and the station never do.
     */
    public static boolean needsRadLining(int radiation, Hazard hazard, WeatherEvent planetWeather) {
        return radiation >= RadiationRules.DAMAGE_THRESHOLD || hazard == Hazard.RADIATION
                || planetWeather == WeatherEvent.DUST_STORM;
    }

    /** {@link #suitKey(boolean, Hazard, boolean, OxygenManager.SuitTier)} for a card with no Tier 2 requirement. */
    public static String suitKey(boolean airless, Hazard hazard, boolean needsRadLining) {
        return suitKey(airless, hazard, needsRadLining, OxygenManager.SuitTier.TIER_1);
    }

    /**
     * The suit line key: T1 / T2 / T2+Thermal / T2+Cryo / T2+Rad, or none for a breathable world. A COLD world whose
     * card requires {@code TIER_2} reads plain T2 (any Tier 2 class suit keeps that cold out).
     */
    public static String suitKey(boolean airless, Hazard hazard, boolean needsRadLining,
            OxygenManager.SuitTier requiredSuit) {
        if (needsRadLining || hazard == Hazard.RADIATION) {
            return "gui.nerospace.card.suit_rad";
        }
        if (hazard == Hazard.HEAT) {
            return "gui.nerospace.card.suit_thermal";
        }
        if (hazard == Hazard.COLD) {
            return requiredSuit == OxygenManager.SuitTier.TIER_2 ? "gui.nerospace.card.suit_t2"
                    : "gui.nerospace.card.suit_cryo";
        }
        return airless ? "gui.nerospace.card.suit_t1" : "gui.nerospace.card.suit_none";
    }
}
