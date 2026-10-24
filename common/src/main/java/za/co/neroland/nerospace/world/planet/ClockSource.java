package za.co.neroland.nerospace.world.planet;

/** Where a planet's clock phase comes from. Persisted nowhere; part of the planet definition. */
public enum ClockSource {
    /** Nerospace's own server-authoritative {@code PlanetClock} (vanilla time stays frozen). */
    PLANET_CLOCK,
    /** A mirror of the level's vanilla day time (Greenxertz keeps its real overworld-style day). */
    LEVEL_TIME
}
