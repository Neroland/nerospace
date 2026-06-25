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

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.registry.ModSounds;

/**
 * Ember Strutter (DEEPER_TERRAFORM_DESIGN.md §5) — the skittish ground bird of mature terraformed
 * Cindara: the chicken-analogue, ember-feathered like its scorched homeworld (and fire-proof like
 * everything that survives there). Breeds with seeds; drops Strutter Drumstick.
 */
public class EmberStrutter extends TerraformLivestock {

    public EmberStrutter(EntityType<? extends EmberStrutter> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT_SEEDS);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.EMBER_STRUTTER.get().create(level, EntitySpawnReason.BREEDING);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createLivestockAttributes(6.0D, 0.3D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return NerospaceCommon.requireNonNull(ModSounds.EMBER_STRUTTER_AMBIENT.get());
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return NerospaceCommon.requireNonNull(ModSounds.EMBER_STRUTTER_HURT.get());
    }

    @Override
    protected SoundEvent getDeathSound() {
        return NerospaceCommon.requireNonNull(ModSounds.EMBER_STRUTTER_DEATH.get());
    }
}
