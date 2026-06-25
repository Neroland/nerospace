package za.co.neroland.nerospace.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;


import za.co.neroland.nerospace.pipe.UniversalPipeBlockEntity;

/**
 * A pipe upgrade module — right-click a Universal Pipe to install (consumed, up to
 * {@link UniversalPipeBlockEntity#MAX_UPGRADES} of each kind per segment):
 * <ul>
 *   <li><b>Speed</b>: multiplies the segment's per-face throughput (energy/gas) and item move rate.</li>
 *   <li><b>Capacity</b>: buffer multiplier (reserved for the fluid/gas tanks + in-transit cap in the
 *       graph slice).</li>
 * </ul>
 * Sneak-right-click the pipe with an empty hand to pop all upgrades back out.
 *
 * <p>Cross-loader port: pure vanilla item; copied verbatim from the standalone mod.</p>
 */
public class PipeUpgradeItem extends Item {

    public enum Kind {
        SPEED,
        CAPACITY
    }

    private final PipeUpgradeItem.Kind kind;

    public PipeUpgradeItem(Properties properties, PipeUpgradeItem.Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public PipeUpgradeItem.Kind kind() {
        return this.kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.isClientSide() && context.getPlayer() instanceof ServerPlayer player
                && level.getBlockEntity(pos) instanceof UniversalPipeBlockEntity pipe) {
            if (pipe.installUpgrade(this.kind)) {
                context.getItemInHand().shrink(1);
                player.sendSystemMessage(Component.translatable("item.nerospace.pipe_upgrade.installed",
                        context.getItemInHand().getHoverName(),
                        pipe.upgradeCount(this.kind), UniversalPipeBlockEntity.MAX_UPGRADES));
            } else {
                player.sendSystemMessage(Component.translatable("item.nerospace.pipe_upgrade.full"));
            }
            return InteractionResult.SUCCESS;
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
