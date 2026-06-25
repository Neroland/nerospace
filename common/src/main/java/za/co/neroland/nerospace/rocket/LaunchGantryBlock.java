package za.co.neroland.nerospace.rocket;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;


/**
 * The Launch Gantry module: placed on a 5x5 pad's border ring it forms the Heavy Launch Complex
 * (Tier 3 without the Station-Wall ring; Tier 4 launch infrastructure). Right-click boards the rocket
 * standing on the adjacent pad — the service-tower QoL, no more pixel-hunting the entity.
 */
public class LaunchGantryBlock extends Block {

    public LaunchGantryBlock(Properties properties) {
        super(java.util.Objects.requireNonNull(properties));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pad = LaunchPadMultiblock.adjacentPad(level, pos);
        Set<BlockPos> pads = pad == null ? Set.of() : LaunchPadMultiblock.connectedPads(level, pad);
        RocketEntity rocket = LaunchPadMultiblock.rocketAbove(level, pads);
        if (rocket == null) {
            player.sendSystemMessage(Component.translatable("block.nerospace.launch_gantry.no_rocket"));
            return InteractionResult.SUCCESS;
        }
        if (player.startRiding(rocket)) {
            player.sendSystemMessage(Component.translatable("block.nerospace.launch_gantry.boarded"));
        }
        return InteractionResult.SUCCESS;
    }
}
