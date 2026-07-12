package za.co.neroland.nerospace.world;

import com.mojang.serialization.Codec;

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
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Living Mega-City — the massive end-state alien settlement: a pillared, lit, crenellated curtain wall
 * with four gates around a glowing tile plaza of towers, and a central keep guarded by the Ruin Warden
 * boss over a grand vault. Very rare, spaced via {@link StructureSpacing}.
 */
public class MegaCityFeature extends Feature<NoneFeatureConfiguration> {

    private static final int WALL_R = 20;   // 41x41 footprint
    private static final int WALL_H = 6;

    public MegaCityFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        BlockPos o = ctx.origin();
        if (!StructureSpacing.shouldPlace(o, StructureSpacing.Roi.MEGA_CITY)) {
            return false;
        }
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        int baseY = o.getY();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockState bricks = AlienBuild.bricks();

        for (int dx = -WALL_R; dx <= WALL_R; dx++) {
            for (int dz = -WALL_R; dz <= WALL_R; dz++) {
                int x = o.getX() + dx;
                int z = o.getZ() + dz;
                m.set(x, baseY - 1, z);
                level.setBlock(m, AlienBuild.tile(), 2);
                boolean perimeter = Math.abs(dx) == WALL_R || Math.abs(dz) == WALL_R;
                boolean gate = (Math.abs(dx) <= 1 && Math.abs(dz) == WALL_R)
                        || (Math.abs(dz) <= 1 && Math.abs(dx) == WALL_R);
                if (!perimeter || gate) {
                    continue;
                }
                boolean pillar = (dx % 5 == 0) || (dz % 5 == 0) || (Math.abs(dx) == WALL_R && Math.abs(dz) == WALL_R);
                int top = pillar ? WALL_H + 1 : WALL_H - 1;
                for (int dy = 0; dy <= top; dy++) {
                    m.set(x, baseY + dy, z);
                    level.setBlock(m, pillar ? AlienBuild.pillar() : bricks, 2);
                }
                if (pillar) {
                    m.set(x, baseY + top + 1, z);
                    level.setBlock(m, AlienBuild.lamp(), 2);
                } else if ((dx + dz) % 2 == 0) {
                    m.set(x, baseY + top + 1, z);
                    level.setBlock(m, bricks, 2);
                }
            }
        }
        for (int[] g : new int[][] {{0, WALL_R}, {0, -WALL_R}, {WALL_R, 0}, {-WALL_R, 0}}) {
            for (int s : new int[] {-2, 2}) {
                int gx = o.getX() + (g[0] == 0 ? s : g[0]);
                int gz = o.getZ() + (g[1] == 0 ? s : g[1]);
                m.set(gx, baseY + WALL_H, gz);
                level.setBlock(m, AlienBuild.lamp(), 2);
            }
        }

        AlienBuild.tower(level, o.getX() - 11, baseY, o.getZ() - 11, 3, 6, false, rand, m);
        AlienBuild.tower(level, o.getX() + 11, baseY, o.getZ() - 11, 3, 6, false, rand, m);
        AlienBuild.tower(level, o.getX() - 11, baseY, o.getZ() + 11, 3, 6, false, rand, m);
        AlienBuild.tower(level, o.getX() + 11, baseY, o.getZ() + 11, 3, 5, false, rand, m);

        AlienBuild.tower(level, o.getX(), baseY, o.getZ(), 5, 8, false, rand, m);
        m.set(o.getX(), baseY, o.getZ());
        level.setBlock(m, ModBlocks.VILLAGE_CORE.get().defaultBlockState(), 2);
        BlockPos chestPos = new BlockPos(o.getX() + 2, baseY, o.getZ() + 2);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
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
