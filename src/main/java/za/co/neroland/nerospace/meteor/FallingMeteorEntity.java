package za.co.neroland.nerospace.meteor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModEntities;

/**
 * A meteor falling from the sky (meteor-events design §4). A non-living, AI-less {@link Entity}
 * (like the rocket) that descends on a diagonal arc toward a fixed crater centre, trailing flame and
 * smoke, and on contact carves a small crater of {@code meteor_rock} around a loot-bearing
 * {@code meteor_core}. All gameplay is server-authoritative; the client only renders the synced
 * position + spins the rock and draws the trail.
 *
 * <p>Motion is recomputed each tick by aiming at the stored target at a fixed speed, so the meteor
 * always lands exactly where it was scheduled and the descent survives a mid-flight save/reload
 * without persisting velocity.</p>
 */
public class FallingMeteorEntity extends Entity {

    /** Blocks above the target the meteor spawns at. */
    public static final int FALL_HEIGHT = 150;
    /** Blocks travelled per tick (fast + dramatic). */
    private static final double SPEED = 1.7D;

    private int targetX;
    private int targetY;
    private int targetZ;
    private long lootSeed;
    /** Gallery/showcase only: hover in place (spin + trail) instead of falling. Not persisted. */
    private boolean frozen;

    private final InterpolationHandler interpolation = new InterpolationHandler(this);

    @SuppressWarnings("this-escape")
    public FallingMeteorEntity(EntityType<? extends FallingMeteorEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true; // we step the position manually; no vanilla collision pushback
    }

    /**
     * Spawns a meteor aimed at {@code target} (a surface block position) with RNG loot from
     * {@code seed}. The spawn point is high above the target with a random horizontal offset so the
     * descent reads as a diagonal arc. Server-side.
     */
    public static FallingMeteorEntity spawn(ServerLevel level, BlockPos target, long seed) {
        FallingMeteorEntity meteor = new FallingMeteorEntity(ModEntities.FALLING_METEOR.get(), level);
        meteor.targetX = target.getX();
        meteor.targetY = target.getY();
        meteor.targetZ = target.getZ();
        meteor.lootSeed = seed;

        double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
        double offset = FALL_HEIGHT * 0.45D;
        double sx = target.getX() + 0.5D + Math.cos(angle) * offset;
        double sz = target.getZ() + 0.5D + Math.sin(angle) * offset;
        meteor.setPos(sx, target.getY() + FALL_HEIGHT, sz);
        level.addFreshEntity(meteor);
        level.playSound(null, target, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, SoundSource.AMBIENT, 4.0F, 0.6F);
        return meteor;
    }

    /** Spawns a non-falling meteor that hovers + spins + trails — for the gallery/showcase only. */
    public static FallingMeteorEntity spawnFrozen(ServerLevel level, double x, double y, double z) {
        FallingMeteorEntity meteor = new FallingMeteorEntity(ModEntities.FALLING_METEOR.get(), level);
        meteor.frozen = true;
        meteor.setPos(x, y, z);
        level.addFreshEntity(meteor);
        return meteor;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // No synced data: the client renders from the tracked position + spins on tickCount.
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    private Vec3 targetVec() {
        return new Vec3(this.targetX + 0.5D, this.targetY + 0.5D, this.targetZ + 0.5D);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            if (this.interpolation.hasActiveInterpolation()) {
                this.interpolation.interpolate();
            }
            spawnTrail();
            return;
        }

        if (this.frozen) {
            return; // gallery/showcase: hover in place
        }

        Vec3 pos = position();
        Vec3 target = targetVec();
        Vec3 delta = target.subtract(pos);
        double dist = delta.length();

        // Impact when we reach the target column or drop to/below the crater surface.
        if (dist <= SPEED || pos.y <= this.targetY + 0.5D) {
            resolveImpact((ServerLevel) level());
            return;
        }

        Vec3 step = delta.scale(SPEED / dist);
        this.setDeltaMovement(step); // for client interpolation / rotation cues
        this.setPos(pos.x + step.x, pos.y + step.y, pos.z + step.z);
    }

