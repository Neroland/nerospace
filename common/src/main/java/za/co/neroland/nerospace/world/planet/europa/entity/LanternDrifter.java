package za.co.neroland.nerospace.world.planet.europa.entity;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.entity.FaunaStats;

/**
 * Lantern Drifter (design 9.1): a glowing bell with four trailing ribbons, the small life of Europa's dark ocean and
 * of the terraformed sea. It swims in slow squid-like pulses every {@link EuropaFaunaRules#PULSE_MIN}-
 * {@link EuropaFaunaRules#PULSE_MAX} ticks and coasts between them; each pulse rises toward block light of 7 or
 * more (lamps, active vents), holds level where it is lit and sinks otherwise, and leans toward the nearest other
 * drifter within 8 blocks, so drifters gather loosely. It flees nothing and attacks nothing. Native fauna; it
 * stores nothing about any player. Sounds reuse the vanilla glow squid.
 */
public class LanternDrifter extends EuropaSwimmer {

    private int pulseIn;

    public LanternDrifter(EntityType<? extends LanternDrifter> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, FaunaStats.DRIFTER_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, FaunaStats.DRIFTER_SPEED);
    }

    @Override
    protected Vec3 swim(Vec3 velocity) {
        if (--this.pulseIn > 0) {
            return velocity.scale(EuropaFaunaRules.WATER_DRAG);
        }
        this.pulseIn = EuropaFaunaRules.pulseInterval(
                this.random.nextInt(EuropaFaunaRules.PULSE_MAX - EuropaFaunaRules.PULSE_MIN + 1));
        BlockPos here = this.blockPosition();
        Level level = this.level();
        int lightAbove = level.getBrightness(LightLayer.BLOCK, here.above(2));
        int lightHere = level.getBrightness(LightLayer.BLOCK, here);
        double lift = EuropaFaunaRules.drifterLift(lightAbove, lightHere);
        LanternDrifter mate = this.nearestMate();
        double[] dir = EuropaFaunaRules.pulseDirection(this.random.nextDouble() * Math.PI * 2.0D, mate != null,
                mate == null ? 0.0D : mate.getX() - this.getX(), mate == null ? 0.0D : mate.getZ() - this.getZ());
        double s = EuropaFaunaRules.PULSE_SPEED;
        return new Vec3(dir[0] * s, lift, dir[1] * s);
    }

    /** The nearest other drifter within {@link EuropaFaunaRules#SCHOOL_RADIUS}, read once per pulse. */
    private LanternDrifter nearestMate() {
        List<LanternDrifter> near = this.level().getEntitiesOfClass(LanternDrifter.class,
                this.getBoundingBox().inflate(EuropaFaunaRules.SCHOOL_RADIUS), e -> e != this && e.isAlive());
        LanternDrifter best = null;
        double bestD = EuropaFaunaRules.SCHOOL_RADIUS * EuropaFaunaRules.SCHOOL_RADIUS;
        for (LanternDrifter d : near) {
            double dist = d.distanceToSqr(this);
            if (dist < bestD) {
                bestD = dist;
                best = d;
            }
        }
        return best;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GLOW_SQUID_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.GLOW_SQUID_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GLOW_SQUID_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }
}
