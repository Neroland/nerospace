package za.co.neroland.nerospace.route;

import java.util.List;
import java.util.Optional;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.api.NerospacePlanets;
import za.co.neroland.nerospace.api.PlanetId;
import za.co.neroland.nerospace.api.route.RouteQuote;
import za.co.neroland.nerospace.config.NerospaceConfig;

/**
 * The cargo-rocket cost model — travel time and fuel as functions of the planet registry, in one place
 * with unit tests, so packs and other mods (NeroLogistics quoting, NeroEvents weather) can reason about
 * them. See {@code docs/CARGO-ROCKETS.md} §4.
 *
 * <pre>
 * travelTicks = TIME_BASE + (sameDim ? 0 : TIME_CROSS_DIM) + TIME_PER_G * (gOrigin + gDest) / 2
 *               clamped to [MIN_TRAVEL, MAX_TRAVEL], then × cargoTravelTimeMultiplier
 * massUnits   = ceil(total items / 64)
 * fuelMb(leg) = round((FUEL_BASE + FUEL_PER_UNIT * massUnits) * max(0.1, gOrigin)) × fuelCostMultiplier
 * </pre>
 *
 * <p>The raw functions take plain doubles so the tests need no server; the {@code *Scaled} variants apply
 * the live Core config values. Every result is an {@code int} ≥ 1.</p>
 */
public final class CargoFormulas {

    /** Ticks every flight takes before gravity is considered (one minute). */
    public static final int TIME_BASE = 1_200;
    /** Extra ticks for leaving one dimension for another (two minutes). */
    public static final int TIME_CROSS_DIM = 2_400;
    /** Ticks added per unit of average gravity between the endpoints. */
    public static final int TIME_PER_G = 1_800;
    public static final int MIN_TRAVEL = 600;
    public static final int MAX_TRAVEL = 24_000;

    /** Millibuckets any launch burns before payload is considered. */
    public static final int FUEL_BASE = 1_000;
    /** Millibuckets per {@link #massUnits mass unit} (one 64-item stack-equivalent). */
    public static final int FUEL_PER_UNIT = 100;
    /** Items per mass unit. */
    public static final int ITEMS_PER_MASS_UNIT = 64;
    /** Gravity floor so a near-weightless origin still costs something. */
    public static final double MIN_GRAVITY = 0.1D;

    private CargoFormulas() {
    }

    // --- Raw (unscaled) ---------------------------------------------------------------------

    /** Unscaled travel time in ticks. */
    public static int rawTravelTicks(double originGravity, double destinationGravity, boolean sameDimension) {
        double avg = (clampGravity(originGravity) + clampGravity(destinationGravity)) / 2.0D;
        long ticks = TIME_BASE + (sameDimension ? 0 : TIME_CROSS_DIM) + Math.round(TIME_PER_G * avg);
        return (int) Math.max(MIN_TRAVEL, Math.min(MAX_TRAVEL, ticks));
    }

    /** Unscaled fuel for one leg in millibuckets. */
    public static int rawFuelMb(int massUnits, double originGravity) {
        double g = Math.max(MIN_GRAVITY, clampGravity(originGravity));
        long fuel = Math.round((FUEL_BASE + (long) FUEL_PER_UNIT * Math.max(0, massUnits)) * g);
        return (int) Math.min(Integer.MAX_VALUE - 1, Math.max(1, fuel));
    }

    /** Payload mass in 64-item stack-equivalents (rounded up; an empty manifest is 0). */
    public static int massUnits(List<ItemStack> manifest) {
        long items = 0;
        if (manifest != null) {
            for (ItemStack stack : manifest) {
                if (stack != null && !stack.isEmpty()) {
                    items += stack.getCount();
                }
            }
        }
        return (int) ((items + ITEMS_PER_MASS_UNIT - 1) / ITEMS_PER_MASS_UNIT);
    }

    // --- Config-scaled --------------------------------------------------------------------

    /** Travel time in ticks after the server's {@code cargoTravelTimeMultiplier}. */
    public static int travelTicks(double originGravity, double destinationGravity, boolean sameDimension) {
        int raw = rawTravelTicks(originGravity, destinationGravity, sameDimension);
        return Math.max(1, (int) Math.round(raw * NerospaceConfig.cargoTravelTimeMultiplier()));
    }

    /** One leg's fuel after the server's {@code fuelCostMultiplier}. */
    public static int fuelMb(int massUnits, double originGravity) {
        return NerospaceConfig.scale(rawFuelMb(massUnits, originGravity), NerospaceConfig.fuelCostMultiplier());
    }

    /**
     * The full quote for a shipment: outbound time, outbound fuel plus (when {@code returnEmpty}) the
     * empty return leg priced from the destination's gravity.
     */
    public static RouteQuote quote(double originGravity, double destinationGravity, boolean sameDimension,
            List<ItemStack> manifest, boolean returnEmpty) {
        int mass = massUnits(manifest);
        int fuel = fuelMb(mass, originGravity);
        if (returnEmpty) {
            fuel = (int) Math.min(Integer.MAX_VALUE - 1, (long) fuel + fuelMb(0, destinationGravity));
        }
        return new RouteQuote(travelTicks(originGravity, destinationGravity, sameDimension), fuel,
                !sameDimension, mass);
    }

    // --- Planet registry lookup -----------------------------------------------------------

    /**
     * The gravity the formulas use for a dimension: the planet registry's default for a Nerospace body,
     * Earth-normal ({@code 1.0}) for the Overworld and any non-Nerospace dimension.
     */
    public static double gravityOf(ResourceKey<Level> dimension) {
        if (dimension == null) {
            return 1.0D;
        }
        Optional<PlanetId> planet = NerospacePlanets.byDimension(dimension);
        return planet.map(id -> NerospacePlanets.traits(id).defaultGravity()).orElse(1.0D);
    }

    private static double clampGravity(double g) {
        if (Double.isNaN(g) || Double.isInfinite(g)) {
            return 1.0D;
        }
        return Math.max(0.0D, Math.min(10.0D, g));
    }
}