    /** Flame + smoke trail, denser as the meteor nears the ground (client-side, per the design §4). */
    private void spawnTrail() {
        double proximity = 1.0D - Math.min(1.0D, (getY() - this.targetY) / (double) FALL_HEIGHT);
        int puffs = 2 + (int) (proximity * 4);
        for (int i = 0; i < puffs; i++) {
            double ox = (this.random.nextDouble() - 0.5D) * 0.8D;
            double oy = (this.random.nextDouble() - 0.5D) * 0.8D;
            double oz = (this.random.nextDouble() - 0.5D) * 0.8D;
            level().addParticle(ParticleTypes.FLAME, getX() + ox, getY() + oy, getZ() + oz, 0.0D, 0.02D, 0.0D);
            level().addParticle(ParticleTypes.LARGE_SMOKE, getX() + ox, getY() + 0.4D + oy, getZ() + oz,
                    0.0D, 0.04D, 0.0D);
        }
    }

    /**
     * Carve a small bowl crater (meteor-events design §4): clears the bowl interior to air, lines the
     * floor with {@code meteor_rock}, and seats a loot-bearing {@code meteor_core} at the deepest
     * point. Non-destructive beyond the radius, never touches bedrock, no fire or wide explosion.
     */
    private void resolveImpact(ServerLevel level) {
        int radius = Math.max(1, Config.METEOR_CRATER_RADIUS.get());
        BlockPos center = new BlockPos(this.targetX, this.targetY, this.targetZ);
        BlockState rock = ModBlocks.METEOR_ROCK.get().defaultBlockState();

        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                double horiz = Math.sqrt(dx * dx + dz * dz);
                if (horiz > radius + 0.6D) {
                    continue;
                }
                int depth = Math.max(1, Math.round((float) (radius - horiz) * 0.7F));
                // Clear the open bowl above the floor.
                for (int dy = -depth + 1; dy <= radius; dy++) {
                    if (Math.sqrt(dx * dx + dy * dy + dz * dz) > radius + 0.6D) {
                        continue;
                    }
                    BlockPos p = center.offset(dx, dy, dz);
                    if (!isProtected(level, p)) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
                // Line the bowl floor with meteor rock.
                BlockPos floor = center.offset(dx, -depth, dz);
                if (!isProtected(level, floor)) {
                    level.setBlock(floor, rock, 3);
                }
            }
        }

        // The loot core sits at the deepest centre point.
        int centerDepth = Math.max(1, Math.round(radius * 0.7F));
        BlockPos corePos = center.offset(0, -centerDepth, 0);
        if (!isProtected(level, corePos)) {
            level.setBlock(corePos, ModBlocks.METEOR_CORE.get().defaultBlockState(), 3);
            if (level.getBlockEntity(corePos) instanceof MeteorCoreBlockEntity core) {
                core.generateLoot(this.lootSeed);
            }
        }

        // Impact feedback (no terrain-damaging explosion).
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.getX() + 0.5D, center.getY() + 1.0D,
                center.getZ() + 0.5D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, center.getX() + 0.5D, center.getY() + 1.0D,
                center.getZ() + 0.5D, 40, radius, 1.0D, radius, 0.02D);
        level.playSound(null, center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D,
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 6.0F, 0.7F);

        MeteorEventManager.get(level).onImpact(center);

        if (Config.METEOR_DEBUG_LOG.get()) {
            // POPIA/GDPR: coordinates + dimension only, never player identifiers.
            Nerospace.LOGGER.info("[meteor] impact dim={} pos={} radius={}", level.dimension(), center, radius);
        }
        discard();
    }

    /** Bedrock and other unbreakable blocks (destroy speed &lt; 0) are left untouched. */
    private static boolean isProtected(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.BEDROCK) || state.getDestroySpeed(level, pos) < 0.0F;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false; // indestructible in flight
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.targetX = input.getIntOr("TargetX", 0);
        this.targetY = input.getIntOr("TargetY", 0);
        this.targetZ = input.getIntOr("TargetZ", 0);
        this.lootSeed = input.getLongOr("LootSeed", 0L);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("TargetX", this.targetX);
        output.putInt("TargetY", this.targetY);
        output.putInt("TargetZ", this.targetZ);
        output.putLong("LootSeed", this.lootSeed);
    }
}
