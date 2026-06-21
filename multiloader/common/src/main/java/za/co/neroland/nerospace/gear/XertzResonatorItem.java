package za.co.neroland.nerospace.gear;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.registry.ModTags;

/**
 * Xertz Resonator (ALIEN_VILLAGERS_DESIGN.md §6.1) — an exclusive Artificer trade. Right-click to ping
 * the surrounding stone: it reports how many ore blocks lie within range, a cross-mod prospecting aid.
 *
 * <p>Cross-loader port: identical to the standalone mod, except the ore test uses the common
 * {@code c:ores} convention tag ({@link ModTags.Blocks#ORES}) instead of the NeoForge
 * {@code Tags.Blocks.ORES} constant — so it still matches any mod's ores on both loaders.</p>
 */
public class XertzResonatorItem extends Item {

    private static final int RADIUS = 8;

    public XertzResonatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            BlockPos center = player.blockPosition();
            int count = 0;
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                    for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                        m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                        if (level.getBlockState(m).is(ModTags.Blocks.ORES)) {
                            count++;
                        }
                    }
                }
            }
            player.sendSystemMessage(Component.literal(
                    "Xertz resonance: " + count + " ore blocks within " + RADIUS + " blocks."));
        }
        return InteractionResult.SUCCESS;
    }
}
