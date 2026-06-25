package za.co.neroland.nerospace.entity;

import java.util.function.Predicate;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Base for the terraform livestock species (DEEPER_TERRAFORM_DESIGN.md §5) — the mod's first
 * breedable {@code Animal}s, the "Earth life took hold" payoff of Living terraformed ground. One
 * shared food-driven goal set (panic/breed/tempt/follow-parent/wander); species supply their food,
 * sounds, attributes and offspring. Breed foods are vanilla crops on purpose: terraformed grass
 * drops seeds, so the ranching loop closes on the planet without overworld imports.
 */
public abstract class TerraformLivestock extends Animal {

    protected TerraformLivestock(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.8D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        Predicate<ItemStack> food = stack -> this.isFood(NerospaceCommon.requireNonNull(stack));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.2D, food, false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.2D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** Shared attribute base; species pass their health/speed. */
    public static AttributeSupplier.Builder createLivestockAttributes(double health, double speed) {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, speed);
    }
}
