package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Places a {@link RocketEntity} of a fixed {@link RocketTier} onto a {@link RocketLaunchPadBlock}.
 * Using it anywhere else does nothing, so a launch pad is required to deploy a rocket.
 */
public class RocketItem extends Item {

    private final RocketTier tier;

    public RocketItem(Properties properties, RocketTier tier) {
        super(properties);
        this.tier = tier;
    }

    public RocketTier tier() {
        return this.tier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof RocketLaunchPadBlock)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            BlockPos above = pos.above();
            RocketEntity rocket = new RocketEntity(
                    level, above.getX() + 0.5D, above.getY(), above.getZ() + 0.5D, this.tier);
            level.addFreshEntity(rocket);

            Player player = context.getPlayer();
            ItemStack stack = context.getItemInHand();
            if (player != null && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (player != null) {
                player.sendSystemMessage(Component.translatable("item.nerospace.rocket.deployed"));
            }
        }

        return InteractionResult.SUCCESS;
    }
}
