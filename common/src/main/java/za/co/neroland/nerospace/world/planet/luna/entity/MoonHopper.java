package za.co.neroland.nerospace.world.planet.luna.entity;

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
import za.co.neroland.nerospace.world.planet.luna.LunaContent;

/**
 * Moon Hopper (003 §8.1): the small grazer of Luna's terraformed lunar meadow. It moves in hops rather than steps;
 * Luna's low gravity (applied by the shared gravity pass) turns each hop into a long, floaty arc. Native to Luna, so
 * it is in {@code #nerospace:native_fauna} and the planet's hazards leave it alone. Breeds with carrots.
 * Sounds reuse the vanilla rabbit.
 */
public class MoonHopper extends TerraformLivestock {

    public MoonHopper(EntityType<? extends MoonHopper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createLivestockAttributes(FaunaStats.HOPPER_HEALTH, FaunaStats.HOPPER_SPEED);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide()
                && FaunaStats.hopperHopsNow(this.onGround(), this.getNavigation().isInProgress(), this.tickCount)) {
            this.getJumpControl().jump();
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.CARROT);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return LunaContent.MOON_HOPPER.get().create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RABBIT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.RABBIT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RABBIT_DEATH;
    }
}
