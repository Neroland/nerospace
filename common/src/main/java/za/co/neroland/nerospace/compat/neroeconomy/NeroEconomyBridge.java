package za.co.neroland.nerospace.compat.neroeconomy;

import java.util.Set;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroeconomy.api.EconomyCaller;
import za.co.neroland.neroeconomy.api.EconomyResult;
import za.co.neroland.neroeconomy.api.SiblingEconomy;

/**
 * The only Nerospace class that imports NeroEconomy (compiled {@code compileOnly}, never bundled). Loaded
 * only by {@link NerospaceEconomy} after {@code isModLoaded("neroeconomy")}.
 */
final class NeroEconomyBridge {

    /** The outcome of a charge: {@code refusal} is null when it went through. */
    record Charge(boolean applied, @Nullable String refusal) {
    }

    private static EconomyCaller caller;

    private NeroEconomyBridge() {
    }

    /** Claims the caller handle; returns false (dormant) when the loaded NeroEconomy is older than API 1. */
    static boolean init() throws ReflectiveOperationException {
        // Read reflectively: the compile-time constant is inlined by javac, so only this sees the loaded jar.
        int api = SiblingEconomy.class.getField("API_VERSION").getInt(null);
        if (api < 1) {
            return false;
        }
        caller = SiblingEconomy.claim("nerospace", Set.of("first_planet_visit", "launch_fee", "docking_fee"));
        return true;
    }

    /** Reserve → player. Returns null on success, otherwise the result's translation key. */
    @Nullable
    static String pay(MinecraftServer server, UUID player, long amount, String reason, @Nullable String key) {
        if (caller == null) {
            return null;
        }
        EconomyResult result = caller.pay(server, player, amount, reason, key);
        return result.ok() ? null : result.translationKey();
    }

    /** Player → Reserve. */
    static Charge charge(MinecraftServer server, UUID player, long amount, String reason, @Nullable String key) {
        if (caller == null) {
            return new Charge(false, null);
        }
        EconomyResult result = caller.charge(server, player, amount, reason, key);
        return new Charge(result == EconomyResult.APPLIED, result.ok() ? null : result.translationKey());
    }
}
