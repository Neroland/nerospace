package za.co.neroland.nerospace.machine;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
//? if <26.3 {
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
//?}
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.levelgen.Heightmap;

import za.co.neroland.nerospace.platform.Services;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;
import za.co.neroland.nerospace.world.planet.TerraformRefusal;
import za.co.neroland.nerospace.world.planet.TerraformTable;
import za.co.neroland.nerospace.world.planet.TerraformTables;
import za.co.neroland.nerospace.world.planet.TreeStyle;
import za.co.neroland.nerospace.registry.ModTags;
import za.co.neroland.nerospace.world.TerraformFauna;

/**
 * Shared, idempotent surface-column conversion for the Terraformer — the staged Rooted → Hydrated →
 * Living pipeline. Extracted so both the live frontier ({@link TerraformerBlockEntity})
 * and the chunk-load catch-up rescan ({@code TerraformManager}) convert columns identically. The
 * caller guarantees the column's chunk is loaded.
 *
 * <p>Stages: {@link #convertColumn} = stage 1 (Rooted), {@link #hydrateColumn} = stage 2 (Hydrated),
 * {@link #vivifyColumn} = stage 3 (Living). Each is a per-stage no-op when re-run, so persistence stays
 * radii + cursors.</p>
 *
 * <p>2.0: what each stage writes comes from the planet's {@link TerraformTable} — biome per stage, surface
 * swap, hydration liquid, flora, fauna, ore seeds and the planet rules — instead of per-dimension branches.
 * Off-planet dimensions use {@link TerraformTables#LEGACY} (the 1.x behaviour); a world without a table (the
 * station) refuses.</p>
 *
 * <p>Cross-loader port note: the per-chunk stage bookkeeping (TERRAFORMED / TERRAFORM_STAGE) goes
 * through the {@link Services#PLATFORM} chunk data-attachment seam rather than NeoForge's
 * {@code chunk.getData/setData}; the terraform config/tuning keys are inlined (config seam deferred).</p>
 */
public final class TerraformConversion {

    // --- Inlined from Config/Tuning (root shipped defaults) until the config seam lands ---
    private static final boolean WATER_ENABLED = true;
    private static final int WATER_MAX_DEPTH = 8;
    private static final boolean PLANTS_ENABLED = true;
    private static final boolean RESOURCES_ENABLED = true;
    private static final double PLANT_CHANCE = 0.08D;
    private static final double RESOURCE_CHANCE = 0.015D;
    private static final double TREE_CHANCE = 0.03D;

    /** {@code #nerospace:nyxara_band} — the twilight band (and the terraformed grove inside it). */
    private static final net.minecraft.tags.TagKey<Biome> NYXARA_BAND = net.minecraft.tags.TagKey.create(
            Registries.BIOME, Identifier.fromNamespaceAndPath("nerospace", "nyxara_band"));

    /** Shimmer dome: points per pulse (sparse — a hint of the edge, not a wall). */
    private static final int DOME_POINTS = 24;

    private TerraformConversion() {
    }

    // --- Table lookup ----------------------------------------------------------

    /** The table for this level: the planet's own, LEGACY off-planet, null where the terraformer refuses. */
    @Nullable
    public static TerraformTable tableFor(ServerLevel level) {
        return TerraformTables.resolve(Planets.get(level.dimension()));
    }

    /** Why a terraformer at {@code pos} will not run, or {@link TerraformRefusal#NONE}. */
    public static TerraformRefusal refusalAt(ServerLevel level, BlockPos pos) {
        TerraformTable table = tableFor(level);
        boolean bandOnly = table != null && table.has(TerraformTable.Rule.BAND_ONLY);
        return TerraformRefusal.evaluate(table != null, bandOnly, !bandOnly || inBand(level, table, pos));
    }

    /** Band-only planets: a column roots only where its biome is the band (or already one of the trio). */
    private static boolean columnAllowed(ServerLevel level, TerraformTable table, int x, int y, int z) {
        return !table.has(TerraformTable.Rule.BAND_ONLY) || inBand(level, table, new BlockPos(x, y, z));
    }

