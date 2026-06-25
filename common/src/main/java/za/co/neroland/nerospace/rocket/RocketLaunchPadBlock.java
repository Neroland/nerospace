package za.co.neroland.nerospace.rocket;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * The craftable launch mount: a short pad a rocket is assembled onto. A {@link RocketItem} may only
 * place a {@link RocketEntity} directly above one of these blocks, so the pad doubles as the
 * "assembly point" gate for a launch. Empty-hand right-click prints a formation report (cluster size,
 * largest formed square, modules, and what is missing).
 */
public class RocketLaunchPadBlock extends Block {

    /** The plate's top surface, in blocks — rockets stand at pad Y + this. */
    public static final double SURFACE_HEIGHT = 3.0D / 16.0D;

    private static final @NonNull VoxelShape SHAPE = NerospaceCommon.requireNonNull(
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D));

    public RocketLaunchPadBlock(Properties properties) {
        super(NerospaceCommon.requireNonNull(properties));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return NerospaceCommon.requireNonNull(SHAPE);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return NerospaceCommon.requireNonNull(SHAPE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Only report on a truly empty hand: this path also runs while HOLDING items (the 26.1
        // interaction order tries it before Item#useOn), and consuming the click here would swallow
        // rocket-item deployment onto the pad.
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Set<BlockPos> pads = LaunchPadMultiblock.connectedPads(level, pos);
        BlockPos corner5 = LaunchPadMultiblock.fullSquareCorner(pads, 5);
        boolean full3 = LaunchPadMultiblock.isFullThreeByThree(pads);
        boolean heavy = LaunchPadMultiblock.isHeavyComplex(level, pads);
        boolean ring = LaunchPadMultiblock.hasStationWallRing(level, pads);

        String formed = heavy ? "heavy" : (corner5 != null ? "5x5" : (full3 ? "3x3" : "none"));
        player.sendSystemMessage(Component.translatable(
                "block.nerospace.rocket_launch_pad.report." + formed, pads.size()));
        if (corner5 != null && !heavy) {
            player.sendSystemMessage(Component.translatable(
                    "block.nerospace.rocket_launch_pad.report.need_gantry"));
        }
        if (full3 || heavy) {
            player.sendSystemMessage(Component.translatable(heavy || ring
                    ? "block.nerospace.rocket_launch_pad.report.t3_ready"
                    : "block.nerospace.rocket_launch_pad.report.t3_not_ready"));
        }
        return InteractionResult.SUCCESS;
    }
}
