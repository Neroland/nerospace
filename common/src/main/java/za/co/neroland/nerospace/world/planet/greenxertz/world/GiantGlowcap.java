package za.co.neroland.nerospace.world.planet.greenxertz.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerospace.block.rules.GrowthRules;
import za.co.neroland.nerospace.registry.ModTags;
import za.co.neroland.nerospace.world.planet.greenxertz.GreenxertzContent;
import za.co.neroland.nerospace.world.planet.greenxertz.block.XertzVineBlock;

/**
 * The Giant Glowcap: a Bio-Bark Log stem under a 3x3 Glowcap Block cap, with a few Xertz Vines hanging
 * from the cap rim. Shared by bone meal on a Glowcap Fungus and by the worldgen feature, so both obey the same
 * {@link GrowthRules#giantFits} gate: Xertz Soil below and five air cells above the fungus. Only air is replaced.
 */
public final class GiantGlowcap {

    /** Stem height when grown from bone meal: it then fits the five-air gate exactly (4 stem + 1 cap). */
    public static final int STEM = 4;
    /** Worldgen stems are 4-6 tall (still gated on the air they need). */
    public static final int MAX_STEM = 6;
    public static final int MAX_VINES = 3;
    public static final int MAX_VINE_LENGTH = 3;

    private GiantGlowcap() {
    }

    /** Air cells directly above {@code pos}, counted up to {@code limit}. */
    static int airAbove(LevelReader level, BlockPos pos, int limit) {
        BlockPos.MutableBlockPos m = pos.mutable();
        int n = 0;
        while (n < limit) {
            m.move(Direction.UP);
            if (!level.isInsideBuildHeight(m.getY()) || !level.getBlockState(m).isAir()) {
                break;
            }
            n++;
        }
        return n;
    }

    public static boolean fits(LevelReader level, BlockPos pos) {
        boolean soil = level.getBlockState(pos.below()).is(ModTags.Blocks.XERTZ_ROOTABLE);
        return GrowthRules.giantFits(airAbove(level, pos, GrowthRules.AIR_NEEDED), soil);
    }

    /** Stem height for a free column of {@code air} cells: bone meal uses {@link #STEM}; worldgen rolls 4-6. */
    public static int stemHeight(int air, int roll, boolean worldgen) {
        int wanted = worldgen ? STEM + Math.floorMod(roll, MAX_STEM - STEM + 1) : STEM;
        return Math.max(STEM, Math.min(wanted, air - 1));
    }

    /**
     * Grows the giant at {@code pos} (the fungus cell, or the surface air cell in worldgen). Returns false and changes
     * nothing when the gate fails.
     */
    public static boolean grow(LevelAccessor level, BlockPos pos, RandomSource random, boolean worldgen) {
        if (!level.getBlockState(pos.below()).is(ModTags.Blocks.XERTZ_ROOTABLE)) {
            return false;
        }
        int air = airAbove(level, pos, MAX_STEM + 1);
        if (!GrowthRules.giantFits(air, true)) {
            return false;
        }
        int stem = stemHeight(air, random.nextInt(16), worldgen);
        BlockState log = GreenxertzContent.BIO_BARK_LOG.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState cap = GreenxertzContent.GLOWCAP_BLOCK.get().defaultBlockState();
        for (int dy = 0; dy < stem; dy++) {
            level.setBlock(pos.above(dy), log, Block.UPDATE_CLIENTS);
        }
        BlockPos capCentre = pos.above(stem);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos c = capCentre.offset(dx, 0, dz);
                if (level.getBlockState(c).isAir()) {
                    level.setBlock(c, cap, Block.UPDATE_CLIENTS);
                }
            }
        }
        hangVines(level, capCentre, random);
        return true;
    }

    private static void hangVines(LevelAccessor level, BlockPos capCentre, RandomSource random) {
        BlockState vine = GreenxertzContent.XERTZ_VINE.get().defaultBlockState();
        int vines = random.nextInt(MAX_VINES + 1);
        for (int i = 0; i < vines; i++) {
            int dx = random.nextInt(3) - 1;
            int dz = random.nextInt(3) - 1;
            if (dx == 0 && dz == 0) {
                continue; // the stem
            }
            BlockPos top = capCentre.offset(dx, -1, dz);
            int length = 1 + random.nextInt(MAX_VINE_LENGTH);
            for (int d = 0; d < length; d++) {
                BlockPos p = top.below(d);
                if (!level.getBlockState(p).isAir()) {
                    break;
                }
                boolean last = d == length - 1 || !level.getBlockState(p.below()).isAir();
                level.setBlock(p, vine.setValue(XertzVineBlock.TIP, last), Block.UPDATE_CLIENTS);
                if (last) {
                    break;
                }
            }
        }
    }
}
