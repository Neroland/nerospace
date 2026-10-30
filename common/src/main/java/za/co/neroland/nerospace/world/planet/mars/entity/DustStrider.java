package za.co.neroland.nerospace.world.planet.mars.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.entity.FaunaStats;
import za.co.neroland.nerospace.entity.TerraformLivestock;
import za.co.neroland.nerospace.world.planet.mars.MarsContent;

/**
 * Dust Strider (003 §8.1): the long-legged herd grazer of Mars' terraformed red steppe. It keeps the shared livestock
 * goals (the herd follows a player holding wheat, calves follow their parents) and spawns in herds of three to six;
 * it never eats the grass, so a herd cannot strip terraformed ground back to dirt. Native
 * to Mars ({@code #nerospace:native_fauna}): perchlorate crust and dust storms do not hurt it. Breeds with wheat.
 * Sounds reuse the vanilla camel.
 */
public class DustStrider extends TerraformLivestock {

    public DustStrider(EntityType<? extends DustStrider> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createLivestockAttributes(FaunaStats.STRIDER_HEALTH, FaunaStats.STRIDER_SPEED);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return MarsContent.DUST_STRIDER.get().create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CAMEL_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.CAMEL_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CAMEL_DEATH;
    }
}
