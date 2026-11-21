package za.co.neroland.nerospace.world.planet.europa.entity;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.entity.FaunaStats;
import za.co.neroland.nerospace.entity.TerraformLivestock;
import za.co.neroland.nerospace.world.planet.europa.EuropaContent;

/**
 * Shell Skater (design 9.3): a low six-legged ice strider with blade feet, Europa's surface grazer. It glides
 * rather than walks: on any ice it keeps most of its speed from tick to tick ({@link EuropaFaunaRules#glide}), and
 * Cut Europa Ice's grip does not slow it. It licks Lineae Salt now and then (the block is not consumed), steers its
 * wander away from Penitente Blades, panics when hit and breeds with brine salt. Native fauna; it stores nothing
 * about any player. Sounds reuse the vanilla strider.
 */
public class ShellSkater extends TerraformLivestock {

    private double lastSpeed;

    public ShellSkater(EntityType<? extends ShellSkater> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createLivestockAttributes(FaunaStats.SKATER_HEALTH, FaunaStats.SKATER_SPEED);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Priority 4, the same as following a parent; the goal skips babies, so the two never compete.
        this.goalSelector.addGoal(4, new LickSaltGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            return;
        }
        Vec3 v = this.getDeltaMovement();
        double speed = v.horizontalDistance();
        if (this.onGround() && isIce(this.level().getBlockState(this.blockPosition().below()))) {
            double glided = EuropaFaunaRules.glide(this.lastSpeed, speed);
            if (speed > 1.0E-4D && glided > speed) {
                double k = glided / speed;
                this.setDeltaMovement(v.x * k, v.y, v.z * k);
                speed = glided;
            }
        }
        this.lastSpeed = speed;
    }

    /** Ice the skater glides on: vanilla ice and Europa's ices. */
    static boolean isIce(BlockState state) {
        return state.is(BlockTags.ICE) || state.is(EuropaContent.EUROPA_ICE.get())
                || state.is(EuropaContent.CHAOS_ICE.get()) || state.is(EuropaContent.CUT_EUROPA_ICE.get())
                || state.is(EuropaContent.EUROPA_ICE_BRICKS.get()) || state.is(EuropaContent.SHELL_ICE.get())
                || state.is(EuropaContent.DEUTERIUM_ICE.get()) || state.is(EuropaContent.OXIDANT_ICE.get());
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        BlockState below = level.getBlockState(pos.below());
        boolean blade = level.getBlockState(pos).is(EuropaContent.PENITENTE_BLADE.get())
                || below.is(EuropaContent.PENITENTE_BLADE.get());
        if (blade) {
            return EuropaFaunaRules.BLADE_AVOIDANCE;
        }
        // V4-03: over ice the value is never negative, so the dark shell interior can spawn skaters (natural
        // spawning needs a walk-target value of at least 0; the inherited one is light-dependent).
        return EuropaFaunaRules.skaterWalkValue(false, isIce(below), super.getWalkTargetValue(pos, level));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(EuropaContent.BRINE_SALT.get());
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return EuropaContent.SHELL_SKATER.get().create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.STRIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.STRIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STRIDER_DEATH;
    }

    /** Walk to a nearby Lineae Salt block and lick it for a few seconds. Samples, never scans a whole box. */
    static final class LickSaltGoal extends Goal {

        private final ShellSkater skater;
        private int cooldown;
        private BlockPos target;
        private int ticks;
        private int licked;

        LickSaltGoal(ShellSkater skater) {
            this.skater = skater;
            this.cooldown = EuropaFaunaRules.lickCooldown(skater.getRandom().nextInt(
                    EuropaFaunaRules.LICK_COOLDOWN_MAX - EuropaFaunaRules.LICK_COOLDOWN_MIN + 1));
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (--this.cooldown > 0 || this.skater.isBaby()) {
                return false;
            }
            this.cooldown = EuropaFaunaRules.lickCooldown(this.skater.getRandom().nextInt(
                    EuropaFaunaRules.LICK_COOLDOWN_MAX - EuropaFaunaRules.LICK_COOLDOWN_MIN + 1));
            this.target = this.findSalt();
            return this.target != null;
        }

        private BlockPos findSalt() {
            BlockPos origin = this.skater.blockPosition();
            Level level = this.skater.level();
            int r = EuropaFaunaRules.LICK_RADIUS;
            for (int i = 0; i < EuropaFaunaRules.LICK_TRIES; i++) {
                BlockPos p = origin.offset(this.skater.getRandom().nextInt(2 * r + 1) - r,
                        this.skater.getRandom().nextInt(3) - 2, this.skater.getRandom().nextInt(2 * r + 1) - r);
                if (level.isLoaded(p) && level.getBlockState(p).is(EuropaContent.LINEAE_SALT.get())
                        && level.getBlockState(p.above()).isAir()) {
                    return p.immutable();
                }
            }
            return null;
        }

        @Override
        public void start() {
            this.ticks = 0;
            this.licked = 0;
            this.skater.getNavigation().moveTo(this.target.getX() + 0.5D, this.target.getY() + 1.0D,
                    this.target.getZ() + 0.5D, 1.0D);
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null && this.ticks < 200 && this.licked < EuropaFaunaRules.LICK_TICKS
                    && this.skater.level().getBlockState(this.target).is(EuropaContent.LINEAE_SALT.get());
        }

        @Override
        public void tick() {
            this.ticks++;
            double d = this.skater.distanceToSqr(this.target.getX() + 0.5D, this.target.getY() + 1.0D,
                    this.target.getZ() + 0.5D);
            if (d < 2.5D) {
                this.skater.getNavigation().stop();
                this.skater.getLookControl().setLookAt(this.target.getX() + 0.5D, this.target.getY() + 0.5D,
                        this.target.getZ() + 0.5D);
                this.licked++;
            }
        }

        @Override
        public void stop() {
            this.target = null;
            this.skater.getNavigation().stop();
        }
    }
}