    private static boolean inBand(ServerLevel level, TerraformTable table, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        return biome.is(NYXARA_BAND)
                || biome.unwrapKey().map(table::isTrioBiome).orElse(false);
    }

    /**
     * The biome {@code stage} writes in this level. A stage biome that is not registered (a datapack removed it)
     * falls back to the LEGACY biome for that stage, so conversion never throws.
     */
    public static ResourceKey<Biome> stageBiomeFor(ServerLevel level, int stage) {
        TerraformTable table = tableFor(level);
        return stageBiome(level, table == null ? TerraformTables.LEGACY : table, stage);
    }

    private static ResourceKey<Biome> stageBiome(ServerLevel level, TerraformTable table, int stage) {
        ResourceKey<Biome> legacy = TerraformTables.LEGACY.stageBiome(stage).orElse(TerraformTables.LEGACY.living());
        return table.stageBiome(stage)
                .filter(key -> level.registryAccess().lookupOrThrow(Registries.BIOME).get(key).isPresent())
                .orElse(legacy);
    }

    /**
     * The mature stage-3 biome for this dimension (§4): the planet's terraform target. Unknown dimensions, planets
     * without a target, and targets whose biome is not registered settle as meadow (the 1.x fallback).
     */
    public static ResourceKey<Biome> matureBiomeFor(ServerLevel level) {
        return stageBiomeFor(level, 3);
    }

