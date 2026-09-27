package za.co.neroland.nerospace.route;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.rocket.LaunchPadMultiblock;

/**
 * Deploys a {@link CargoRocketEntity} onto a Cargo Pad. Unlike the crewed {@code RocketItem} it only
 * accepts a {@link CargoPadBlock} (a cargo rocket on a plain launch pad could never be launched), and it
 * stands the rocket on the Cargo Pad block itself so the pad's "docked rocket" lookup is unambiguous.
 */
public class CargoRocketItem extends Item {

    public CargoRocketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof CargoPadBlock)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        Set<BlockPos> pads = LaunchPadMultiblock.connectedPads(level, pos);
        if (LaunchPadMultiblock.rocketAbove(level, pads) != null) {
            if (player != null) {
                player.sendSystemMessage(Component.translatable("item.nerospace.rocket.pad_occupied"));
            }
            return InteractionResult.SUCCESS;
        }
        level.addFreshEntity(CargoRocketEntity.standOn(level, pos));
        ItemStack stack = context.getItemInHand();
        if (player != null && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (player != null) {
            int tier = LaunchPadMultiblock.padTier(level, pads);
            player.sendSystemMessage(tier >= CargoLaunch.REQUIRED_PAD_TIER
                    ? Component.translatable("item.nerospace.cargo_rocket.deployed")
                    : Component.translatable("item.nerospace.cargo_rocket.deployed_need_pad", tier,
                            CargoLaunch.REQUIRED_PAD_TIER));
        }
        return InteractionResult.SUCCESS;
    }
}
