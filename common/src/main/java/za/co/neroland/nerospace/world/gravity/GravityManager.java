package za.co.neroland.nerospace.world.gravity;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.platform.Services;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.registry.ModTags;

/**
 * Per-dimension / per-biome gravity (GRAVITY_DESIGN.md). Cross-loader server-side driver: each loader calls
 * {@link #tick(MinecraftServer)} once per server tick from its own hook (NeoForge {@code ServerTickEvent.Post},
 * Forge {@code TickEvent.ServerTickEvent.Post}, Fabric {@code ServerTickEvents.END_SERVER_TICK}), beside the
 * other per-level drivers.
 *
 * <p><b>Resolution.</b> {@link #factorAt(ServerLevel, BlockPos)} resolves a gravity factor for a position:
 * a biome carrying a {@link ModTags.Biomes gravity tag} overrides its dimension's flat default; otherwise the
 * dimension default ({@link #DIMENSION_DEFAULTS}); otherwise {@code 1.0} (Earth-normal). The result is scaled
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

    /** Flat per-dimension gravity defaults (first-pass values; tuned during the Phase 4 playtest). */
    private static final Map<ResourceKey<Level>, Double> DIMENSION_DEFAULTS = Map.of(
            ModDimensions.GREENXERTZ_LEVEL, 0.6,
            ModDimensions.CINDARA_LEVEL, 0.8,
            ModDimensions.GLACIRA_LEVEL, 0.4,
            ModDimensions.STATION_LEVEL, 0.1);   // orbital station = near-zero g

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
        Holder<Biome> biome = level.getBiome(pos);
        double base;
        if (biome.is(ModTags.Biomes.GRAVITY_MICRO)) {
            base = MICRO_FACTOR;
        } else if (biome.is(ModTags.Biomes.GRAVITY_LOW)) {
            base = LOW_FACTOR;
        } else if (biome.is(ModTags.Biomes.GRAVITY_HIGH)) {
            base = HIGH_FACTOR;
        } else if (Services.PLATFORM.isTerraformed(level.getChunkAt(pos))) {
            base = NORMAL_FACTOR;
        } else {
            base = DIMENSION_DEFAULTS.getOrDefault(level.dimension(), 1.0);
        }
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
     *       hardcoded gravity every tick, so the correction must too). Skips entities with no gravity (the
     *       rocket / meteor self-manage), the grounded, and those in fluid.</li>
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
                    applyToLiving(living, factorAt(level, living.blockPosition()));
                }
            }
            if (DIMENSION_DEFAULTS.containsKey(level.dimension())) {
                for (Entity entity : level.getEntities(EntityTypeTest.forClass(Entity.class),
                        e -> !(e instanceof LivingEntity))) {
                    applyMotionCorrection(level, entity);
                }
            }
        }
    }

    /**
     * Adds / updates / removes the transient gravity modifier on a living entity to match {@code factor}.
     * No-op when the entity has no {@code GRAVITY} attribute or the modifier is already at the right value,
     * so it never re-syncs needlessly.
     */
    private static void applyToLiving(LivingEntity entity, double factor) {
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
     * Non-living gravity (GRAVITY_DESIGN.md §5c). These entities don't read the {@code GRAVITY} attribute —
     * vanilla applied their hardcoded gravity this tick, so we add back the unwanted share to leave a net of
     * {@code factor × gravity}. Uses the public {@link Entity#getGravity()} (javap-verified public on 26.1.2
     * and 26.2) — no mixin. Skips no-gravity entities (rocket/meteor self-manage; their gravity is 0 anyway),
     * the grounded (correction would jitter them), and the in-fluid (vanilla buoyancy already overrides).
     */
    private static void applyMotionCorrection(ServerLevel level, Entity entity) {
        if (entity.isNoGravity() || entity.onGround() || entity.isInWater()) {
            return;
        }
        double factor = factorAt(level, entity.blockPosition());
        if (factor == 1.0) {
            return;
        }
        double gravity = entity.getGravity();
        if (gravity == 0.0) {
            return;
        }
        Vec3 dm = entity.getDeltaMovement();
        entity.setDeltaMovement(dm.x, dm.y + gravity * (1.0 - factor), dm.z);
    }
}
