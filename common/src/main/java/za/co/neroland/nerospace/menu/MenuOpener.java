package za.co.neroland.nerospace.menu;

import java.util.OptionalInt;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.telemetry.NerospaceTelemetry;

/**
 * The single door every Nerospace GUI goes through — {@code openMenu}, but it cannot take the server
 * thread with it when the host platform misbehaves.
 *
 * <p>Vanilla {@code Player#openMenu} is a plain call, so anything that throws underneath it unwinds
 * straight out through the packet handler and kills the tick loop. On a stock Fabric/NeoForge/Forge
 * server nothing does. On a Bukkit/Paper hybrid it can: opening a menu there fires
 * {@code InventoryOpenEvent}, and any listening plugin that walks the inventory drags the server's
 * item-meta bridge across modded items it was never written for (see Sentry {@code MC-NEROSPACE-E} —
 * a {@code NullPointerException} raised inside {@code CraftMetaBlockState} while a plugin read NBT
 * during our open). Hybrids are not a supported platform and we cannot fix their bridge, but a GUI
 * that refuses to open is a far better outcome than a downed server.</p>
 *
 * <p>So: catch, log once, put the player back in a clean state, tell them why, and carry on. Only
 * {@link RuntimeException} and {@link LinkageError} are caught — the failure modes a foreign platform
 * actually produces. Genuine {@code Error}s (out of memory, stack overflow) still propagate.</p>
 *
 * <p><b>POPIA/GDPR:</b> the log line and the telemetry report identify the menu by translation key or
 * class name, never the player and never a rendered title (which can contain player-authored text —
 * see {@link #describe}). No name, UUID, IP, or position is recorded on this path.</p>
 */
public final class MenuOpener {

    private MenuOpener() {
    }

    /**
     * Opens {@code provider} for {@code player}.
     *
     * @return the container id as vanilla would return it, or {@link OptionalInt#empty()} if the open
     *         was refused by the platform (or by vanilla itself, which also returns empty).
     */
    public static OptionalInt open(Player player, MenuProvider provider) {
        try {
            return player.openMenu(provider);
        } catch (RuntimeException | LinkageError e) {
            recover(player, provider, e);
            return OptionalInt.empty();
        }
    }

    /**
     * {@link #open} for the common interaction case, translated into the value a {@code useWithoutItem} /
     * {@code useOn} / {@code interact} override should return.
     *
     * <p>{@link InteractionResult#SUCCESS} when the menu opened. When it did not, the interaction is
     * still spent — {@link InteractionResult#CONSUME}, not {@code FAIL}. {@code FAIL} does not consume
     * the action, so the interaction would fall through to {@code ItemStack#useOn} and a refused GUI
     * would end with the player placing a block against the machine they were trying to open.</p>
     *
     * <p>Note that server-side {@code CONSUME} and {@code SUCCESS} behave identically at these call
     * sites — both are {@code Success}, and the arm swing is driven by the client's own predicted
     * result, which is always {@code SUCCESS} here. The distinction is recorded intent, not a
     * behaviour change: the refusal path returns the value that says "spent, nothing happened" so the
     * next person to read it is not told the menu opened.</p>
     */
    public static InteractionResult openOrConsume(Player player, MenuProvider provider) {
        return open(player, provider).isPresent() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    private static void recover(Player player, MenuProvider provider, Throwable cause) {
        String menu = describe(provider);
        NerospaceCommon.LOGGER.warn(
                "[Nerospace] The server platform refused to open menu '{}' — the interaction was "
                        + "cancelled instead of being allowed to crash the server thread. This is "
                        + "typically a Bukkit/Paper hybrid server, or a plugin reacting to the "
                        + "inventory-open event, choking on a modded item. Hybrid servers are not a "
                        + "supported platform for Nerospace.",
                menu, cause);
        // Best-effort cleanup — the platform is already misbehaving, so nothing here may throw again.
        // Only ServerPlayer exposes closeContainer() publicly across all three loaders; on the client
        // side there is nothing to unwind anyway.
        try {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.closeContainer();
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Nothing further to do; the client will resync on its own.
        }
        try {
            player.sendSystemMessage(Component.translatable("gui.nerospace.menu_open_failed"));
        } catch (RuntimeException | LinkageError ignored) {
            // The player simply does not get the notice.
        }
        NerospaceTelemetry.captureHandledException(cause, "menu_open", menu);
    }

    /**
     * A label for the menu that is safe to log and to send to Sentry: the translation key we authored,
     * or failing that the provider class. Never the <em>rendered</em> title.
     *
     * <p>Rendering the title would be the obvious thing to do and is the reason this method is written
     * out rather than being {@code title.getString()}. A display name is not always ours: a renamed
     * Alien Villager carries whatever a player typed on the name tag, and a station-scoped provider can
     * carry a station name. Both are player-authored free text, so under POPIA/GDPR neither belongs in
     * a log line or a telemetry payload. A translation key is a constant from our own lang file and
     * carries no user input.</p>
     */
    private static String describe(MenuProvider provider) {
        try {
            Component title = provider.getDisplayName();
            if (title != null && title.getContents() instanceof TranslatableContents translatable) {
                return translatable.getKey();
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Fall through to the class name.
        }
        return provider.getClass().getName();
    }
}
