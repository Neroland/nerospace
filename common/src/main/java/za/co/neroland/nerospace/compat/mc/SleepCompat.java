package za.co.neroland.nerospace.compat.mc;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Version seam for planet sleeping. 1.21.11 replaced the {@code GameRules.Key} rules with the registry-backed
 * {@code gamerules.GameRules} (snake-case ids, {@code get(rule)}) and turned {@code Player.BedSleepingProblem} from an
 * enum into a record that carries its own message. Everything else the planet sleep code calls ({@code isSleeping},
 * {@code isSleepingLongEnough}, {@code stopSleepInBed(boolean, boolean)}) is unchanged from 1.21.1 to 26.3.
 */
public final class SleepCompat {

    private SleepCompat() {
    }

    /** The level's {@code players_sleeping_percentage} game rule (vanilla default 100). */
    public static int sleepingPercentage(ServerLevel level) {
        //? if >=1.21.11 {
        return level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.PLAYERS_SLEEPING_PERCENTAGE);
        //?} else {
        /*return level.getGameRules().getInt(net.minecraft.world.level.GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
        *///?}
    }

    /**
     * A bed refusal carrying {@code message}. The bed block shows a problem's message on the action bar; before
     * 1.21.11 the problem is an enum with fixed vanilla text, so the message is sent here and a message-less problem
     * is returned (the bed then shows nothing over it).
     */
    public static Player.BedSleepingProblem refusal(ServerPlayer player, Component message) {
        //? if >=1.21.11 {
        return new Player.BedSleepingProblem(message);
        //?} else {
        /*player.sendOverlayMessage(message);
        return Player.BedSleepingProblem.OTHER_PROBLEM;
        *///?}
    }
}
