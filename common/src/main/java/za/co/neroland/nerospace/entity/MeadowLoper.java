package za.co.neroland.nerospace.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.registry.ModSounds;

/**
 * Meadow Loper (DEEPER_TERRAFORM_DESIGN.md §5) — the placid bulk grazer of mature terraformed
 * Greenxertz: the cow-analogue of the seeded ecosystem. Breeds with wheat; drops Loper Haunch.
 */
public class MeadowLoper extends TerraformLivestock {

    public MeadowLoper(EntityType<? extends MeadowLoper> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.MEADOW_LOPER.get().create(level, EntitySpawnReason.BREEDING);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createLivestockAttributes(10.0D, 0.22D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MEADOW_LOPER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.MEADOW_LOPER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MEADOW_LOPER_DEATH.get();
    }
}
