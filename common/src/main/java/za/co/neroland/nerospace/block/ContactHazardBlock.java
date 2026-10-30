package za.co.neroland.nerospace.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.ContactHazardRules;
import za.co.neroland.nerospace.block.rules.HeatContactRules;
import za.co.neroland.nerospace.world.planet.PlanetHazards;

/**
 * Step hazard ground (002 §1.2 Perchlorate Crust, §1.3 Ember-Cracked Ashstone). {@link Hazard#POISON}: Poison I 4 s
 * without any sealed boots, not re-applied while the effect runs. {@link Hazard#HEAT}: 1.0 hot-floor damage every
 * 20 ticks without heat boots (no burning). Two hazards, two suit gates — the 002 §2.1.7 split.
 */
public class ContactHazardBlock extends Block {

    public enum Hazard {
        POISON, HEAT
    }

    public static final MapCodec<ContactHazardBlock> CODEC = BlockCodecs.simple(p -> new ContactHazardBlock(p, Hazard.POISON));

    private final Hazard hazard;

    public ContactHazardBlock(Properties properties, Hazard hazard) {
        super(properties);
        this.hazard = hazard;
    }

    protected MapCodec<? extends ContactHazardBlock> codec() {
        return CODEC;
    }

    public Hazard hazard() {
        return this.hazard;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel server && entity instanceof LivingEntity living
                && !PlanetHazards.isNative(entity)) {
            switch (this.hazard) {
                case POISON -> {
                    if (ContactHazardRules.poison(true, BlockSuits.sealedBoots(entity))
                            && !living.hasEffect(MobEffects.POISON)) {
                        living.addEffect(new MobEffectInstance(MobEffects.POISON, ContactHazardRules.POISON_TICKS,
                                ContactHazardRules.POISON_AMPLIFIER));
                    }
                }
                case HEAT -> {
                    float dmg = HeatContactRules.damage(true, BlockSuits.heatBoots(entity));
                    if (dmg > 0.0F && HeatContactRules.dueThisTick(server.getGameTime())) {
                        living.hurtServer(server, server.damageSources().hotFloor(), dmg);
                    }
                }
            }
        }
        super.stepOn(level, pos, state, entity);
    }
}
