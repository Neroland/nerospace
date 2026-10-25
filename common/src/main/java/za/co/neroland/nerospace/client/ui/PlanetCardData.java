package za.co.neroland.nerospace.client.ui;

import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.world.OxygenManager;
import za.co.neroland.nerospace.world.planet.WeatherEvent;

/**
 * Everything a {@link PlanetCard} shows (004 §5), filled by the host screen from client state only — never from
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

    /** Pure state rule (004 §5, {@code PlanetCardStateTest}): here beats gate beats tier beats selection. */
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

    /** The suit line key: T1 / T2 / T2+Thermal / T2+Cryo / T2+Rad, or none for a breathable world. */
    public static String suitKey(boolean airless, Hazard hazard, boolean needsRadLining) {
        if (needsRadLining || hazard == Hazard.RADIATION) {
            return "gui.nerospace.card.suit_rad";
        }
        if (hazard == Hazard.HEAT) {
            return "gui.nerospace.card.suit_thermal";
        }
        if (hazard == Hazard.COLD) {
            return "gui.nerospace.card.suit_cryo";
        }
        return airless ? "gui.nerospace.card.suit_t1" : "gui.nerospace.card.suit_none";
    }
}
