package za.co.neroland.nerospace.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.SporeBurstRules;

/**
 * Bursting pod (Spore Pod, 002 §1.5, §2.1.1): a living entity walking into it or a punch consumes the block and leaves
 * an area cloud (r 2.5, 100 ticks, Nausea I 8 s). Shears/silk harvest without bursting is the loot table's job —
 * breaking never bursts. The vanilla cloud applies its effect to every living entity inside; the sealed-helmet gate
 * is applied by {@link #burst} only to the toucher (the cloud itself cannot see armour).
 */
public class SporeBurstBlock extends Block {

    public static final MapCodec<SporeBurstBlock> CODEC = BlockCodecs.simple(SporeBurstBlock::new);

    public SporeBurstBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends SporeBurstBlock> codec() {
        return CODEC;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effects, boolean isPrecise) {
        if (level instanceof ServerLevel server && entity instanceof LivingEntity) {
            burst(server, pos);
        }
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel server) {
            burst(server, pos);
        }
    }

    public static void burst(ServerLevel level, BlockPos pos) {
        if (!level.removeBlock(pos, false)) {
            return;
        }
        SporeBurstRules.Cloud spec = SporeBurstRules.cloud();
        AreaEffectCloud cloud = new AreaEffectCloud(level, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5);
        cloud.setRadius((float) spec.radius());
        cloud.setDuration(spec.durationTicks());
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(0.0F);
        cloud.addEffect(new MobEffectInstance(MobEffects.NAUSEA, spec.nauseaTicks(), 0));
        level.addFreshEntity(cloud);
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SporeBurstRules.PARTICLES, 1.0, 0.5, 1.0, 0.02);
    }
}
