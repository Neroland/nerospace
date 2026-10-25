package za.co.neroland.nerospace.world.station.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.RunPlacementRules;

/**
 * Truss: an open lattice of 3/16 bars with a walkable deck, climbable on any face (it is in
 * {@code minecraft:climbable}), with an {@code AXIS}. Sneak-placing lays a straight run of up to eight in the
 * direction the player faces, one item each, stopping at the first non-air cell ({@link RunPlacementRules}).
 */
public class TrussBlock extends RotatedPillarBlock {

    public static final MapCodec<TrussBlock> CODEC = BlockCodecs.simple(TrussBlock::new);

    private static final VoxelShape VERTICAL = Shapes.or(
            Block.box(0, 0, 0, 3, 16, 3), Block.box(13, 0, 0, 16, 16, 3),
            Block.box(0, 0, 13, 3, 16, 16), Block.box(13, 0, 13, 16, 16, 16));
    private static final VoxelShape ALONG_X = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16), Block.box(0, 13, 0, 16, 16, 3), Block.box(0, 13, 13, 16, 16, 16));
    private static final VoxelShape ALONG_Z = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16), Block.box(0, 13, 0, 3, 16, 16), Block.box(13, 13, 0, 16, 16, 16));

    public TrussBlock(Properties properties) {
        super(properties);
    }

    public MapCodec<? extends RotatedPillarBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case Y -> VERTICAL;
            case X -> ALONG_X;
            case Z -> ALONG_Z;
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || !(placer instanceof Player player) || !player.isShiftKeyDown()) {
            return;
        }
        Direction dir = Direction.orderedByNearest(player)[0];
        int extraMax = RunPlacementRules.MAX_RUN - 1;
        boolean[] path = new boolean[extraMax];
        for (int i = 0; i < extraMax; i++) {
            BlockPos p = pos.relative(dir, i + 1);
            path[i] = level.isLoaded(p) && level.getBlockState(p).isAir();
        }
        boolean creative = player.getAbilities().instabuild;
        // The clicked block is consumed by the BlockItem after this call; the run spends the rest of the stack.
        int available = creative ? extraMax : Math.max(0, stack.getCount() - 1);
        int n = RunPlacementRules.runLength(available, path, extraMax);
        BlockState runState = defaultBlockState().setValue(AXIS, dir.getAxis());
        for (int i = 0; i < n; i++) {
            level.setBlock(pos.relative(dir, i + 1), runState, Block.UPDATE_ALL);
        }
        if (!creative && n > 0) {
            stack.shrink(n);
        }
    }
}
