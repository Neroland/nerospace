package za.co.neroland.nerospace.suit;

import za.co.neroland.nerospace.api.TemperatureBand;

/**
 * Shared flight step. The server and the client both call {@link #step}; the server clamps the result to
 * {@code maxSpeed}. Zero gravity hands the suit to EVA rules, which this release does not fly.
 */
public final class SuitFlightMath {

    public enum Mode {
        GROUNDED, HOVER, CRUISE, BOOST, FAILSAFE, EVA_HANDOVER
    }

    public record Input(boolean jump, boolean sneak, boolean sprint, boolean brake, boolean engage,
            boolean airborne, boolean inCombatLock, boolean allowed) {
    }

    public record Step(Mode mode, double vx, double vy, double vz, int heat, boolean descending) {
    }

    /** Gravity at or below this is zero-G and belongs to Milestone B's EVA thrusters. */
    public static final double ZERO_G = 0.05D;

    private SuitFlightMath() {
    }

    public static boolean zeroG(double gravity) {
        return gravity < ZERO_G;
    }

    /**
     * Heat gained this tick of thrust, before dissipation. Hot worlds build heat faster; frigid worlds slower.
     */
    public static int heatGain(TemperatureBand band) {
        if (band == null) {
            return 2;
        }
        return switch (band) {
            case FRIGID -> 1;
            case COLD -> 1;
            case TEMPERATE -> 2;
            case HOT -> 3;
            case INFERNAL -> 4;
        };
    }

    /** Heat lost per tick while not thrusting. */
    public static int heatLoss(TemperatureBand band) {
        if (band == null) {
            return 1;
        }
        return switch (band) {
            case FRIGID -> 3;
            case COLD -> 2;
            case TEMPERATE -> 1;
            case HOT -> 1;
            case INFERNAL -> 1;
        };
    }

    public static int nextHeat(int heat, boolean thrusting, TemperatureBand band) {
        int next = heat + (thrusting ? heatGain(band) : -heatLoss(band));
        return Math.max(0, Math.min(100, next));
    }

    /**
     * One tick of flight. {@code lookX/Z} is the horizontal look vector (not necessarily unit length).
     * Speed is clamped to {@code maxSpeed} (boost is 1.6×). Hover cannot climb once the burst is spent.
     */
    public static Step step(Input input, Mode current, double lookX, double lookZ, double gravity, int heat,
            int burstTicks, int maxBurst, double maxSpeed, boolean energyAboveReserve, TemperatureBand band) {
        if (zeroG(gravity)) {
            return new Step(Mode.EVA_HANDOVER, 0, 0, 0, nextHeat(heat, false, band), false);
        }
        if (!input.allowed() || input.inCombatLock() || !energyAboveReserve) {
            if (input.airborne() && current != Mode.GROUNDED) {
                return descend(heat, band);
            }
            return new Step(Mode.GROUNDED, 0, 0, 0, nextHeat(heat, false, band), false);
        }
        if (input.brake() || !input.airborne() && current == Mode.GROUNDED && !input.engage()) {
            return new Step(Mode.GROUNDED, 0, 0, 0, nextHeat(heat, false, band), false);
        }
        boolean hover = current == Mode.HOVER || (input.jump() && current != Mode.CRUISE && current != Mode.BOOST);
        if (current == Mode.HOVER || (hover && current != Mode.CRUISE && current != Mode.BOOST && !input.engage())) {
            boolean thrusting = input.jump() && burstTicks < maxBurst && heat < 100;
            double climb = thrusting ? 0.12D * Math.max(SuitEnergyMath.GRAVITY_FLOOR, Math.min(1.0D, gravity)) : 0.0D;
            // A spent burst holds altitude for the rest of the tick, then the caller drops back to grounded.
            return new Step(Mode.HOVER, 0, climb, 0, nextHeat(heat, thrusting, band), false);
        }
        if (!input.engage() && current != Mode.CRUISE && current != Mode.BOOST) {
            return new Step(input.airborne() ? Mode.FAILSAFE : Mode.GROUNDED, 0, input.airborne() ? -0.08D : 0, 0,
                    nextHeat(heat, false, band), input.airborne());
        }
        double speed = maxSpeed * (input.sprint() ? 1.6D : 1.0D);
        double len = Math.hypot(lookX, lookZ);
        double vx = len < 1.0E-4 ? 0 : lookX / len * speed;
        double vz = len < 1.0E-4 ? 0 : lookZ / len * speed;
        double vy = 0;
        if (input.jump()) {
            vy = 0.18D * Math.max(SuitEnergyMath.GRAVITY_FLOOR, gravity);
        } else if (input.sneak()) {
            vy = -0.18D;
        }
        Mode mode = input.sprint() ? Mode.BOOST : Mode.CRUISE;
        return new Step(mode, clamp(vx, speed), clamp(vy, speed), clamp(vz, speed), nextHeat(heat, true, band), false);
    }

    private static Step descend(int heat, TemperatureBand band) {
        return new Step(Mode.FAILSAFE, 0, -0.08D, 0, nextHeat(heat, false, band), true);
    }

    public static double clamp(double value, double max) {
        if (value > max) {
            return max;
        }
        if (value < -max) {
            return -max;
        }
        return value;
    }
}
