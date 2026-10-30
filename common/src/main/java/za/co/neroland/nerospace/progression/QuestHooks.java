package za.co.neroland.nerospace.progression;

import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.event.CoreEvents;
import za.co.neroland.nerolandcore.progression.GateEvents.GateChange;
import za.co.neroland.nerolandcore.progression.GateScope;
import za.co.neroland.nerolandcore.progression.ProgressionGates;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.progression.StarGuide.Chapter;
import za.co.neroland.nerospace.progression.StarGuide.Step;
import za.co.neroland.nerospace.world.OxygenManager;

/**
 * Reacts to Nerospace's <em>hook gates</em> — {@code nerospace:hook/reveal_star_guide_page},
 * {@code nerospace:hook/oxygen_top_up} and (2.0) {@code nerospace:hook/worlds_briefing}. A quest mod (or any other mod) that wants to hand a player one of
 * these effects opens the matching player-scope record gate through Neroland Core; Nerospace listens on
 * Core's gate bus, applies the effect once and closes the gate again to re-arm it. Nerospace itself never
 * reads these gates for anything else: they are records that gate nothing, and no quest mod is ever
 * imported or required for this to work.
 *
 * <p>A hook that opens while its player is offline simply stays open; {@link #onJoin} delivers it on the
 * next login. Both effects are idempotent and bounded — a chat hint, or a tank filled to its normal
 * maximum — so a duplicate delivery can never pay out twice.</p>
 *
 * <p>POPIA/GDPR: the gate change carries the player's UUID; it is used only to find the online player and
 * is never logged or sent anywhere.</p>
 */
public final class QuestHooks {

    /** Points the player at their next incomplete Star Guide step (a hint; awards nothing). */
    public static final Identifier REVEAL_STAR_GUIDE_PAGE =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "hook/reveal_star_guide_page");
    /** Refills the player's personal / suit oxygen to its normal maximum once. */
    public static final Identifier OXYGEN_TOP_UP =
            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "hook/oxygen_top_up");

    /** The running server, captured on the first player join (a hook can only be delivered to a joined player). */
    @Nullable
    private static volatile MinecraftServer server;

    private QuestHooks() {
    }

    /** Registers the gate listener. Called once from common init. */
    public static void init() {
        CoreEvents.onProgression(QuestHooks::onGateChange);
    }

    /**
     * Player join: remembers the server for the listener and delivers any hook that opened while this
     * player was offline.
     */
    public static void onJoin(ServerPlayer player) {
        server = player.level().getServer();
        deliverIfOpen(player, REVEAL_STAR_GUIDE_PAGE);
        deliverIfOpen(player, OXYGEN_TOP_UP);
        deliverIfOpen(player, WorldsProgress.WORLDS_BRIEFING);
    }

    private static void onGateChange(GateChange change) {
        if (!change.open() || change.scope() != GateScope.PLAYER) {
            return;
        }
        Identifier gate = change.gate();
        if (!REVEAL_STAR_GUIDE_PAGE.equals(gate) && !OXYGEN_TOP_UP.equals(gate)
                && !WorldsProgress.WORLDS_BRIEFING.equals(gate)) {
            return;
        }
        MinecraftServer running = server;
        if (running == null) {
            return;
        }
        UUID id;
        try {
            id = UUID.fromString(change.target());
        } catch (IllegalArgumentException e) {
            return;
        }
        ServerPlayer player = running.getPlayerList().getPlayer(id);
        if (player == null) {
            return; // offline: the gate stays open and onJoin delivers it
        }
        apply(player, gate);
        ProgressionGates.close(running, id, gate); // re-arm; the resulting open=false change is ignored above
    }

    private static void deliverIfOpen(ServerPlayer player, Identifier gate) {
        if (ProgressionGates.isOpen(player, gate)) {
            apply(player, gate);
            ProgressionGates.close(player, gate);
        }
    }

    private static void apply(ServerPlayer player, Identifier gate) {
        if (REVEAL_STAR_GUIDE_PAGE.equals(gate)) {
            revealNextStep(player);
        } else if (OXYGEN_TOP_UP.equals(gate)) {
            topUpOxygen(player);
        } else if (WorldsProgress.WORLDS_BRIEFING.equals(gate)) {
            WorldsProgress.briefing(player);
        }
    }

    /**
     * Tells the player which Star Guide step comes next (chapter + step title and its guide text). Pure
     * hint: no advancement is awarded, so progression is never skipped.
     */
    private static void revealNextStep(ServerPlayer player) {
        for (Chapter chapter : StarGuide.CHAPTERS) {
            for (Step step : chapter.steps()) {
                if (step.hidden() || StarGuideProgress.isComplete(player, step)) {
                    continue; // a hidden step is never hinted
                }
                player.sendSystemMessage(Component.empty()
                        .append(Component.literal("[Nerospace] ").withStyle(ChatFormatting.LIGHT_PURPLE))
                        .append(Component.translatable("message.nerospace.hook.star_guide_hint",
                                Component.translatable(chapter.titleKey()).withStyle(ChatFormatting.AQUA),
                                Component.translatable(step.titleKey()).withStyle(ChatFormatting.AQUA))
                                .withStyle(ChatFormatting.GRAY)));
                player.sendSystemMessage(Component.translatable(step.textKey()).withStyle(ChatFormatting.GRAY));
                return;
            }
        }
        player.sendSystemMessage(Component.empty()
                .append(Component.literal("[Nerospace] ").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.translatable("message.nerospace.hook.star_guide_complete")
                        .withStyle(ChatFormatting.GRAY)));
    }

    /** Fills the personal / suit tank to its normal maximum (never lowers an arrival overfill). */
    private static void topUpOxygen(ServerPlayer player) {
        OxygenManager.topUp(player);
        player.sendSystemMessage(Component.empty()
                .append(Component.literal("[Nerospace] ").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.translatable("message.nerospace.hook.oxygen_top_up")
                        .withStyle(ChatFormatting.GRAY)));
    }
}
