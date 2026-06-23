package za.co.neroland.nerospace.meteor;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Creative-only Meteor Caller (meteor-events design §7): right-click a block to call a meteor down
 * onto that spot with freshly rolled RNG loot — the same path natural spawning uses, on demand.
 * Functions only for creative-mode players; in survival it does nothing (and says so).
 */
public class MeteorCallerItem extends Item {

    public MeteorCallerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.getAbilities().instabuild) {
            if (player != null && !context.getLevel().isClientSide()) {
                player.sendSystemMessage(Component.translatable("item.nerospace.meteor_caller.creative_only"));
            }
            return InteractionResult.PASS;
        }

        if (context.getLevel() instanceof ServerLevel level) {
            BlockPos target = context.getClickedPos();
            FallingMeteorEntity.spawn(level, target, level.getRandom().nextLong());
            player.sendSystemMessage(Component.translatable("item.nerospace.meteor_caller.called"));
        }
        return InteractionResult.SUCCESS;
    }
}
