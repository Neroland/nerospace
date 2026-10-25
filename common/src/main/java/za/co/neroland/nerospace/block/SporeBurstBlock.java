package za.co.neroland.nerospace.block;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.SporeBurstRules;
import za.co.neroland.nerospace.block.rules.SporeContactRules;

/**
 * Bursting pod (Spore Pod, 002 §1.5, §2.1.1): a living entity walking into it, or a punch with anything but shears or
 * a Silk Touch tool, consumes the block. The burst leaves a <b>visual</b> cloud (r 2.5, 100 ticks, no effect of its
 * own); {@link #tickBursts} applies Nausea I 8 s every 10 ticks to living entities inside it that are not wearing a
 * sealed helmet (a vanilla cloud cannot read armour). Shears/Silk Touch mine it normally — the loot table drops it.
 */
public class SporeBurstBlock extends Block {

    public static final MapCodec<SporeBurstBlock> CODEC = BlockCodecs.simple(SporeBurstBlock::new);

    /** How often a live burst re-applies its gated nausea. */
    public static final int APPLY_INTERVAL_TICKS = 10;

    private record Burst(ServerLevel level, double x, double y, double z, long expiresAt) {
    }

    /** Live bursts (positions + expiry only; no entity or player data). Almost always empty. */
    private static final List<Burst> BURSTS = new ArrayList<>();

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
        if (level instanceof ServerLevel server
                && SporeBurstRules.bursts(harvestsWhole(server, player.getMainHandItem()))) {
            burst(server, pos);
        }
    }

    /** Shears or a Silk Touch tool take the pod whole. */
    private static boolean harvestsWhole(ServerLevel level, ItemStack stack) {
        if (stack.is(Items.SHEARS)) {
            return true;
        }
        return EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),
                stack) > 0;
    }

    public static void burst(ServerLevel level, BlockPos pos) {
        if (!level.removeBlock(pos, false)) {
            return;
        }
        SporeBurstRules.Cloud spec = SporeBurstRules.cloud();
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.2;
        double z = pos.getZ() + 0.5;
        AreaEffectCloud cloud = new AreaEffectCloud(level, x, y, z);
        cloud.setRadius((float) spec.radius());
        cloud.setDuration(spec.durationTicks());
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(0.0F);
        level.addFreshEntity(cloud); // visual only: no effect added
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, x, pos.getY() + 0.5, z, SporeBurstRules.PARTICLES,
                1.0, 0.5, 1.0, 0.02);
        synchronized (BURSTS) {
            BURSTS.add(new Burst(level, x, y, z, level.getGameTime() + spec.durationTicks()));
        }
        applyNausea(level, x, y, z, spec);
    }

    private static void applyNausea(ServerLevel level, double x, double y, double z, SporeBurstRules.Cloud spec) {
        double r = spec.radius();
        AABB box = new AABB(x - r, y - 0.5, z - r, x + r, y + 2.5, z + r);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.distanceToSqr(x, e.getY(), z) <= r * r)) {
            if (SporeContactRules.nausea(BlockSuits.sealedHelmet(living))) {
                living.addEffect(new MobEffectInstance(MobEffects.NAUSEA, spec.nauseaTicks(), 0));
            }
        }
    }

    /** Per server tick (from {@link BlockTicks}): re-apply gated nausea for live bursts every 10 ticks. */
    public static void tickBursts() {
        if (BURSTS.isEmpty()) {
            return;
        }
        SporeBurstRules.Cloud spec = SporeBurstRules.cloud();
        synchronized (BURSTS) {
            BURSTS.removeIf(b -> {
                long now = b.level().getGameTime();
                if (now > b.expiresAt() || b.level().getServer() == null) {
                    return true;
                }
                if (now % APPLY_INTERVAL_TICKS == 0) {
                    applyNausea(b.level(), b.x(), b.y(), b.z(), spec);
                }
                return false;
            });
        }
    }

    /** Drops every queued burst (server stop). */
    static void clear() {
        synchronized (BURSTS) {
            BURSTS.clear();
        }
    }
}
