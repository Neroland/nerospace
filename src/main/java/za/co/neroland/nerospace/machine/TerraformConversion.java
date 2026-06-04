package za.co.neroland.nerospace.machine;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
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

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.registry.ModAttachments;
import za.co.neroland.nerospace.registry.ModTags;
import za.co.neroland.nerospace.world.ModBiomes;

/**
 * Shared, idempotent surface-column conversion for the Terraformer (terraform design §2.2). Extracted
 * so both the live frontier ({@link TerraformerBlockEntity}) and the chunk-load catch-up rescan
 * ({@code TerraformManager}) convert columns identically. The caller guarantees the column's chunk is
 * loaded.
 */
public final class TerraformConversion {

    private TerraformConversion() {
    }

    /** Convert one surface column: terrain → grass/dirt, flag breathable, write biome, plants, ore. */
    public static void convertColumn(ServerLevel level, int x, int z, int tier, Set<LevelChunk> biomeChanged) {
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
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
        if (!Boolean.TRUE.equals(chunk.getData(ModAttachments.TERRAFORMED))) {
            chunk.setData(ModAttachments.TERRAFORMED, Boolean.TRUE);
            chunk.markUnsaved();
        }

        if (writeTerraformedBiome(level, chunk, x, z)) {
            biomeChanged.add(chunk);
        }

        scatterPlant(level, x, surfaceY, z);
        seedResource(level, x, surfaceY, z, tier);
    }

    /** Write the vibrant terraformed biome down this column's sections. @return true if anything changed. */
    private static boolean writeTerraformedBiome(ServerLevel level, LevelChunk chunk, int x, int z) {
        Holder<Biome> terra = level.registryAccess()
                .lookupOrThrow(Registries.BIOME).getOrThrow(ModBiomes.TERRAFORMED);
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
        if (!Config.TERRAFORM_PLANTS_ENABLED.get()) {
            return;
        }
        RandomSource rnd = level.getRandom();
        if (rnd.nextDouble() >= Config.TERRAFORM_PLANT_CHANCE.get()) {
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
        if (tier < 3 || !Config.TERRAFORM_RESOURCES_ENABLED.get()) {
            return;
        }
        RandomSource rnd = level.getRandom();
        if (rnd.nextDouble() >= Config.TERRAFORM_RESOURCE_CHANCE.get()) {
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
