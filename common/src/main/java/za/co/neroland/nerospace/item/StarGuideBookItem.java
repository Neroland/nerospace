package za.co.neroland.nerospace.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.progression.StarGuideMenu;

/**
 * The Star Guide Book: the key to the Star Guide pedestal, and a working copy of the guide on its own
 * — used in hand it opens the same live progression tree. The menu is player-progress-backed, so no
 * block is needed; the pedestal remains the in-world anchor with the hologram.
 *
 * <p>Cross-loader port: vanilla {@code SimpleMenuProvider} + {@code openMenu}; identical to the
 * standalone mod.</p>
 */
public class StarGuideBookItem extends Item {

    public StarGuideBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new StarGuideMenu(id, inventory, p),
                    Component.translatable("container.nerospace.star_guide")));
        }
        return InteractionResult.SUCCESS;
    }
}
