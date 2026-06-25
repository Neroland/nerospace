package za.co.neroland.nerospace.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;


import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModSounds;

/**
 * Cinder Stalker (Phase 7) — the hostile predator of the volcanic moon Cindara. Tougher and faster
 * than the Greenxertz {@link XertzStalker}, and fire-immune (set via the EntityType builder), so the
 * planet's lava and heat don't bother it. Server-authoritative AI.
 */
public class CinderStalker extends Monster {

    public CinderStalker(EntityType<? extends CinderStalker> type, Level level) {
        super(type, level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return NerospaceCommon.requireNonNull(ModSounds.CINDER_STALKER_AMBIENT.get());
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return NerospaceCommon.requireNonNull(ModSounds.CINDER_STALKER_HURT.get());
    }

    @Override
    protected SoundEvent getDeathSound() {
        return NerospaceCommon.requireNonNull(ModSounds.CINDER_STALKER_DEATH.get());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
}
