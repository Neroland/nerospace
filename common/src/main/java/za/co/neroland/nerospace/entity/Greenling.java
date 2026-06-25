package za.co.neroland.nerospace.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;


import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModSounds;

/**
 * Greenling (Phase 5) — a small, harmless ambient creature. It wanders the Greenxertz surface and
 * flees from players and from being hurt; it has no attack. Pure flavor / life.
 */
public class Greenling extends PathfinderMob {

    public Greenling(EntityType<? extends Greenling> type, Level level) {
        super(type, level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return NerospaceCommon.requireNonNull(ModSounds.GREENLING_AMBIENT.get());
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return NerospaceCommon.requireNonNull(ModSounds.GREENLING_HURT.get());
    }

    @Override
    protected SoundEvent getDeathSound() {
        return NerospaceCommon.requireNonNull(ModSounds.GREENLING_DEATH.get());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 8.0F, 1.2D, 1.5D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }
}
