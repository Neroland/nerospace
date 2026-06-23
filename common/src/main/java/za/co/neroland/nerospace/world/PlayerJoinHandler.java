package za.co.neroland.nerospace.world;

import java.net.URI;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Greets a player on join with a short welcome pointing at the Star Guide and a clickable link to the
 * mod's repository. Cross-loader: the loader entry points invoke {@link #onPlayerJoin} from their join
 * events (NeoForge {@code PlayerEvent.PlayerLoggedInEvent} / Fabric
 * {@code ServerPlayConnectionEvents.JOIN}). The {@link ClickEvent.OpenUrl} record and {@code Style}
 * builder calls are vanilla on both 26.1.2 and 26.2, so no per-loader split is needed.
 *
 * <p>POPIA/GDPR: only sends an outbound chat message to the joining player; logs no player identity or
 * personal data.</p>
 */
public final class PlayerJoinHandler {

    private static final String REPO_URL = "https://github.com/Neroland/nerospace";

    private PlayerJoinHandler() {
    }

    /** Send the one-time welcome to a freshly joined server player. */
    public static void onPlayerJoin(ServerPlayer player) {
        player.sendSystemMessage(Component.empty()
                .append(Component.literal("[Nerospace] ").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.translatable("message.nerospace.welcome.intro").withStyle(ChatFormatting.GRAY)));

        Component link = Component.literal(REPO_URL).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(REPO_URL))));

        player.sendSystemMessage(Component.empty()
                .append(Component.translatable("message.nerospace.welcome.link").withStyle(ChatFormatting.GRAY))
                .append(link));
    }
}
