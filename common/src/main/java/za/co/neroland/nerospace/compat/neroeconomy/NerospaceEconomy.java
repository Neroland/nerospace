package za.co.neroland.nerospace.compat.neroeconomy;

import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.api.PlanetVisitEvents;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.platform.Services;
import za.co.neroland.nerospace.progression.PlanetVisitState;

/**
 * Optional NeroEconomy integration — the façade the rest of Nerospace calls. It imports nothing from
 * NeroEconomy: {@link NeroEconomyBridge} is the only class that does, and it is touched only after
 * {@code isModLoaded("neroeconomy")} succeeded and the bridge reported the API it needs.
 *
 * <p>Everything here is opt-in per server: the first-visit reward, launch fee and docking fee all default
 * to 0, and 0 means the feature does nothing. Without NeroEconomy (or with one older than API version 1),
 * flights stay free and visits pay nothing.</p>
 *
 * <p><b>Privacy (POPIA/GDPR):</b> a player's UUID is passed to NeroEconomy only for the charge or payout
 * itself. It is never logged and never placed in a request key; keys carry a planet id and a world-wide
 * sequence number, or a flight id.</p>
 */
public final class NerospaceEconomy {

    private static volatile boolean active;

    private NerospaceEconomy() {
    }

    /** Common init: claim the caller handle when NeroEconomy is present. Never throws. */
    public static void init() {
        if (!Services.PLATFORM.isModLoaded("neroeconomy")) {
            return;
        }
        try {
            active = NeroEconomyBridge.init();
        } catch (Throwable t) {
            active = false;
            NerospaceCommon.LOGGER.warn("[Nerospace] NeroEconomy integration unavailable; flights stay free.", t);
            return;
        }
        if (active) {
            PlanetVisitEvents.onVisit(NerospaceEconomy::onFirstVisit);
            NerospaceCommon.LOGGER.info("[Nerospace] NeroEconomy integration enabled (amounts are set in config).");
        }
    }

    /** Whether the NeroEconomy bridge is live. */
    public static boolean active() {
        return active;
    }

    private static void onFirstVisit(PlanetVisitEvents.Visit visit) {
        int reward = NerospaceConfig.economyFirstVisitReward();
        if (!active || reward <= 0) {
            return;
        }
        ServerPlayer player = visit.player();
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        // The sequence was advanced by the record() that fired this event, so it is unique to this visit.
        // Never retried automatically: a missed welcome bonus beats a backlog of payouts.
        String key = "visit/" + visit.planet().asString() + "/" + PlanetVisitState.get(server).sequence();
        String refused = NeroEconomyBridge.pay(server, player.getUUID(), reward, "first_planet_visit", key);
        if (refused == null) {
            player.sendSystemMessage(Component.translatable("message.nerospace.economy.first_visit_reward", reward));
        } else {
            player.sendSystemMessage(Component.translatable(refused));
        }
    }

    /**
     * Charges the fees for one flight, after every other launch check has passed. Docking is charged first
     * (only when {@code toStation}); if the launch fee is then refused, the docking fee just taken is
     * handed back, so a refused launch never costs anything.
     *
     * @param launchKey request key for the launch fee (a flight id), or {@code null} when there is none
     * @param dockKey   request key for the docking fee, or {@code null}
     * @return {@code null} when the flight may launch (nothing due, NeroEconomy absent, or paid); otherwise
     *         the NeroEconomy translation key explaining the refusal
     */
    @Nullable
    public static String chargeFlight(MinecraftServer server, @Nullable UUID payer, boolean toStation,
            @Nullable String launchKey, @Nullable String dockKey) {
        if (!active || server == null) {
            return null;
        }
        int launchFee = Math.max(0, NerospaceConfig.economyLaunchFee());
        int dockFee = toStation ? Math.max(0, NerospaceConfig.economyDockingFee()) : 0;
        if (launchFee == 0 && dockFee == 0) {
            return null;
        }
        if (payer == null) {
            return "economy_result.neroeconomy.invalid";
        }
        boolean dockApplied = false;
        if (dockFee > 0) {
            // With a launch fee also due, the docking charge carries no key: it may be refunded below, and a
            // remembered key would then make the next attempt read as "already paid".
            NeroEconomyBridge.Charge dock = NeroEconomyBridge.charge(server, payer, dockFee, "docking_fee",
                    launchFee > 0 ? null : dockKey);
            if (dock.refusal() != null) {
                return dock.refusal();
            }
            dockApplied = dock.applied();
        }
        if (launchFee > 0) {
            NeroEconomyBridge.Charge launch = NeroEconomyBridge.charge(server, payer, launchFee, "launch_fee",
                    launchKey);
            if (launch.refusal() != null) {
                if (dockApplied) {
                    NeroEconomyBridge.pay(server, payer, dockFee, "docking_fee", null);
                }
                return launch.refusal();
            }
        }
        return null;
    }
}
