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
 *   <li><b>Speed</b>: multiplies the segment's per-face throughput (energy/fluid/gas) and how fast
 *       items travel through it.</li>
 *   <li><b>Capacity</b>: multiplies the segment's fluid/gas buffers and how many item stacks may be
 *       in transit at once.</li>
 * </ul>
 * Sneak-right-click the pipe with an empty hand to pop all upgrades back out.
 */
public class PipeUpgradeItem extends Item {

    public enum Kind {
        SPEED,
        CAPACITY
    }

    private final Kind kind;

    public PipeUpgradeItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
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
                        getName(context.getItemInHand()),
                        pipe.upgradeCount(this.kind), UniversalPipeBlockEntity.MAX_UPGRADES));
            } else {
                player.sendSystemMessage(Component.translatable("item.nerospace.pipe_upgrade.full"));
            }
            return InteractionResult.SUCCESS;
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
