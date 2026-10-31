package za.co.neroland.nerospace.world.planet.nyxara.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.SharpRules;
import za.co.neroland.nerospace.world.planet.PlanetHazards;

/**
 * Shard Drift (Nyxara dayside): loose crystal shards in snow-like layers (1-8), static (no weather accumulation, no
 * melting: it is registered without random ticks). A living entity standing in it with no boots at all takes 1.0
 * damage on every 20th world tick (synchronised to game time, so the first cut lands 0-19 ticks after stepping in;
 * the hurt cooldown caps it anyway); any boots protect. Native fauna are left alone, like every other contact hazard.
 */
public class ShardDriftBlock extends SnowLayerBlock {

    /**
     * Kept for registries that want an explicit codec. Not returned from {@code codec()}: SnowLayerBlock's override is
     * invariant ({@code MapCodec<SnowLayerBlock>}) on 26.1.2/26.2 and absent on 26.3, so the inherited one stands
     * (the same arrangement as the Ash Layer).
     */
    public static final MapCodec<ShardDriftBlock> CODEC = BlockCodecs.simple(ShardDriftBlock::new);

    public ShardDriftBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effects, boolean isPrecise) {
        if (level instanceof ServerLevel server && entity instanceof LivingEntity living
                && !PlanetHazards.isNative(entity) && SharpRules.dueThisTick(server.getGameTime())
                && SharpRules.cuts(true, !living.getItemBySlot(EquipmentSlot.FEET).isEmpty())) {
            living.hurtServer(server, server.damageSources().generic(), SharpRules.DAMAGE);
        }
        super.entityInside(state, level, pos, entity, effects, isPrecise);
    }
}
