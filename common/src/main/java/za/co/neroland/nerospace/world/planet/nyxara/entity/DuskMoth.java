package za.co.neroland.nerospace.world.planet.nyxara.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.entity.FaunaStats;

/**
 * Dusk Moth: the ambient flier of Nyxara's terraformed twilight grove. It drifts between the grove's
 * trees, flees when struck and never lands for long. It is an AMBIENT-category mob that only spawns where the light
 * is at most 7 ({@link FaunaStats#MOTH_MAX_LIGHT}), so it fills the grove's dim corners without eating the creature
 * cap. Native to Nyxara ({@code #nerospace:native_fauna}). Takes no fall damage. Sounds reuse the vanilla bat.
 */
public class DuskMoth extends PathfinderMob {

    // this-escape: idiomatic Minecraft constructor wiring. rawtypes/unchecked: FlyingMoveControl is generic from 26.2
    // on but not on 26.1.2, so the raw constructor is the one form that compiles on every line.
    @SuppressWarnings({"this-escape", "rawtypes", "unchecked"})
    public DuskMoth(EntityType<? extends DuskMoth> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, FaunaStats.MOTH_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, FaunaStats.MOTH_SPEED)
                .add(Attributes.FLYING_SPEED, FaunaStats.MOTH_FLYING_SPEED)
                .add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6D));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.BAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.3F;
    }
}
