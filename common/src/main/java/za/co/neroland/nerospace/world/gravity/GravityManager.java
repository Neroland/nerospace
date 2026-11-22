package za.co.neroland.nerospace.world.gravity;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.block.rules.BlockGravityRules;
import za.co.neroland.nerospace.block.world.BlockGravityOverrides;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.platform.Services;
import za.co.neroland.nerospace.registry.ModTags;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;

/**
 * Per-dimension / per-biome gravity (GRAVITY_DESIGN.md). Cross-loader server-side driver: each loader calls
 * {@link #tick(MinecraftServer)} once per server tick from its own hook (NeoForge {@code ServerTickEvent.Post},
 * Forge {@code TickEvent.ServerTickEvent.Post}, Fabric {@code ServerTickEvents.END_SERVER_TICK}), beside the
 * other per-level drivers.
 *
 * <p><b>Resolution.</b> {@link #factorAt(ServerLevel, BlockPos)} resolves a gravity factor for a position:
 * a biome carrying a {@link ModTags.Biomes gravity tag} overrides its dimension's flat default; otherwise the
 * dimension default (the planet's {@code defaultGravity} trait); otherwise {@code 1.0} (Earth-normal). The result is scaled
 * by the global {@link NerospaceConfig#gravityMultiplier()} balance knob. {@code 1.0} means "do nothing".</p>
 *
 * <p><b>Phase 1 — living entities.</b> Players and mobs are adjusted through the vanilla {@code GRAVITY}
 * entity attribute, which is synced to the client, so movement prediction matches the server (no
 * rubber-banding) and jump height / fall damage scale for free. A single transient {@link AttributeModifier}
 * (id {@link #MODIFIER_ID}, {@code ADD_MULTIPLIED_TOTAL}, amount {@code factor - 1}) is added/updated/removed
 * idempotently; transient ⇒ never written to NBT, so it cannot stack or dupe across relog. The mod's own
 * entities (rocket, meteor) and vanilla non-living entities (items/arrows/…) are handled in later phases.</p>
 */
public final class GravityManager {

    /** Stable id for the gravity attribute modifier (26.x uses {@link Identifier}, not ResourceLocation). */
    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath("nerospace", "dimension_gravity");

    /** Flat per-dimension gravity default: the planet's {@code defaultGravity} trait, 1.0 off-planet. */
    private static double dimensionDefault(ResourceKey<Level> dimension) {
        PlanetDefinition planet = Planets.get(dimension);
        return planet == null ? 1.0 : planet.traits().defaultGravity();
    }

    /** Biome-tag override factors, checked most-specific first. */
    private static final double MICRO_FACTOR = 0.15;
    private static final double LOW_FACTOR = 0.4;
    private static final double HIGH_FACTOR = 1.5;
    /** Terraformed ground reads as Earth-normal gravity, regardless of the planet's low default. */
    private static final double NORMAL_FACTOR = 1.0;

    /** Re-evaluate every N ticks (crossing a biome line updates gravity without per-tick cost). */
    private static final int CHECK_INTERVAL_TICKS = 10;

    private GravityManager() {
    }

