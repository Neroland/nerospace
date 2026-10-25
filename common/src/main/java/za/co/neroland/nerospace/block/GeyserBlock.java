package za.co.neroland.nerospace.block;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.GeyserRules;
import za.co.neroland.nerospace.block.rules.TickBootstrapRules;

/**
 * Frozen Geyser (002 §1.4): {@code CHARGED} toggles every half interval and the geyser vents each time it becomes
 * charged — once per 160 ticks. On the vent, entities standing in the 1×1 above get +1.2 dy and 60 ticks of fall
 * grace (fall distance held at 0, {@link #tickGrace}), with 12 cloud particles. Not weather-driven. Register with
 * {@code randomTicks()}: worldgen never calls {@code onPlace}, so a generated geyser starts its cycle on its first
 * random tick ({@link TickBootstrapRules}).
 */
public class GeyserBlock extends Block {

    public static final MapCodec<GeyserBlock> CODEC = BlockCodecs.simple(GeyserBlock::new);

    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");

    /** Launched entity → game time its fall grace ends (live objects only, weakly held, never persisted). */
    private static final Map<Entity, Long> GRACE = Collections.synchronizedMap(new WeakHashMap<>());

    @SuppressWarnings("this-escape")
    public GeyserBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(CHARGED, Boolean.FALSE));
    }

    protected MapCodec<? extends GeyserBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGED);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) {
            level.scheduleTick(pos, this, GeyserRules.INTERVAL_TICKS / 2);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (TickBootstrapRules.shouldBootstrap(level.getBlockTicks().hasScheduledTick(pos, this))) {
            level.scheduleTick(pos, this, TickBootstrapRules.geyserDelay(random.nextInt(GeyserRules.INTERVAL_TICKS / 2)));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean charged = !state.getValue(CHARGED);
        level.setBlock(pos, state.setValue(CHARGED, charged), Block.UPDATE_CLIENTS);
        if (charged) {
            for (Entity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos.above()))) {
                Vec3 v = e.getDeltaMovement();
                e.setDeltaMovement(v.x, GeyserRules.launchVelocity(true, Math.max(0.0, v.y)), v.z);
                e.resetFallDistance();
                // Push the new velocity to the client (field renamed in 26.3).
                //? if >=26.3 {
                /*e.needsSync = true;
                *///?} else {
                e.hurtMarked = true;
                //?}
                GRACE.put(e, level.getGameTime() + GeyserRules.FALL_GRACE_TICKS);
            }
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    GeyserRules.CLOUD_PARTICLES, 0.2, 0.4, 0.2, 0.05);
        }
        level.scheduleTick(pos, this, GeyserRules.INTERVAL_TICKS / 2);
    }

    /** Per server tick: hold launched entities' fall distance at 0 until their grace ends. */
    public static void tickGrace() {
        if (GRACE.isEmpty()) {
            return;
        }
        synchronized (GRACE) {
            GRACE.entrySet().removeIf(e -> {
                Entity entity = e.getKey();
                if (!entity.isAlive() || entity.level().getGameTime() > e.getValue()) {
                    return true;
                }
                entity.resetFallDistance();
                return false;
            });
        }
    }
}
