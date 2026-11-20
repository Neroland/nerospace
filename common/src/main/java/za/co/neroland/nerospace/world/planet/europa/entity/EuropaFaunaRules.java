package za.co.neroland.nerospace.world.planet.europa.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure numbers and rules for Europa's three creatures (design 9.1-9.3), free of Minecraft types so they are
 * unit-tested headless. The entity classes read every constant from here.
 */
public final class EuropaFaunaRules {

    // --- Lantern Drifter --------------------------------------------------------------------------
    /** Shortest and longest gap between two swim pulses (ticks). */
    public static final int PULSE_MIN = 30;
    public static final int PULSE_MAX = 50;
    /** Speed of one pulse (blocks per tick); the drift decays by {@link #WATER_DRAG} every tick after it. */
    public static final double PULSE_SPEED = 0.16D;
    /** Velocity kept per tick while coasting in water. */
    public static final double WATER_DRAG = 0.9D;
    /** Block light at or above this level, just above the drifter, makes it rise (lamps and active vents give 7+). */
    public static final int RISE_LIGHT = 7;
    /** Vertical part of a pulse: up toward light, level where it is already lit, down otherwise. */
    public static final double RISE = 0.08D;
    public static final double SINK = -0.04D;
    /** A drifter schools with the nearest other drifter within this many blocks. */
    public static final double SCHOOL_RADIUS = 8.0D;
    /** Share of the horizontal pulse that points at the school mate (the rest is random). */
    public static final double SCHOOL_PULL = 0.6D;
    /** Ticks a stranded water creature lasts before the vanilla air rules start drowning it. */
    public static final int DRY_GRACE_TICKS = 200;

    // --- Vent Ray -----------------------------------------------------------------------------------
    /** Ticks between two vent searches; the found vent is cached in between. */
    public static final int RETARGET_TICKS = 200;
    /** Search box half-sizes (25 x 9 x 25) and the most block reads one search makes. */
    public static final int SCAN_XZ = 12;
    public static final int SCAN_Y = 4;
    public static final int SCAN_MAX_READS = 400;
    /** The ray circles its vent at this radius, and never strays further than {@link #LEASH} while it has one. */
    public static final double CIRCLE_RADIUS = 7.0D;
    public static final double LEASH = 12.0D;
    /** Angle the ray moves along its circle per tick (radians): one lap in about 30 seconds. */
    public static final double CIRCLE_STEP = Math.PI * 2.0D / 600.0D;
    /** Cruising and fleeing speed (blocks per tick), and how quickly the velocity turns toward the wanted one. */
    public static final double CRUISE = 0.10D;
    public static final double FLEE_SPEED = 0.35D;
    public static final double STEER = 0.1D;
    /** A player closer than this makes the ray flick away, {@link #FLEE_DISTANCE} blocks. */
    public static final double FLEE_TRIGGER = 3.0D;
    public static final double FLEE_DISTANCE = 6.0D;
    /** Rest on the floor beside a vent: length range (ticks) and the chance per tick of starting one. */
    public static final int SETTLE_MIN = 100;
    public static final int SETTLE_MAX = 300;
    public static final double SETTLE_CHANCE = 1.0D / 400.0D;
    /** The ray settles only this close to its vent (blocks). */
    public static final double SETTLE_REACH = 4.0D;
    /** Natural spawns only below this Y. */
    public static final int RAY_MAX_SPAWN_Y = 0;

    // --- Shell Skater -----------------------------------------------------------------------------
    /** Share of last tick's horizontal speed a skater keeps on ice, and the fastest glide. */
    public static final double GLIDE_RETAIN = 0.92D;
    public static final double MAX_GLIDE = 0.3D;
    /** Salt licking: pause between tries, blocks sampled per try, reach, ticks spent licking. */
    public static final int LICK_COOLDOWN_MIN = 200;
    public static final int LICK_COOLDOWN_MAX = 400;
    public static final int LICK_TRIES = 48;
    public static final int LICK_RADIUS = 6;
    public static final int LICK_TICKS = 60;
    /** Walk-target value of a cell holding (or standing on) a penitente blade: strongly avoided. */
    public static final float BLADE_AVOIDANCE = -10.0F;

    private static final List<int[]> SCAN = buildScan();

    private EuropaFaunaRules() {
    }

    /** Gap before the next pulse for a roll in {@code 0..PULSE_MAX-PULSE_MIN} (clamped). */
    public static int pulseInterval(int roll) {
        return PULSE_MIN + Math.max(0, Math.min(PULSE_MAX - PULSE_MIN, roll));
    }

