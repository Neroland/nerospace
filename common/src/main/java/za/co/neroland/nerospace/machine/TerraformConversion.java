package za.co.neroland.nerospace.machine;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
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
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.registry.ModTags;
import za.co.neroland.nerospace.world.ModBiomes;
import za.co.neroland.nerospace.world.TerraformFauna;

/**
 * Shared, idempotent surface-column conversion for the Terraformer (terraform design §2.2; staged per
 * DEEPER_TERRAFORM_DESIGN.md). Extracted so both the live frontier ({@link TerraformerBlockEntity})
 * and the chunk-load catch-up rescan ({@code TerraformManager}) convert columns identically. The
 * caller guarantees the column's chunk is loaded.
 *
 * <p>Stages: {@link #convertColumn} = stage 1 (Rooted), {@link #hydrateColumn} = stage 2 (Hydrated),
 * {@link #vivifyColumn} = stage 3 (Living). Each is a per-stage no-op when re-run, so persistence stays
 * radii + cursors.</p>
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

    private TerraformConversion() {
    }

    // --- Stage bookkeeping (DEEPER_TERRAFORM_DESIGN.md §2.2) ----------------

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

    /** Convert one surface column: terrain → grass/dirt, flag breathable, write biome, plants, ore. */
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
        BlockPos top = new BlockPos(x, surfaceY - 1, z);
        BlockState topState = level.getBlockState(top);

        if (topState.is(ModTags.Blocks.TERRAFORM_TO_GRASS) && !topState.is(Blocks.GRASS_BLOCK)) {
            level.setBlock(top, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int d = 1; d <= 3; d++) {
                BlockPos below = new BlockPos(x, surfaceY - 1 - d, z);
                if (level.getBlockState(below).is(ModTags.Blocks.TERRAFORM_TO_DIRT)) {
                    level.setBlock(below, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            frontierFx(level, x, surfaceY, z);
        }

        // Atmosphere payoff (§3.4): flag the chunk permanently breathable at/above the surface.
        LevelChunk chunk = level.getChunkAt(top);
        if (!Services.PLATFORM.isTerraformed(chunk)) {
            Services.PLATFORM.setTerraformed(chunk, true);
            chunk.markUnsaved();
        }
        bumpStage(chunk, 1);

        if (writeBiomeColumn(level, chunk, x, z, ModBiomes.TERRAFORMED)) {
            biomeChanged.add(chunk);
        }

        scatterPlant(level, x, surfaceY, z);
        seedResource(level, x, surfaceY, z, tier);
    }

    // --- Stage 2: Hydrated (DEEPER_TERRAFORM_DESIGN.md §3.2) ----------------

    /**
     * Stage-2 conversion of one column: fill the basin below {@code waterTableY} with still water,
     * one hydration unit per source placed. The scan walks down from the table; columns whose terrain
     * sits at/above the table get no water (dry hills), and a chasm deeper than {@code WATER_MAX_DEPTH}
     * is skipped entirely rather than part-filled mid-air.
     *
     * @param sink hydration supply, or {@code null} for the free chunk-load catch-up
     * @return {@code false} when the sink ran dry mid-column (stall: re-run this column later);
     *         {@code true} when the column is fully hydrated (possibly needing no water at all)
     */
    public static boolean hydrateColumn(ServerLevel level, int x, int z, int waterTableY, HydrationSink sink) {
        LevelChunk chunk = level.getChunkAt(new BlockPos(x, 0, z));
        if (WATER_ENABLED) {
            int maxDepth = WATER_MAX_DEPTH;
            int table = Math.max(waterTableY, level.getMinY() + 1);

            // Find the basin floor: first non-fillable cell at/below the table, within the depth cap.
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, table, z);
            int floorY = Integer.MIN_VALUE;
            for (int depth = 0; depth <= maxDepth; depth++) {
                int y = table - depth;
                if (y <= level.getMinY()) {
                    break;
                }
                cursor.setY(y);
                BlockState state = level.getBlockState(cursor);
                if (!isWaterFillable(state)) {
                    floorY = y;
                    break;
                }
            }

            if (floorY != Integer.MIN_VALUE && floorY < table) {
                // Fill bottom-up so a stall never leaves water floating above a gap.
                for (int y = floorY + 1; y <= table; y++) {
                    cursor.setY(y);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(Blocks.WATER)) {
                        continue; // already hydrated — idempotent, no cost
                    }
                    if (!state.isAir() && !state.canBeReplaced()) {
                        break; // overhang closed the basin above this point
                    }
                    if (sink != null && sink.draw(1) <= 0) {
                        return false; // out of glacite — stall, keep the cursor on this column
                    }
                    level.setBlock(cursor, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        bumpStage(chunk, 2);
        return true;
    }

    /** A cell the water fill may occupy: air, water, or replaceable ground cover (grass/flowers). */
    private static boolean isWaterFillable(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER) || state.canBeReplaced();
    }

    // --- Stage 3: Living (DEEPER_TERRAFORM_DESIGN.md §4–5) ------------------

    /**
     * Stage-3 conversion of one column: settle the mature per-planet biome (real weather), grow the
     * occasional tree, and seed a starter herd of the planet's livestock. Idempotent per stage.
     */
    public static void vivifyColumn(ServerLevel level, int x, int z, Set<LevelChunk> biomeChanged) {
        LevelChunk chunk = level.getChunkAt(new BlockPos(x, 0, z));
        if (writeBiomeColumn(level, chunk, x, z, matureBiomeFor(level))) {
            biomeChanged.add(chunk);
        }
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        placeTree(level, x, surfaceY, z);
        TerraformFauna.seedHerd(level, x, surfaceY, z);
        bumpStage(chunk, 3);
    }

    /** The mature stage-3 biome for this dimension (§4); unknown dimensions settle as meadow. */
    public static ResourceKey<Biome> matureBiomeFor(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        if (ModDimensions.CINDARA_LEVEL.equals(dimension)) {
            return ModBiomes.TERRAFORMED_SAVANNA;
        }
        if (ModDimensions.GLACIRA_LEVEL.equals(dimension)) {
            return ModBiomes.TERRAFORMED_TUNDRA;
        }
        return ModBiomes.TERRAFORMED_MEADOW;
    }

    /** Sparse GROWN trees on Living ground (§4): vanilla features, themed per planet. */
    private static void placeTree(ServerLevel level, int x, int surfaceY, int z) {
        if (!PLANTS_ENABLED || level.getRandom().nextDouble() >= TREE_CHANCE) {
            return;
        }
        BlockPos ground = new BlockPos(x, surfaceY - 1, z);
        BlockPos above = ground.above();
        if (!level.getBlockState(ground).is(Blocks.GRASS_BLOCK) || !level.getBlockState(above).isAir()) {
            return;
        }
        ResourceKey<Level> dimension = level.dimension();
        ResourceKey<ConfiguredFeature<?, ?>> tree;
        if (ModDimensions.CINDARA_LEVEL.equals(dimension)) {
            tree = TreeFeatures.ACACIA;
        } else if (ModDimensions.GLACIRA_LEVEL.equals(dimension)) {
            tree = TreeFeatures.SPRUCE;
        } else {
            tree = level.getRandom().nextBoolean() ? TreeFeatures.OAK : TreeFeatures.BIRCH;
        }
        level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(tree)
                .ifPresent(holder -> holder.value().place(
                        level, level.getChunkSource().getGenerator(), level.getRandom(), above));
    }

    /** Write {@code biomeKey} down this column's sections. @return true if anything changed. */
    @SuppressWarnings("unchecked")
    private static boolean writeBiomeColumn(ServerLevel level, LevelChunk chunk, int x, int z,
            ResourceKey<Biome> biomeKey) {
        Holder<Biome> terra = level.registryAccess()
                .lookupOrThrow(Registries.BIOME).getOrThrow(
                        za.co.neroland.nerospace.NerospaceCommon.requireNonNull(biomeKey));
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

    /** Sparse grass/flower/sapling scatter on freshly grassed ground (terraform design §2.2). */
    private static void scatterPlant(ServerLevel level, int x, int surfaceY, int z) {
        if (!PLANTS_ENABLED) {
            return;
        }
        RandomSource rnd = level.getRandom();
        if (rnd.nextDouble() >= PLANT_CHANCE) {
            return;
        }
        BlockPos ground = new BlockPos(x, surfaceY - 1, z);
        BlockPos above = new BlockPos(x, surfaceY, z);
        if (!level.getBlockState(ground).is(Blocks.GRASS_BLOCK) || !level.getBlockState(above).isAir()) {
            return;
        }
        double roll = rnd.nextDouble();
        Block plant;
        if (roll < 0.06D) {
            plant = Blocks.OAK_SAPLING;
        } else if (roll < 0.30D) {
            plant = switch (rnd.nextInt(4)) {
                case 0 -> Blocks.POPPY;
                case 1 -> Blocks.DANDELION;
                case 2 -> Blocks.CORNFLOWER;
                default -> Blocks.AZURE_BLUET;
            };
        } else {
            plant = Blocks.SHORT_GRASS;
        }
        level.setBlock(above, plant.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    /** Tier-3 low-rate ore seeding into the converted subsurface (terraform design §2.2 / §T3). */
    private static void seedResource(ServerLevel level, int x, int surfaceY, int z, int tier) {
        if (tier < 3 || !RESOURCES_ENABLED) {
            return;
        }
        RandomSource rnd = level.getRandom();
        if (rnd.nextDouble() >= RESOURCE_CHANCE) {
            return;
        }
        Block ore = TerraformResources.pickOre(rnd);
        if (ore == null) {
            return;
        }
        int y = surfaceY - 4 - rnd.nextInt(8);
        BlockPos orePos = new BlockPos(x, y, z);
        if (level.hasChunk(orePos.getX() >> 4, orePos.getZ() >> 4)
                && level.getBlockState(orePos).is(ModTags.Blocks.TERRAFORM_TO_DIRT)) {
            level.setBlock(orePos, ore.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Sparse green frontier dust + a soft soil sound as a shell converts (terraform design §2.4). */
    private static void frontierFx(ServerLevel level, int x, int surfaceY, int z) {
        RandomSource rnd = level.getRandom();
        if (rnd.nextFloat() < 0.10F) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    x + 0.5D, surfaceY + 0.1D, z + 0.5D, 2, 0.3D, 0.2D, 0.3D, 0.0D);
        }
        if (rnd.nextFloat() < 0.02F) {
            level.playSound(null, x + 0.5D, surfaceY, z + 0.5D,
                    SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.3F, 0.8F + rnd.nextFloat() * 0.3F);
        }
    }
}
