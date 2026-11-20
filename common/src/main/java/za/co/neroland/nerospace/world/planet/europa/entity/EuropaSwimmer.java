package za.co.neroland.nerospace.world.planet.europa.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
//? if >=1.21.11 {
import net.minecraft.world.entity.animal.fish.WaterAnimal;
//?} else {
/*import net.minecraft.world.entity.animal.WaterAnimal;
*///?}
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Shared body of Europa's two sea creatures. Like the vanilla squid they do not path-find: each tick on the server,
 * while in water, the species {@link #swim(Vec3)} turns the current velocity into the next one, and
 * {@link #travel(Vec3)} only applies it, so the water carries no gravity. Out of water the creature sinks, loses
 * most of its sideways speed, and has {@link EuropaFaunaRules#DRY_GRACE_TICKS} ticks of air before the vanilla
 * water-animal rules drown it. The body turns to face where it is going.
 */
public abstract class EuropaSwimmer extends WaterAnimal {

    protected EuropaSwimmer(EntityType<? extends EuropaSwimmer> type, Level level) {
        super(type, level);
    }

    /** The species' steering: from this tick's velocity, the next one (server only, in water only). */
    protected abstract Vec3 swim(Vec3 velocity);

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            return;
        }
        Vec3 v = this.getDeltaMovement();
        if (this.isInWater()) {
            this.setDeltaMovement(this.swim(v));
        } else {
            // Stranded: fall, keep little sideways speed, and run on the short air grace.
            this.setDeltaMovement(v.x * 0.5D, (v.y - 0.08D) * 0.98D, v.z * 0.5D);
            this.setAirSupply(EuropaFaunaRules.strandedAir(this.getAirSupply()));
        }
        Vec3 now = this.getDeltaMovement();
        if (now.horizontalDistanceSqr() > 1.0E-5D) {
            float yaw = -((float) Mth.atan2(now.x, now.z)) * Mth.RAD_TO_DEG;
            this.yBodyRot += Mth.wrapDegrees(yaw - this.yBodyRot) * 0.1F;
            this.setYRot(this.yBodyRot);
        }
    }

    @Override
    public void travel(Vec3 input) {
        this.move(MoverType.SELF, this.getDeltaMovement());
    }
}
