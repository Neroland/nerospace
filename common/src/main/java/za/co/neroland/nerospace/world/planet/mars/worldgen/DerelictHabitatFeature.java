package za.co.neroland.nerospace.world.planet.mars.worldgen;

//? if <26.3 {
import com.mojang.serialization.Codec;
//?}

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
//? if <26.3 {
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
//?}

import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.world.AlienBuild;
import za.co.neroland.nerospace.world.SettlementPlacement;
import za.co.neroland.nerospace.world.StructureRegion;
import za.co.neroland.nerospace.world.StructureSpacing;
import za.co.neroland.nerospace.world.planet.luna.LunaContent;
import za.co.neroland.nerospace.world.planet.mars.MarsContent;
import za.co.neroland.nerospace.world.station.StationContent;

/**
 * Derelict Habitat (Mars) — the ruin of an early surface habitat: a round station-floor pad, a cracked hull ring
 * with a window band, a half-collapsed dome, regolith drifted through the breaches, a dead console and a supply
 * locker. No villagers, no village core. Spaced by {@link StructureSpacing} with the Mars salt; every write stays in
 * the feature's 3×3-chunk region.
 */
//? if >=26.3 {
/*public class DerelictHabitatFeature implements Feature {
*///?} else {
public class DerelictHabitatFeature extends Feature<NoneFeatureConfiguration> {
//?}

    //? if >=26.3 {
    /*// Minecraft 26.3+: features are data-driven values; this configuration-free type has one instance.
    public static final DerelictHabitatFeature INSTANCE = new DerelictHabitatFeature();
    public static final com.mojang.serialization.MapCodec<DerelictHabitatFeature> CODEC = com.mojang.serialization.MapCodec.unit(INSTANCE);

    @Override
    public com.mojang.serialization.MapCodec<? extends Feature> codec() {
        return CODEC;
    }
    *///?}
    //? if <26.3 {
    public DerelictHabitatFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }
    //?}

    //? if >=26.3 {
    /*@Override
    public boolean place(WorldGenLevel level, net.minecraft.world.level.chunk.ChunkGenerator generator, RandomSource rand,
            BlockPos o) {
        return generate(level, rand, o);
    }
    *///?} else {
    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        return generate(ctx.level(), ctx.random(), ctx.origin());
    }
    //?}

    /** Hull radius; with the floor rim the habitat fits in ±{@value #FOOTPRINT_R}. */
    public static final int HULL_R = 5;
    public static final int FOOTPRINT_R = HULL_R + 1;
    private static final int WALL_H = 4;

    /** Version-neutral body shared by both feature APIs. */
    private boolean generate(WorldGenLevel level, RandomSource rand, BlockPos o) {
        StructureRegion region = SettlementPlacement.beginPlain(level, o, StructureSpacing.Roi.DERELICT_HABITAT);
        if (region == null) {
            return false;
        }
        AlienBuild.Site site = new AlienBuild.Site(level, AlienBuild.XERTZ, region);
        int baseY = o.getY();
        BlockState floor = ModBlocks.STATION_FLOOR.get().defaultBlockState();
        BlockState hull = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState rib = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState window = Blocks.GLASS.defaultBlockState();
        BlockState regolith = MarsContent.RED_REGOLITH.get().defaultBlockState();
        int inner = (HULL_R - 1) * (HULL_R - 1) + HULL_R - 1;
        int outer = HULL_R * HULL_R + HULL_R;

        for (int dx = -FOOTPRINT_R; dx <= FOOTPRINT_R; dx++) {
            for (int dz = -FOOTPRINT_R; dz <= FOOTPRINT_R; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > outer) {
                    continue;
                }
                int x = o.getX() + dx;
                int z = o.getZ() + dz;
                site.set(x, baseY - 1, z, floor);
                for (int dy = 0; dy <= WALL_H + 2; dy++) {
                    site.set(x, baseY + dy, z, AlienBuild.air());
                }
                boolean wall = d2 > inner;
                boolean door = dz < 0 && Math.abs(dx) <= 1;
                if (wall && !door) {
                    boolean isRib = dx == 0 || dz == 0 || Math.abs(dx) == Math.abs(dz);
                    for (int dy = 0; dy < WALL_H; dy++) {
                        if (!isRib && rand.nextFloat() < 0.25F) {
                            continue; // breaches
                        }
                        site.set(x, baseY + dy, z, isRib ? rib : (dy == 2 ? window : hull));
                    }
                } else if (!wall && rand.nextFloat() < 0.2F) {
                    site.set(x, baseY, z, regolith); // drifted in through the breaches
                }
                // Half-collapsed dome.
                if (d2 <= inner && rand.nextFloat() < 0.45F) {
                    site.set(x, baseY + WALL_H, z, hull);
                }
                if (d2 <= 4 && rand.nextFloat() < 0.4F) {
                    site.set(x, baseY + WALL_H + 1, z, window);
                }
            }
        }

        site.set(o.getX() - 2, baseY, o.getZ() + 2, Blocks.LODESTONE.defaultBlockState()); // the dead console
        BlockPos chestPos = new BlockPos(o.getX() + 2, baseY, o.getZ() + 2);
        site.set(chestPos.getX(), chestPos.getY(), chestPos.getZ(), Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setItem(4, new ItemStack(LunaContent.TITANIUM_INGOT.get(), 1 + rand.nextInt(3)));
            chest.setItem(6, new ItemStack(StationContent.HULL_PLATING_ITEM.get(), 2 + rand.nextInt(3)));
            chest.setItem(10, new ItemStack(MarsContent.PERCHLORATE_OXIDISER.get(), 1 + rand.nextInt(3)));
            chest.setItem(13, new ItemStack(ModItems.NEROSTEEL_INGOT.get(), 1 + rand.nextInt(4)));
            if (rand.nextFloat() < 0.3F) {
                chest.setItem(22, new ItemStack(ModItems.ROCKET_FUEL_CANISTER.get(), 1));
            }
        }
        return true;
    }
}
