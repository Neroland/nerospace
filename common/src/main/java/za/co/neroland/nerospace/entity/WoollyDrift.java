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
 * Woolly Drift (DEEPER_TERRAFORM_DESIGN.md §5) — the shaggy cold-coat grazer of mature terraformed
 * Glacira: the sheep-analogue, unbothered by the cold like the Frost Strider that hunts it. Breeds
 * with wheat; drops Drift Fleece (→ string).
 */
public class WoollyDrift extends TerraformLivestock {

    public WoollyDrift(EntityType<? extends WoollyDrift> type, Level level) {
        super(type, level);
    }

    /** Bred for the ice moon: the cold coat means no freeze build-up (mirrors the Frost Strider). */
    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.WOOLLY_DRIFT.get().create(level, EntitySpawnReason.BREEDING);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createLivestockAttributes(8.0D, 0.23D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return NerospaceCommon.requireNonNull(ModSounds.WOOLLY_DRIFT_AMBIENT.get());
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return NerospaceCommon.requireNonNull(ModSounds.WOOLLY_DRIFT_HURT.get());
    }

    @Override
    protected SoundEvent getDeathSound() {
        return NerospaceCommon.requireNonNull(ModSounds.WOOLLY_DRIFT_DEATH.get());
    }
}
