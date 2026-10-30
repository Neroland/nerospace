package za.co.neroland.nerospace.world.planet;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.api.TemperatureBand;

/**
 * Pure per-player hazard resolution. A position can carry several hazards at once (the Nyxara nightside is irradiated
 * <em>and</em> cold; a Mars dust storm irradiates a cold world). The hazard that bites a player is the first one, in
 * the fixed order {@link #ORDER}, that the player is not shielded from; {@link Hazard#NONE} when every present hazard
 * is covered. A player therefore needs every matching lining, and the HUD shows the hazard that would hurt them next.
 */
public final class HazardOrderRules {

    /** Fixed resolution order: radiation first, then heat, then cold. */
    public static final List<Hazard> ORDER = List.of(Hazard.RADIATION, Hazard.HEAT, Hazard.COLD);

    /**
     * The suit a player wears, as far as hazards care: nothing, a full Tier 1 set, a full set of Tier 2 class pieces
     * (Tier 2, Thermal or Cryo, mixed), or a full matching Thermal or Cryo set.
     */
    public enum SuitClass { NONE, T1, T2, THERMAL, CRYO }

    private HazardOrderRules() {
    }

    /**
     * Whether the cold here is frigid: a frigid world (Glacira) or a biome tagged colder than its planet (the Nyxara
     * Nightside, the Martian Polar Cap). Frigid cold needs the Cryo Suit (or a flaring Dawnband Stone); the milder
     * cold of a cold world (Mars) is kept out by any Tier 2 class suit.
     */
    public static boolean frigidCold(TemperatureBand band, boolean coldBiome) {
        return coldBiome || band == TemperatureBand.FRIGID;
    }

    /** Every hazard present: weather radiation, each matching biome hazard tag and the planet's trait hazard. */
    public static Set<Hazard> present(Hazard trait, boolean radiationBiome, boolean heatBiome, boolean coldBiome,
            boolean weatherRadiation) {
        Set<Hazard> out = EnumSet.noneOf(Hazard.class);
        if (weatherRadiation || radiationBiome) {
            out.add(Hazard.RADIATION);
        }
        if (heatBiome) {
            out.add(Hazard.HEAT);
        }
        if (coldBiome) {
            out.add(Hazard.COLD);
        }
        if (trait != null && trait != Hazard.NONE) {
            out.add(trait);
        }
        return out;
    }

    /**
     * The hazards a worn suit shields at a position: Thermal keeps out HEAT, Cryo keeps out COLD, any Tier 2 class
     * suit (Tier 2, Thermal or Cryo) keeps out COLD that is not {@linkplain #frigidCold frigid}, and a full Rad-Lined
     * suit keeps out RADIATION. A fresh mutable set (positional shields such as a flaring Dawnband are added by the
     * caller).
     */
    public static Set<Hazard> shields(SuitClass suit, boolean radLined, boolean frigidCold) {
        Set<Hazard> out = EnumSet.noneOf(Hazard.class);
        if (suit == SuitClass.THERMAL) {
            out.add(Hazard.HEAT);
        }
        if (suit == SuitClass.CRYO) {
            out.add(Hazard.COLD);
        }
        boolean tier2Class = suit == SuitClass.T2 || suit == SuitClass.THERMAL || suit == SuitClass.CRYO;
        if (tier2Class && !frigidCold) {
            out.add(Hazard.COLD);
        }
        if (radLined) {
            out.add(Hazard.RADIATION);
        }
        return out;
    }

    /**
     * The full per-player resolution: what bites a player wearing {@code suit} where {@code present} hazards meet.
     *
     * @param dawnbandWarmth a flaring Dawnband Stone within reach (keeps out any COLD, frigid or not)
     */
    public static Hazard resolve(Set<Hazard> present, SuitClass suit, boolean radLined, boolean frigidCold,
            boolean dawnbandWarmth) {
        Set<Hazard> shielded = shields(suit, radLined, frigidCold);
        Hazard first = firstUnshielded(present, shielded);
        if (first == Hazard.COLD && dawnbandWarmth) {
            shielded.add(Hazard.COLD);
            first = firstUnshielded(present, shielded);
        }
        return first;
    }

    /**
     * The first hazard in {@link #ORDER} that is present and not shielded, else {@link Hazard#NONE}.
     *
     * @param present  the hazards at the position (NONE entries are ignored)
     * @param shielded the hazards the player is protected from (NONE entries are ignored)
     */
    public static Hazard firstUnshielded(Collection<Hazard> present, Collection<Hazard> shielded) {
        for (Hazard h : ORDER) {
            if (present.contains(h) && !shielded.contains(h)) {
                return h;
            }
        }
        return Hazard.NONE;
    }
}
