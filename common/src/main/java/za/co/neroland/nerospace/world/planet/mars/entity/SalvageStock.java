package za.co.neroland.nerospace.world.planet.mars.entity;

/**
 * The Salvage Drone's restock rule, kept free of game classes so it can be tested on its own. A drone's stock is
 * one shared price list whose {@code maxUses} are real: uses persist with the entity and the whole list is rebuilt
 * (every row back to full) once per day, checked when a trade window opens.
 */
public final class SalvageStock {

    /** One restock per Minecraft day. */
    public static final long RESTOCK_TICKS = 24_000L;

    /** Marker for "never stocked" (a fresh drone, or one saved before stock was persisted). */
    public static final long NEVER = -1L;

    private SalvageStock() {
    }

    /**
     * Whether the stock must be rebuilt now: never stocked, a day or more since the last restock, or the clock went
     * backwards (a world whose game time was reset by a command).
     */
    public static boolean due(long now, long lastRestock) {
        return lastRestock < 0 || now < lastRestock || now - lastRestock >= RESTOCK_TICKS;
    }
}
