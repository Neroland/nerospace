package za.co.neroland.nerospace.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;


import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModSounds;

/**
 * Quartz Crawler (Phase 5) — a neutral creature. It grazes the Greenxertz surface peacefully and
 * ignores players, but retaliates with a melee attack when struck (via {@link HurtByTargetGoal}, with
 * no player-seeking target goal).
 */
public class QuartzCrawler extends PathfinderMob {

    public QuartzCrawler(EntityType<? extends QuartzCrawler> type, Level level) {
        super(type, level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return NerospaceCommon.requireNonNull(ModSounds.QUARTZ_CRAWLER_AMBIENT.get());
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return NerospaceCommon.requireNonNull(ModSounds.QUARTZ_CRAWLER_HURT.get());
    }

    @Override
    protected SoundEvent getDeathSound() {
        return NerospaceCommon.requireNonNull(ModSounds.QUARTZ_CRAWLER_DEATH.get());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // Retaliation only — no NearestAttackableTargetGoal, so it never hunts unprovoked.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }
}