    /**
     * The gravity factor at a position, resolved in priority order: an explicit biome gravity tag (authored
     * intent wins), else <b>terraformed ground</b> → Earth-normal (gravity normalizes as you terraform a
     * low-gravity planet — the existing {@link Services#PLATFORM} {@code isTerraformed} chunk flag), else the
     * dimension default, else {@code 1.0}. Scaled by the config multiplier and floored at 0 (no inverted
     * gravity).
     */
    public static double factorAt(ServerLevel level, BlockPos pos) {
        // Block overrides come first: Gravity Plate beats Gravity Shard, then lowest factor.
        // A Shard's negative factor is intentional (slow levitation), so the floor below does not apply to it.
        double block = BlockGravityOverrides.factorAt(level, pos);
        // A switched-off station module (Gravity Deck power toggle) skips the overrides inside its box.
        if (!Double.isNaN(block) && za.co.neroland.nerospace.registry.ModDimensions.STATION_LEVEL.equals(level.dimension())
                && !za.co.neroland.nerospace.rocket.StationRegistry.get(level.getServer())
                        .moduleEnabledAt(level.dimension(), pos)) {
            block = Double.NaN;
        }
        if (!Double.isNaN(block)) {
            return block < 0.0 ? block : block * NerospaceConfig.gravityMultiplier();
        }
        Holder<Biome> biome = level.getBiome(pos);
        double base;
        if (biome.is(ModTags.Biomes.GRAVITY_MICRO)) {
            base = MICRO_FACTOR;
        } else if (biome.is(ModTags.Biomes.GRAVITY_LOW)) {
            base = LOW_FACTOR;
        } else if (biome.is(ModTags.Biomes.GRAVITY_HIGH)) {
            base = HIGH_FACTOR;
        } else if (isTerraformed(level, pos)) {
            base = NORMAL_FACTOR;
        } else {
            base = dimensionDefault(level.dimension());
        }
        return Math.max(0.0, base * NerospaceConfig.gravityMultiplier());
    }

    /**
     * Whether the chunk at {@code pos} carries the terraformed flag — read from an ALREADY-LOADED chunk only.
     * {@code level.getChunkAt} would block and load (or generate) the chunk synchronously; called from the
     * server tick for an entity standing on a chunk edge, that nested load runs other chunk tasks mid-tick
     * and can re-enter the entity-section manager while it is iterating (the ConcurrentModificationException
     * seen with C2ME). An unloaded chunk simply reads as "not terraformed".
     */
    private static boolean isTerraformed(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && Services.PLATFORM.isTerraformed(chunk);
    }

    /**
     * The flat dimension-default gravity factor (config-scaled, floored at 0), with no per-position biome
     * tag or terraforming override applied — i.e. the gravity an untagged, un-terraformed spot in that
     * dimension has. {@code 1.0} (Earth-normal) for any dimension without a Nerospace default. This is the
     * read-only value surfaced as the "default gravity" planet trait by the public
     * {@code za.co.neroland.nerospace.api} facade; for an exact per-position reading use
     * {@link #factorAt(ServerLevel, BlockPos)}.
     */
    public static double defaultFactor(ResourceKey<Level> dimension) {
        double base = dimensionDefault(dimension);
        return Math.max(0.0, base * NerospaceConfig.gravityMultiplier());
    }