    /** Vertical part of a drifter pulse from the block light just above it and where it is. */
    public static double drifterLift(int lightAbove, int lightHere) {
        if (lightAbove >= RISE_LIGHT) {
            return RISE;
        }
        return lightHere >= RISE_LIGHT ? 0.0D : SINK;
    }

    /**
     * Horizontal direction of a pulse: the random unit direction blended with the unit direction to the school
     * mate ({@code hasMate} false: the random one). Returns a unit vector {x, z}, or the random one when the blend
     * cancels out.
     */
    public static double[] pulseDirection(double randomAngle, boolean hasMate, double toMateX, double toMateZ) {
        double rx = Math.cos(randomAngle);
        double rz = Math.sin(randomAngle);
        if (!hasMate) {
            return new double[] {rx, rz};
        }
        double len = Math.sqrt(toMateX * toMateX + toMateZ * toMateZ);
        if (len < 1.0E-6D) {
            return new double[] {rx, rz};
        }
        double x = rx * (1.0D - SCHOOL_PULL) + toMateX / len * SCHOOL_PULL;
        double z = rz * (1.0D - SCHOOL_PULL) + toMateZ / len * SCHOOL_PULL;
        double l = Math.sqrt(x * x + z * z);
        return l < 1.0E-6D ? new double[] {rx, rz} : new double[] {x / l, z / l};
    }

    /** Air left to a water creature that has just been stranded: never more than the grace. */
    public static int strandedAir(int air) {
        return Math.min(air, DRY_GRACE_TICKS);
    }

    /**
     * The vent search order: offsets {dx, dy, dz} inside the 25 x 9 x 25 box, nearest first, the first
     * {@link #SCAN_MAX_READS} only. Shared and unmodifiable.
     */
    public static List<int[]> scanOffsets() {
        return SCAN;
    }

    private static List<int[]> buildScan() {
        List<int[]> all = new ArrayList<>();
        for (int dx = -SCAN_XZ; dx <= SCAN_XZ; dx++) {
            for (int dy = -SCAN_Y; dy <= SCAN_Y; dy++) {
                for (int dz = -SCAN_XZ; dz <= SCAN_XZ; dz++) {
                    all.add(new int[] {dx, dy, dz});
                }
            }
        }
        // Stable order: distance, then y, x, z, so the list is the same on every run.
        all.sort((a, b) -> {
            int d = Integer.compare(a[0] * a[0] + a[1] * a[1] + a[2] * a[2], b[0] * b[0] + b[1] * b[1] + b[2] * b[2]);
            if (d != 0) {
                return d;
            }
            d = Integer.compare(a[1], b[1]);
            if (d != 0) {
                return d;
            }
            d = Integer.compare(a[0], b[0]);
            return d != 0 ? d : Integer.compare(a[2], b[2]);
        });
        return Collections.unmodifiableList(new ArrayList<>(all.subList(0, SCAN_MAX_READS)));
    }

    /** Whether a player this far away (squared blocks) sends the ray off. */
    public static boolean rayFlees(double distanceSq) {
        return distanceSq < FLEE_TRIGGER * FLEE_TRIGGER;
    }

    /** Point {x, z} on the ray's circle around a vent centre at {@code angle}. */
    public static double[] circlePoint(double centreX, double centreZ, double angle) {
        return new double[] {centreX + Math.cos(angle) * CIRCLE_RADIUS, centreZ + Math.sin(angle) * CIRCLE_RADIUS};
    }

    /** Settle length for a roll in {@code 0..SETTLE_MAX-SETTLE_MIN} (clamped). */
    public static int settleTicks(int roll) {
        return SETTLE_MIN + Math.max(0, Math.min(SETTLE_MAX - SETTLE_MIN, roll));
    }

    /** One steering step: the velocity component moved {@link #STEER} of the way to the wanted one. */
    public static double steer(double current, double wanted) {
        return current + (wanted - current) * STEER;
    }

    /** Horizontal speed of a skater on ice after a tick: it keeps most of last tick's speed, capped. */
    public static double glide(double previousSpeed, double speedNow) {
        double kept = Math.max(speedNow, previousSpeed * GLIDE_RETAIN);
        return Math.min(kept, Math.max(speedNow, MAX_GLIDE));
    }

    /** Pause before the next lick try for a roll in {@code 0..LICK_COOLDOWN_MAX-LICK_COOLDOWN_MIN} (clamped). */
    public static int lickCooldown(int roll) {
        return LICK_COOLDOWN_MIN + Math.max(0, Math.min(LICK_COOLDOWN_MAX - LICK_COOLDOWN_MIN, roll));
    }
}
