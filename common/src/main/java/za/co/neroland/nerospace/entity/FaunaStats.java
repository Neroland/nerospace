package za.co.neroland.nerospace.entity;

/**
 * Plain numbers for the terraform fauna: Moon Hopper (Luna), Dust Strider (Mars) and Dusk Moth
 * (Nyxara). Kept free of Minecraft types so the attribute sanity test can read them without a registry bootstrap;
 * each species' {@code createAttributes()} builds its attribute supplier from these.
 */
public final class FaunaStats {

    /** Moon Hopper: small, quick, light on its feet; hops every {@link #HOPPER_HOP_INTERVAL} ticks while moving. */
    public static final double HOPPER_HEALTH = 6.0D;
    public static final double HOPPER_SPEED = 0.3D;
    public static final int HOPPER_HOP_INTERVAL = 12;

    /** Dust Strider: a long-legged herd grazer, sturdier and a little slower. */
    public static final double STRIDER_HEALTH = 14.0D;
    public static final double STRIDER_SPEED = 0.22D;

    /** Dusk Moth: fragile ambient flier; spawns only where the light level is at most {@link #MOTH_MAX_LIGHT}. */
    public static final double MOTH_HEALTH = 4.0D;
    public static final double MOTH_SPEED = 0.25D;
    public static final double MOTH_FLYING_SPEED = 0.5D;
    public static final int MOTH_MAX_LIGHT = 7;

    private FaunaStats() {
    }

    /** True when a Dusk Moth may spawn at this block-light level (0..15; sky light is not counted). */
    public static boolean mothMaySpawnAt(int light) {
        return light >= 0 && light <= MOTH_MAX_LIGHT;
    }

    /** True when a moving, grounded Moon Hopper should hop on this tick. */
    public static boolean hopperHopsNow(boolean onGround, boolean moving, int tickCount) {
        return onGround && moving && tickCount % HOPPER_HOP_INTERVAL == 0;
    }
}
