package za.co.neroland.nerospace.world;

//? if <26.3 {
import com.mojang.serialization.Codec;
//?}

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
//? if <26.3 {
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
//?}

import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Living Mega-City — the massive end-state alien settlement: a pillared, lit, crenellated curtain wall
 * with four gates around a glowing tile plaza of towers, and a central keep guarded by the Ruin Warden
 * boss over a grand vault. Very rare, spaced via {@link StructureSpacing}. Built by the Xertz (Greenxertz) and the
 * Duskborn (Nyxara twilight band only), each in its own palette; centred on its chunk so every write stays inside
 * the feature's 3×3-chunk region.
 */
//? if >=26.3 {
/*public class MegaCityFeature implements Feature {
*///?} else {
public class MegaCityFeature extends Feature<NoneFeatureConfiguration> {
//?}

    private static final int WALL_R = MegaCityLayout.WALL_R;   // 41x41 footprint
    private static final int WALL_H = 6;
    private static final int CORNER_OFFSET = MegaCityLayout.CORNER_OFFSET;
    private static final int CORNER_R = MegaCityLayout.CORNER_R;
    private static final int KEEP_R = MegaCityLayout.KEEP_R;

    //? if >=26.3 {
    /*// Minecraft 26.3+: features are data-driven values; this configuration-free type has one instance.
    public static final MegaCityFeature INSTANCE = new MegaCityFeature();
    public static final com.mojang.serialization.MapCodec<MegaCityFeature> CODEC = com.mojang.serialization.MapCodec.unit(INSTANCE);

    @Override
    public com.mojang.serialization.MapCodec<? extends Feature> codec() {
        return CODEC;
    }
    *///?}
    //? if <26.3 {
    public MegaCityFeature(Codec<NoneFeatureConfiguration> codec) {
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

    /** Version-neutral body shared by both feature APIs. */
    private boolean generate(WorldGenLevel level, RandomSource rand, BlockPos origin) {
        AlienBuild.Site site = SettlementPlacement.begin(level, origin, StructureSpacing.Roi.MEGA_CITY);
        if (site == null) {
            return false;
        }
        int cx = MegaCityLayout.centreFor(origin.getX());
        int cz = MegaCityLayout.centreFor(origin.getZ());
        int baseY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz);
        BlockPos o = new BlockPos(cx, baseY, cz);
        BlockState bricks = site.bricks();

        for (int dx = -WALL_R; dx <= WALL_R; dx++) {
            for (int dz = -WALL_R; dz <= WALL_R; dz++) {
                int x = o.getX() + dx;
                int z = o.getZ() + dz;
                site.set(x, baseY - 1, z, site.tile());
                boolean perimeter = Math.abs(dx) == WALL_R || Math.abs(dz) == WALL_R;
                boolean gate = (Math.abs(dx) <= 1 && Math.abs(dz) == WALL_R)
                        || (Math.abs(dz) <= 1 && Math.abs(dx) == WALL_R);
                if (!perimeter || gate) {
                    continue;
                }
                boolean pillar = (dx % 5 == 0) || (dz % 5 == 0) || (Math.abs(dx) == WALL_R && Math.abs(dz) == WALL_R);
                int top = pillar ? WALL_H + 1 : WALL_H - 1;
                for (int dy = 0; dy <= top; dy++) {
                    site.set(x, baseY + dy, z, pillar ? site.pillar() : bricks);
                }
                if (pillar) {
                    site.set(x, baseY + top + 1, z, site.lamp());
                } else if ((dx + dz) % 2 == 0) {
                    site.set(x, baseY + top + 1, z, bricks);
                }
            }
        }
        for (int[] g : new int[][] {{0, WALL_R}, {0, -WALL_R}, {WALL_R, 0}, {-WALL_R, 0}}) {
            for (int s : new int[] {-2, 2}) {
                int gx = o.getX() + (g[0] == 0 ? s : g[0]);
                int gz = o.getZ() + (g[1] == 0 ? s : g[1]);
                site.set(gx, baseY + WALL_H, gz, site.lamp());
            }
        }

        int c = CORNER_OFFSET;
        AlienBuild.tower(site, o.getX() - c, baseY, o.getZ() - c, CORNER_R, 6, false, rand);
        AlienBuild.tower(site, o.getX() + c, baseY, o.getZ() - c, CORNER_R, 6, false, rand);
        AlienBuild.tower(site, o.getX() - c, baseY, o.getZ() + c, CORNER_R, 6, false, rand);
        AlienBuild.tower(site, o.getX() + c, baseY, o.getZ() + c, CORNER_R, 5, false, rand);

        AlienBuild.tower(site, o.getX(), baseY, o.getZ(), KEEP_R, 8, false, rand);
        site.set(o.getX(), baseY, o.getZ(), ModBlocks.VILLAGE_CORE.get().defaultBlockState());
        BlockPos chestPos = new BlockPos(o.getX() + 2, baseY, o.getZ() + 2);
        site.set(chestPos.getX(), chestPos.getY(), chestPos.getZ(), Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setItem(4, new ItemStack(ModItems.ALIEN_CORE.get(), 2 + rand.nextInt(3)));
            chest.setItem(6, new ItemStack(ModItems.GRAV_STRIDERS.get(), 1));
            chest.setItem(10, new ItemStack(ModItems.XERTZ_RESONATOR.get(), 1));
            chest.setItem(13, new ItemStack(Items.DIAMOND, 4 + rand.nextInt(6)));
            chest.setItem(22, new ItemStack(Items.EMERALD, 12 + rand.nextInt(12)));
        }
        int by = level.getHeight(Heightmap.Types.WORLD_SURFACE, o.getX(), o.getZ());
        // Spawn on the world-gen thread using the feature's own RandomSource. EntityType.spawn()/create()
        // would otherwise roll the initial yaw from the ServerLevel's random, which is owned by the server
        // thread and trips c2me's off-thread ThreadLocalRandom guard during multithreaded (Distant Horizons)
        // world generation (MC-NEROSPACE-D). Build the entity manually, position it with ctx.random(), and
        // add it through the WorldGenLevel instead.
        var warden = ModEntities.RUIN_WARDEN.get().create(level.getLevel(), EntitySpawnReason.EVENT);
        if (warden != null) {
            warden.snapTo(o.getX() + 0.5, by, o.getZ() + 0.5, rand.nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(warden);
        }
        return true;
    }
}
