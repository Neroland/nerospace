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

/**
 * The craftable launch mount: a short pad a rocket is assembled onto. A {@link RocketItem} may only
 * place a {@link RocketEntity} directly above one of these blocks, so the pad doubles as the
 * "assembly point" gate for a launch. Empty-hand right-click prints a formation report (cluster size,
 * largest formed square, modules, and what is missing).
 */
public class RocketLaunchPadBlock extends Block {

    /** The plate's top surface, in blocks — rockets stand at pad Y + this. */
    public static final double SURFACE_HEIGHT = 3.0D / 16.0D;

    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D);

    public RocketLaunchPadBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
        int tier = LaunchPadMultiblock.padTier(level, pads);
        // Summary line: cluster size + the highest rocket tier this pad can launch.
        player.sendSystemMessage(Component.translatable(
                "block.nerospace.rocket_launch_pad.report.summary", pads.size(), tier));
        // Next-tier build hint (or "max" once the Heavy Launch Complex is formed).
        String hint = switch (tier) {
            case 1 -> "block.nerospace.rocket_launch_pad.report.next_t2";
            case 2 -> "block.nerospace.rocket_launch_pad.report.next_t3";
            case 3 -> "block.nerospace.rocket_launch_pad.report.next_t4";
            default -> "block.nerospace.rocket_launch_pad.report.max";
        };
        player.sendSystemMessage(Component.translatable(hint));
        return InteractionResult.SUCCESS;
    }
}