    /**
     * Per-server tick. Two passes (common is raw vanilla → the no-arg {@code getEntities()} is protected, so
     * both use the public typed query):
     * <ul>
     *   <li><b>Living entities</b> — all loaded levels, throttled every {@link #CHECK_INTERVAL_TICKS} ticks
     *       (the {@code GRAVITY} attribute modifier persists between checks; scanning all levels lets a
     *       factor of 1.0 strip the modifier off entities that returned to a normal dimension).</li>
     *   <li><b>Non-living entities</b> — gravity dimensions only, <em>every</em> tick (vanilla applies their
     *       hardcoded gravity every tick, so the correction must too), but only inside the columns that still
     *       entity-tick: simulation distance around each player, plus force-loaded chunks. Skips entities with
     *       no gravity (the rocket / meteor self-manage), the grounded, and those in water. A block position's
     *       factor is computed once per tick and reused, so a pile of items costs one biome lookup.</li>
     * </ul>
     */
    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            // No observers in this level → nothing to adjust; skip the per-tick entity scan entirely
            // (mirrors the MeteorEvents idle short-circuit). The level the player IS in still runs, so the
            // living-entity cleanup pass keeps working for entities returning to a normal dimension.
            if (level.players().isEmpty()) {
                continue;
            }
            if (level.getGameTime() % CHECK_INTERVAL_TICKS == 0) {
                for (LivingEntity living : level.getEntities(EntityTypeTest.forClass(LivingEntity.class), e -> true)) {
                    BlockPos pos = living.blockPosition();
                    // Leave entities on not-yet-loaded ground alone until their chunk is ready (see isTerraformed).
                    if (!level.hasChunkAt(pos)) {
                        continue;
                    }
                    applyToLiving(living, factorAt(level, pos));
                }
            }
            if (Planets.isPlanet(level.dimension())) {
                correctNonLiving(level);
            }
        }
    }

    /**
     * Non-living correction over the entities vanilla is actually moving. A full-level query also walked entities
     * in lazy chunks, which are not ticked, and allocated a list of every item on the ground. Each entity is
     * corrected at most once even when two players' columns overlap.
     */
    private static void correctNonLiving(ServerLevel level) {
        int reach = GravityScanRules.horizontalReach(level.getServer().getPlayerList().getSimulationDistance());
        IntOpenHashSet seen = new IntOpenHashSet();
        Long2DoubleOpenHashMap factors = new Long2DoubleOpenHashMap();
        factors.defaultReturnValue(Double.NaN);
        int minY = level.getMinY();
        int maxY = level.getMaxY();
        for (ServerPlayer player : level.players()) {
            correctBox(level, new AABB(player.getX() - reach, minY, player.getZ() - reach,
                    player.getX() + reach, maxY, player.getZ() + reach), seen, factors);
        }
        LongSet forced = level.getChunkSource().getForceLoadedChunks();
        LongIterator it = forced.iterator();
        while (it.hasNext()) {
            long key = it.nextLong();
            int cx = ChunkPos.getX(key);
            int cz = ChunkPos.getZ(key);
            correctBox(level, new AABB(cx << 4, minY, cz << 4, (cx << 4) + 16, maxY, (cz << 4) + 16), seen, factors);
        }
    }

    private static void correctBox(ServerLevel level, AABB box, IntOpenHashSet seen, Long2DoubleOpenHashMap factors) {
        for (Entity entity : level.getEntities(EntityTypeTest.forClass(Entity.class), box,
                e -> !(e instanceof LivingEntity)
                        && !GravityScanRules.skipMotion(e.isNoGravity(), e.onGround(), e.isInWater()))) {
            if (!seen.add(entity.getId())) {
                continue;
            }
            BlockPos pos = entity.blockPosition();
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            long key = pos.asLong();
            double factor = factors.get(key);
            if (Double.isNaN(factor)) {
                factor = factorAt(level, pos);
                factors.put(key, factor);
            }
            applyCorrection(entity, factor);
        }
    }

    /**
     * Adds / updates / removes the transient gravity modifier on a living entity to match {@code factor}.
     * No-op when the entity has no {@code GRAVITY} attribute or the modifier is already at the right value,
     * so it never re-syncs needlessly.
     */
    private static void applyToLiving(LivingEntity entity, double factor) {
        if (factor < 0.0) {
            // Gravity Shard: weightless + a slow upward drift capped at levitation I, re-asserted every check
            // (a negative factor is a marker, never a negative acceleration).
            applyToLiving(entity, 0.0);
            entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION,
                    BlockGravityRules.SHARD_LEVITATION_TICKS, 0, true, false));
            return;
        }
        AttributeInstance instance = entity.getAttribute(Attributes.GRAVITY);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(MODIFIER_ID);
        if (factor == 1.0) {
            if (existing != null) {
                instance.removeModifier(MODIFIER_ID);
            }
            return;
        }
        double amount = factor - 1.0;
        if (existing != null && existing.amount() == amount) {
            return;
        }
        instance.addOrUpdateTransientModifier(
                new AttributeModifier(MODIFIER_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /**
     * Applies an already-resolved factor. Vanilla already subtracted {@code getGravity()} this tick; the rules
     * put back the share that does not belong, including the Gravity Shard drift. {@code 1.0} and zero gravity
     * leave the velocity alone.
     */
    private static void applyCorrection(Entity entity, double factor) {
        double gravity = entity.getGravity();
        double dy = GravityScanRules.correctedDy(entity.getDeltaMovement().y, gravity, factor);
        Vec3 dm = entity.getDeltaMovement();
        if (dy != dm.y) {
            entity.setDeltaMovement(dm.x, dy, dm.z);
        }
    }
}
