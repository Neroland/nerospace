package za.co.neroland.nerospace.machine;

import za.co.neroland.nerospace.world.planet.europa.EuropaNumbers;

/** Burn times of the Combustion Generator's fuels, in ticks (no Minecraft types, JUnit-tested). */
public final class GeneratorFuels {

    public static final int COAL = 1_600;
    public static final int COAL_BLOCK = 16_000;
    public static final int BLAZE_ROD = 2_400;
    public static final int ROCKET_FUEL_CANISTER = 4_000;
    /** 1.5x coal: methane is Glacira's fuel. */
    public static final int METHANE_CRYSTAL = 2_400;
    /** Europa's fuel: twice a canister. */
    public static final int DEUTERIUM_CELL = EuropaNumbers.DEUTERIUM_CELL_BURN_TICKS;

    private GeneratorFuels() {
    }
}
