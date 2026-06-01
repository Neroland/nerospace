package za.co.neroland.nerospace.world;

import java.net.URI;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import za.co.neroland.nerospace.Nerospace;

/**
 * Greets a player on join with a short "work in progress" notice and a clickable link to the mod's
 * GitHub repository. Server-side only; fires once per login.
 */
@EventBusSubscriber(modid = Nerospace.MODID)
public final class PlayerJoinHandler {

    private static final String REPO_URL = "https://github.com/Neroland/nerospace";

    private PlayerJoinHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        player.sendSystemMessage(Component.empty()
                .append(Component.literal("[Nerospace] ").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.translatable("message.nerospace.welcome.wip").withStyle(ChatFormatting.GRAY)));

        Component link = Component.literal(REPO_URL).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(REPO_URL))));

        player.sendSystemMessage(Component.empty()
                .append(Component.translatable("message.nerospace.welcome.link").withStyle(ChatFormatting.GRAY))
                .append(link));
    }
}
