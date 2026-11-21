package za.co.neroland.nerospace.world.planet.europa.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.entity.FaunaStats;
import za.co.neroland.nerospace.world.planet.europa.EuropaContent;
import za.co.neroland.nerospace.world.planet.europa.block.HydrothermalVentBlock;

/**
 * Vent Ray (design 9.2): a flat manta of the ocean floor. Every {@link EuropaFaunaRules#RETARGET_TICKS} ticks it
 * keeps its vent if that is still loaded, active and within {@link EuropaFaunaRules#LEASH}; otherwise it looks for
 * the nearest active hydrothermal vent with one bounded search (at most {@link EuropaFaunaRules#SCAN_MAX_READS}
 * block reads on a lattice spanning the 25 x 9 x 25 box, shifted every search, loaded chunks only) and keeps that
 * position; it then glides in wide circles around it. On or inside its circle it sometimes settles on the floor for
 * 100-300 ticks.
 * A player within 3 blocks makes it flick 6 blocks away. With no vent it wanders. Never hostile; stores nothing
 * about any player (the vent position is not saved). Sounds reuse the vanilla tropical fish.
 */
public class VentRay extends EuropaSwimmer {

    private BlockPos vent;
    private int retargetIn;
    /** Which lattice the next search reads (memory only). */
    private int scanPass;
    private int settleTicks;
    private double angle;
    private Vec3 fleeTarget;
    private int wanderIn;
    private Vec3 wander = Vec3.ZERO;

    public VentRay(EntityType<? extends VentRay> type, Level level) {
        super(type, level);
        this.angle = this.random.nextDouble() * Math.PI * 2.0D;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, FaunaStats.RAY_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, FaunaStats.RAY_SPEED);
    }

    @Override
    protected Vec3 swim(Vec3 velocity) {
        if (this.tickCount % 5 == 0) {
            this.checkThreat();
        }
        if (this.fleeTarget != null) {
            Vec3 to = this.fleeTarget.subtract(this.position());
            if (to.lengthSqr() < 1.0D) {
                this.fleeTarget = null;
            } else {
                return toward(velocity, to.normalize().scale(EuropaFaunaRules.FLEE_SPEED));
            }
        }
        if (this.settleTicks > 0) {
            this.settleTicks--;
            // Rest on the floor: no sideways motion, a slow sink until something holds it.
            return new Vec3(velocity.x * 0.5D, Math.max(velocity.y - 0.01D, -0.05D), velocity.z * 0.5D);
        }
        if (--this.retargetIn <= 0) {
            this.retargetIn = EuropaFaunaRules.RETARGET_TICKS;
            if (!this.ventStillHolds()) {
                this.vent = this.findVent();
            }
        }
        if (this.vent != null) {
            double dx = this.vent.getX() + 0.5D - this.getX();
            double dz = this.vent.getZ() + 0.5D - this.getZ();
            double distSq = dx * dx + dz * dz;
            if (distSq < EuropaFaunaRules.SETTLE_REACH * EuropaFaunaRules.SETTLE_REACH
                    && this.random.nextDouble() < EuropaFaunaRules.SETTLE_CHANCE) {
                this.settleTicks = EuropaFaunaRules.settleTicks(
                        this.random.nextInt(EuropaFaunaRules.SETTLE_MAX - EuropaFaunaRules.SETTLE_MIN + 1));
                return velocity.scale(0.5D);
            }
            this.angle += EuropaFaunaRules.CIRCLE_STEP;
            double[] p = EuropaFaunaRules.circlePoint(this.vent.getX() + 0.5D, this.vent.getZ() + 0.5D, this.angle);
            double y = this.vent.getY() + 2.5D;
            Vec3 to = new Vec3(p[0] - this.getX(), y - this.getY(), p[1] - this.getZ());
            if (to.lengthSqr() < 1.0E-4D) {
                return velocity.scale(EuropaFaunaRules.WATER_DRAG);
            }
            return toward(velocity, to.normalize().scale(EuropaFaunaRules.CRUISE));
        }
        if (--this.wanderIn <= 0) {
            this.wanderIn = 60 + this.random.nextInt(60);
            double a = this.random.nextDouble() * Math.PI * 2.0D;
            this.wander = new Vec3(Math.cos(a), (this.random.nextDouble() - 0.5D) * 0.4D, Math.sin(a))
                    .normalize().scale(EuropaFaunaRules.CRUISE * 0.6D);
        }
        return toward(velocity, this.wander);
    }

    private static Vec3 toward(Vec3 v, Vec3 wanted) {
        return new Vec3(EuropaFaunaRules.steer(v.x, wanted.x), EuropaFaunaRules.steer(v.y, wanted.y),
                EuropaFaunaRules.steer(v.z, wanted.z));
    }

    /** A player within 3 blocks: settle ends and the ray darts 6 blocks directly away. */
    private void checkThreat() {
        Player p = this.level().getNearestPlayer(this, EuropaFaunaRules.FLEE_TRIGGER);
        if (p == null || !EuropaFaunaRules.rayFlees(p.distanceToSqr(this))) {
            return;
        }
        Vec3 away = this.position().subtract(p.position());
        if (away.lengthSqr() < 1.0E-4D) {
            away = new Vec3(1.0D, 0.0D, 0.0D);
        }
        this.settleTicks = 0;
        this.fleeTarget = this.position().add(away.normalize().scale(EuropaFaunaRules.FLEE_DISTANCE));
    }

    /** The cached vent is kept while it is loaded, still active and within the leash (one block read). */
    private boolean ventStillHolds() {
        if (this.vent == null) {
            return false;
        }
        Level level = this.level();
        boolean loaded = level.isLoaded(this.vent);
        boolean active = false;
        if (loaded) {
            BlockState s = level.getBlockState(this.vent);
            active = s.is(EuropaContent.HYDROTHERMAL_VENT.get()) && s.getValue(HydrothermalVentBlock.ACTIVE);
        }
        return EuropaFaunaRules.keepVent(loaded, active,
                this.distanceToSqr(this.vent.getX() + 0.5D, this.vent.getY() + 0.5D, this.vent.getZ() + 0.5D));
    }

    /**
     * Nearest active vent among this search's lattice of the box (the lattice shifts every search), reading loaded
     * positions only; null when none.
     */
    private BlockPos findVent() {
        BlockPos origin = this.blockPosition();
        Level level = this.level();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int[] o : EuropaFaunaRules.scanOffsets(this.scanPass++)) {
            at.set(origin.getX() + o[0], origin.getY() + o[1], origin.getZ() + o[2]);
            if (!level.isLoaded(at)) {
                continue;
            }
            BlockState s = level.getBlockState(at);
            if (s.is(EuropaContent.HYDROTHERMAL_VENT.get()) && s.getValue(HydrothermalVentBlock.ACTIVE)) {
                return at.immutable();
            }
        }
        return null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.TROPICAL_FISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TROPICAL_FISH_DEATH;
    }
}