    @Nullable
    private static Block block(Identifier id) {
        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            return null;
        }
        Block b = BuiltInRegistries.BLOCK.getValue(id);
        return b == Blocks.AIR ? null : b;
    }

    private static Block blockOr(Identifier id, Block fallback) {
        Block b = block(id);
        return b == null ? fallback : b;
    }

    private static boolean isExtraSurface(TerraformTable table, BlockState state) {
        for (Identifier id : table.extraSurface()) {
            Block b = block(id);
            if (b != null && state.is(b)) {
                return true;
            }
        }
        return false;
    }

    // --- Stage bookkeeping ---------------------------------------------------

    /**
     * The chunk's effective terraform stage. Legacy chunks (pre-stage saves) carry only the
     * {@code TERRAFORMED} boolean, which maps to stage 1 — the no-break contract.
     */
    public static int effectiveStage(LevelChunk chunk) {
        int stage = Services.PLATFORM.getTerraformStage(chunk);
        return Math.max(stage, Services.PLATFORM.isTerraformed(chunk) ? 1 : 0);
    }

    /** Raise the chunk's recorded stage to at least {@code stage} (never lowers it). */
    private static void bumpStage(LevelChunk chunk, int stage) {
        if (Services.PLATFORM.getTerraformStage(chunk) < stage) {
            Services.PLATFORM.setTerraformStage(chunk, stage);
            chunk.markUnsaved();
        }
    }

    /**
     * Where stage-2 hydration draws its units from: the Terraformer's glacite-fed buffer for the live
     * frontier, or {@code null} for the chunk-load catch-up (which converts for free).
     */
    @FunctionalInterface
    public interface HydrationSink {
        /** @return units actually granted (0 = stall: stop advancing the stage-2 cursor). */
        int draw(int units);
    }

    /** Convert one surface column: terrain → the planet's soil, flag breathable, write biome, plants, ore. */
    public static void convertColumn(ServerLevel level, int x, int z, int tier, Set<LevelChunk> biomeChanged) {
        convertColumn(level, x, z, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), tier, biomeChanged);
    }

    /**
     * {@link #convertColumn(ServerLevel, int, int, int, Set)} with the surface height injected — the
     * seam the gametests need because the data-driven test framework encases every arena in a barrier
     * shell, so the in-arena heightmap always points at the barrier lid.
     */
    public static void convertColumn(ServerLevel level, int x, int z, int surfaceY, int tier,
            Set<LevelChunk> biomeChanged) {
        TerraformTable table = tableFor(level);
        if (table == null || !columnAllowed(level, table, x, surfaceY - 1, z)) {
            return; // the station, or outside Nyxara's band: nothing roots here
        }
        BlockPos top = new BlockPos(x, surfaceY - 1, z);
        BlockState topState = level.getBlockState(top);
        LevelChunk chunk = level.getChunkAt(top);
        Block topBlock = blockOr(table.surfaceTop(), Blocks.GRASS_BLOCK);
        Block subBlock = blockOr(table.surfaceSub(), Blocks.DIRT);

        // A stage-3 living top (Greenxertz' Xertz Soil) is kept out of the stage-1 swap once the chunk is Living.
        Block livingTop = table.livingTop().map(TerraformConversion::block).orElse(null);
        boolean keepLivingTop = livingTop != null && topState.is(livingTop) && effectiveStage(chunk) >= 3;

        if (!keepLivingTop && (topState.is(ModTags.Blocks.TERRAFORM_TO_GRASS) || isExtraSurface(table, topState))
                && !topState.is(topBlock)) {
            level.setBlock(top, topBlock.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int d = 1; d <= 3; d++) {
                BlockPos below = new BlockPos(x, surfaceY - 1 - d, z);
                BlockState belowState = level.getBlockState(below);
                if (belowState.is(ModTags.Blocks.TERRAFORM_TO_DIRT) || isExtraSurface(table, belowState)) {
                    level.setBlock(below, subBlock.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            frontierFx(level, table, x, surfaceY, z);
        }

        // Atmosphere payoff (§3.4): flag the chunk permanently breathable at/above the surface.
        if (!Services.PLATFORM.isTerraformed(chunk)) {
            Services.PLATFORM.setTerraformed(chunk, true);
            chunk.markUnsaved();
        }
        bumpStage(chunk, 1);

        if (writeBiomeColumn(level, chunk, x, z, stageBiome(level, table, 1))) {
            biomeChanged.add(chunk);
        }

        scatterPlant(level, table, topBlock, x, surfaceY, z);
        seedResource(level, table, x, surfaceY, z, tier);
    }

    // --- Stage 2: Hydrated ---------------------------------------------------

    /**
     * The hydration disc of one terraformer (centre and squared Hydrated radius). Mars' spring is only ever opened
     * on a column inside it, never elsewhere in the chunk.
     */
    public record SpringArea(int centerX, int centerZ, long radiusSq) {
        public static SpringArea of(BlockPos center, int radius) {
            return new SpringArea(center.getX(), center.getZ(), (long) radius * radius);
        }

        public boolean contains(int x, int z) {
            long dx = (long) x - this.centerX;
            long dz = (long) z - this.centerZ;
            return dx * dx + dz * dz <= this.radiusSq;
        }
    }

    /** {@link #hydrateColumn(ServerLevel, int, int, int, HydrationSink, Set)} without a biome resync set. */
    public static boolean hydrateColumn(ServerLevel level, int x, int z, int waterTableY, HydrationSink sink) {
        return hydrateColumn(level, x, z, waterTableY, sink, new java.util.HashSet<>());
    }

    /** The column alone is the spring area (no terraformer disc known). */
    public static boolean hydrateColumn(ServerLevel level, int x, int z, int waterTableY, HydrationSink sink,
            Set<LevelChunk> biomeChanged) {
        return hydrateColumn(level, x, z, waterTableY, sink, biomeChanged, new SpringArea(x, z, 0L));
    }

    /**
     * Stage-2 conversion of one column: fill the basin below {@code waterTableY} with the planet's still liquid,
     * one hydration unit per source placed, and write the Hydrated biome. The scan walks down from the table;
     * columns whose terrain sits at/above the table get no water (dry hills), and a chasm deeper than
     * {@code WATER_MAX_DEPTH} is skipped entirely rather than part-filled mid-air. On Mars the first hydrated
     * column of a chunk also opens a spring at the chunk's lowest surface cell (the first open water on Mars).
     *
     * @param sink hydration supply, or {@code null} for the free chunk-load catch-up
     * @param springArea the terraformer's hydration disc; Mars' spring is opened only inside it
     * @return {@code false} when the sink ran dry mid-column (stall: re-run this column later);
     *         {@code true} when the column is fully hydrated (possibly needing no water at all)
     */
    public static boolean hydrateColumn(ServerLevel level, int x, int z, int waterTableY, HydrationSink sink,
            Set<LevelChunk> biomeChanged, SpringArea springArea) {
        TerraformTable table = tableFor(level);
        LevelChunk chunk = level.getChunkAt(new BlockPos(x, 0, z));
        if (table == null || !columnAllowed(level, table, x,
                level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z)) {
            return true; // nothing to hydrate here — never a stall
        }
        Block liquid = blockOr(table.liquid(), Blocks.WATER);
        boolean firstInChunk = Services.PLATFORM.getTerraformStage(chunk) < 2;
        if (WATER_ENABLED) {
            int maxDepth = WATER_MAX_DEPTH;
            int tableY = Math.max(waterTableY, level.getMinY() + 1);

            // Find the basin floor: first non-fillable cell at/below the table, within the depth cap.
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, tableY, z);
            int floorY = Integer.MIN_VALUE;
            for (int depth = 0; depth <= maxDepth; depth++) {
                int y = tableY - depth;
                if (y <= level.getMinY()) {
                    break;
                }
                cursor.setY(y);
                BlockState state = level.getBlockState(cursor);
                if (!isFillable(state, liquid)) {
                    floorY = y;
                    break;
                }
            }

            if (floorY != Integer.MIN_VALUE) {
                cursor.setY(floorY);
                if (za.co.neroland.nerospace.block.rules.ThawRules.hydrationSkipsFill(
                        level.getBlockState(cursor).is(ModTags.Blocks.HYDRATION_IMMUNE))) {
                    // Permafrost floor: the stage advances, but no meltwater until stage 3 thaws it.
                    bumpStage(chunk, 2);
                    writeHydratedBiome(level, table, chunk, x, z, biomeChanged);
                    return true;
                }
            }
            if (floorY != Integer.MIN_VALUE && floorY < tableY) {
                // Fill bottom-up so a stall never leaves water floating above a gap.
                for (int y = floorY + 1; y <= tableY; y++) {
                    cursor.setY(y);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(liquid)) {
                        continue; // already hydrated — idempotent, no cost
                    }
                    if (!state.isAir() && !state.canBeReplaced()) {
                        break; // overhang closed the basin above this point
                    }
                    if (sink != null && sink.draw(1) <= 0) {
                        return false; // out of glacite — stall, keep the cursor on this column
                    }
                    level.setBlock(cursor, liquid.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            if (firstInChunk && table.has(TerraformTable.Rule.OPEN_WATER_SPRING)) {
                openSpring(level, table, chunk, liquid, springArea);
            }
        }
        bumpStage(chunk, 2);
        writeHydratedBiome(level, table, chunk, x, z, biomeChanged);
        return true;
    }

    private static void writeHydratedBiome(ServerLevel level, TerraformTable table, LevelChunk chunk, int x, int z,
            Set<LevelChunk> biomeChanged) {
        // Never step a Living column back to Hydrated (a catch-up replay after stage 3 is impossible, but be safe).
        if (effectiveStage(chunk) >= 3) {
            return;
        }
        if (writeBiomeColumn(level, chunk, x, z, stageBiome(level, table, 2))) {
            biomeChanged.add(chunk);
        }
    }

    /**
     * Mars' first open water: one free source block at the lowest surface cell of the chunk's columns inside the
     * terraformer's hydration disc, placed once when the chunk first turns Hydrated. Only natural ground counts as a
     * floor (the planet's surface blocks or the terraform soil), so a hole a player dug into a build or the floor of
     * a roofless build never gets the spring. Skipped when no such cell is open (an overhang, a structure).
     */
    private static void openSpring(ServerLevel level, TerraformTable table, LevelChunk chunk, Block liquid,
            SpringArea area) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int bestX = minX;
        int bestZ = minZ;
        int bestY = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = minX + dx;
                int z = minZ + dz;
                if (!area.contains(x, z)) {
                    continue;
                }
                int h = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                if (h >= bestY || !isNaturalFloor(table, level.getBlockState(probe.set(x, h, z)))) {
                    continue;
                }
                bestY = h;
                bestX = x;
                bestZ = z;
            }
        }
        if (bestY == Integer.MAX_VALUE) {
            return; // no natural floor inside the disc in this chunk
        }
        BlockPos spring = new BlockPos(bestX, bestY + 1, bestZ);
        BlockState at = level.getBlockState(spring);
        if ((at.isAir() || at.canBeReplaced()) && !at.is(liquid)
                && level.getBlockState(spring.below()).isFaceSturdy(level, spring.below(), net.minecraft.core.Direction.UP)) {
            level.setBlock(spring, liquid.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Natural terraformable ground: the tagged soils/stones, the planet's own surface, or the terraform soil. */
    private static boolean isNaturalFloor(TerraformTable table, BlockState state) {
        return state.is(ModTags.Blocks.TERRAFORM_TO_GRASS) || state.is(ModTags.Blocks.TERRAFORM_TO_DIRT)
                || isExtraSurface(table, state)
                || state.is(blockOr(table.surfaceTop(), Blocks.GRASS_BLOCK))
                || state.is(blockOr(table.surfaceSub(), Blocks.DIRT));
    }

    /** A cell the liquid fill may occupy: air, the liquid itself, or replaceable ground cover (grass/flowers). */
    private static boolean isFillable(BlockState state, Block liquid) {
        return state.isAir() || state.is(liquid) || state.canBeReplaced();
    }

    // --- Stage 3: Living -----------------------------------------------------

    /**
     * Stage-3 conversion of one column: settle the mature per-planet biome (real weather), thaw permafrost, grow
     * the occasional tree, lay the planet's living top and ground cover, and seed a starter herd of the planet's
     * livestock. Idempotent per stage.
     */
    public static void vivifyColumn(ServerLevel level, int x, int z, Set<LevelChunk> biomeChanged) {
        TerraformTable table = tableFor(level);
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        if (table == null || !columnAllowed(level, table, x, surfaceY - 1, z)) {
            return;
        }
        LevelChunk chunk = level.getChunkAt(new BlockPos(x, 0, z));
        if (writeBiomeColumn(level, chunk, x, z, stageBiome(level, table, 3))) {
            biomeChanged.add(chunk);
        }
        Block topBlock = blockOr(table.surfaceTop(), Blocks.GRASS_BLOCK);
        thawPermafrost(level, x, surfaceY, z);
        placeTree(level, table, topBlock, x, surfaceY, z);
        layLivingTop(level, table, topBlock, x, surfaceY, z);
        TerraformFauna.seedHerd(level, table, x, surfaceY, z);
        bumpStage(chunk, 3);
    }

    /**
     * Greenxertz: Xertz Soil replaces the stage-1 top on Living ground, with tame Spore Moss and glowcaps on it.
     * A table without a living top (every other planet) leaves the column exactly as it was and rolls nothing.
     */
    private static void layLivingTop(ServerLevel level, TerraformTable table, Block topBlock, int x, int surfaceY,
            int z) {
        Block living = table.livingTop().map(TerraformConversion::block).orElse(null);
        if (living == null) {
            return;
        }
        BlockPos ground = new BlockPos(x, surfaceY - 1, z);
        if (!level.getBlockState(ground).is(topBlock)) {
            return;
        }
        level.setBlock(ground, living.defaultBlockState(), Block.UPDATE_CLIENTS);
        BlockPos above = ground.above();
        RandomSource rnd = level.getRandom();
        if (!PLANTS_ENABLED || table.livingFlora().isEmpty() || rnd.nextDouble() >= PLANT_CHANCE) {
            return;
        }
        BlockState aboveState = level.getBlockState(above);
        if (!aboveState.isAir() && !aboveState.canBeReplaced()) {
            return;
        }
        TerraformTable.pick(table.livingFlora(), rnd.nextInt(TerraformTable.totalWeight(table.livingFlora())))
                .map(TerraformConversion::block)
                .ifPresent(plant -> level.setBlock(above, plant.defaultBlockState(), Block.UPDATE_CLIENTS));
    }

    /**
     * Stage 3 thaws Permafrost under the surface: grass on top, dirt below, {@code ThawRules.THAW_DEPTH}
     * deep. Columns without Permafrost (every 1.x chunk) are left exactly as they were.
     */
    private static void thawPermafrost(ServerLevel level, int x, int surfaceY, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int d = 1; d <= za.co.neroland.nerospace.block.rules.ThawRules.THAW_DEPTH; d++) {
            p.set(x, surfaceY - d, z);
            switch (za.co.neroland.nerospace.block.rules.ThawRules.thawAt(
                    level.getBlockState(p).is(ModTags.Blocks.HYDRATION_IMMUNE), d,
                    za.co.neroland.nerospace.block.rules.ThawRules.MIN_TERRAFORM_STAGE)) {
                case GRASS -> level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                case DIRT -> level.setBlock(p, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                case NONE -> {
                }
            }
        }
    }

    /** Sparse GROWN trees on Living ground (§4): the planet's feature or vanilla family, scaled per planet. */
    private static void placeTree(ServerLevel level, TerraformTable table, Block topBlock, int x, int surfaceY,
            int z) {
        if (!PLANTS_ENABLED || level.getRandom().nextDouble() >= TREE_CHANCE * table.treeChance()) {
            return;
        }
        BlockPos ground = new BlockPos(x, surfaceY - 1, z);
        BlockPos above = ground.above();
        if (!level.getBlockState(ground).is(topBlock) || !level.getBlockState(above).isAir()) {
            return;
        }
        if (table.treeFeature().isPresent() && level.getRandom().nextBoolean()
                && placeFeature(level, table.treeFeature().get(), above)) {
            return;
        }
        PlanetDefinition planet = Planets.get(level.dimension());
        TreeStyle style = planet == null ? TreeStyle.OAK_BIRCH : planet.treeStyle();
        //? if >=26.3 {
        /*ResourceKey<net.minecraft.world.level.levelgen.feature.Feature> tree;
        *///?} else {
        ResourceKey<ConfiguredFeature<?, ?>> tree;
        //?}
        tree = switch (style) {
            case ACACIA -> TreeFeatures.ACACIA;
            case SPRUCE -> TreeFeatures.SPRUCE;
            case OAK_BIRCH -> level.getRandom().nextBoolean() ? TreeFeatures.OAK : TreeFeatures.BIRCH;
        };
        //? if >=26.3 {
        /*level.registryAccess().lookupOrThrow(Registries.FEATURE).get(tree)
        *///?} else {
        level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(tree)
        //?}
                .ifPresent(holder -> holder.value().place(
                        level, level.getChunkSource().getGenerator(), level.getRandom(), above));
    }

    /** Places a data-driven feature by id (a planet's own tree analogue); false when it is missing or fails. */
    private static boolean placeFeature(ServerLevel level, Identifier id, BlockPos at) {
        //? if >=26.3 {
        /*var key = ResourceKey.create(Registries.FEATURE, id);
        return level.registryAccess().lookupOrThrow(Registries.FEATURE).get(key)
        *///?} else {
        var key = ResourceKey.create(Registries.CONFIGURED_FEATURE, id);
        return level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(key)
        //?}
                .map(holder -> holder.value().place(level, level.getChunkSource().getGenerator(), level.getRandom(), at))
                .orElse(false);
    }

    /** Write {@code biomeKey} down this column's sections. @return true if anything changed. */
    private static boolean writeBiomeColumn(ServerLevel level, LevelChunk chunk, int x, int z,
            ResourceKey<Biome> biomeKey) {
        Holder<Biome> terra = level.registryAccess()
                .lookupOrThrow(Registries.BIOME).getOrThrow(biomeKey);
        int bx = (x & 15) >> 2;   // biome cells are 4-block resolution (0..3 within a section)
        int bz = (z & 15) >> 2;
        boolean changed = false;
        for (LevelChunkSection section : chunk.getSections()) {
            PalettedContainerRO<Holder<Biome>> ro = section.getBiomes();
            if (!(ro instanceof PalettedContainer<?>)) {
                continue; // not writable — skip rather than risk a cast error
            }
            PalettedContainer<Holder<Biome>> biomes = (PalettedContainer<Holder<Biome>>) ro;
            for (int by = 0; by < 4; by++) {
                if (biomes.getAndSet(bx, by, bz, terra) != terra) {
                    changed = true;
                }
            }
        }
        if (changed) {
            chunk.markUnsaved();
        }
        return changed;
    }

    /** Sparse ground-cover scatter on freshly converted ground — the planet's flora set (terraform design §2.2). */
    private static void scatterPlant(ServerLevel level, TerraformTable table, Block topBlock, int x, int surfaceY,
            int z) {
        if (!PLANTS_ENABLED || table.flora().isEmpty()) {
            return;
        }
        RandomSource rnd = level.getRandom();
        if (rnd.nextDouble() >= PLANT_CHANCE) {
            return;
        }
        BlockPos ground = new BlockPos(x, surfaceY - 1, z);
        BlockPos above = new BlockPos(x, surfaceY, z);
        if (!level.getBlockState(ground).is(topBlock) || !level.getBlockState(above).isAir()) {
            return;
        }
        TerraformTable.pick(table.flora(), rnd.nextInt(TerraformTable.totalWeight(table.flora())))
                .map(TerraformConversion::block)
                .ifPresent(plant -> level.setBlock(above, plant.defaultBlockState(), Block.UPDATE_CLIENTS));
    }

    /** Tier-3 low-rate ore seeding into the converted subsurface — the planet's ore seed set (§2.2 / §T3). */
    private static void seedResource(ServerLevel level, TerraformTable table, int x, int surfaceY, int z, int tier) {
        if (tier < 3 || !RESOURCES_ENABLED) {
            return;
        }
        RandomSource rnd = level.getRandom();
        if (rnd.nextDouble() >= RESOURCE_CHANCE) {
            return;
        }
        Block ore = TerraformResources.pickOre(table, rnd);
        if (ore == null) {
            return;
        }
        int y = surfaceY - 4 - rnd.nextInt(8);
        BlockPos orePos = new BlockPos(x, y, z);
        BlockState at = level.hasChunk(orePos.getX() >> 4, orePos.getZ() >> 4) ? level.getBlockState(orePos) : null;
        if (at != null && (at.is(ModTags.Blocks.TERRAFORM_TO_DIRT) || isExtraSurface(table, at)
                || at.is(blockOr(table.surfaceSub(), Blocks.DIRT)))) {
            level.setBlock(orePos, ore.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Sparse frontier particles (the planet's own) + a soft soil sound as a shell converts (§2.4). */
    private static void frontierFx(ServerLevel level, TerraformTable table, int x, int surfaceY, int z) {
        RandomSource rnd = level.getRandom();
        if (rnd.nextFloat() < 0.10F) {
            level.sendParticles(particle(table), x + 0.5D, surfaceY + 0.1D, z + 0.5D, 2, 0.3D, 0.2D, 0.3D, 0.0D);
        }
        if (rnd.nextFloat() < 0.02F) {
            level.playSound(null, x + 0.5D, surfaceY, z + 0.5D,
                    SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.3F, 0.8F + rnd.nextFloat() * 0.3F);
        }
    }

    private static SimpleParticleType particle(TerraformTable table) {
        if (BuiltInRegistries.PARTICLE_TYPE.containsKey(table.particle())) {
            ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.getValue(table.particle());
            if (type instanceof SimpleParticleType simple) {
                return simple;
            }
        }
        return ParticleTypes.HAPPY_VILLAGER;
    }

    /**
     * Luna's shimmer dome: a sparse ring of glints along the terraformer's Living radius — the only world
     * whose terraformed edge glows, because there is no atmosphere to hold it. Visual only: no block, no state.
     */
    public static void shimmerDome(ServerLevel level, BlockPos center, int radius) {
        TerraformTable table = tableFor(level);
        if (table == null || !table.has(TerraformTable.Rule.SHIMMER_DOME) || radius <= 0) {
            return;
        }
        RandomSource rnd = level.getRandom();
        SimpleParticleType glint = particle(table);
        for (int i = 0; i < DOME_POINTS; i++) {
            double a = rnd.nextDouble() * Math.PI * 2.0D;
            int x = center.getX() + (int) Math.round(Math.cos(a) * radius);
            int z = center.getZ() + (int) Math.round(Math.sin(a) * radius);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            level.sendParticles(glint, x + 0.5D, y + 0.5D + rnd.nextDouble() * 6.0D, z + 0.5D, 1,
                    0.1D, 0.4D, 0.1D, 0.0D);
        }
    }
}
