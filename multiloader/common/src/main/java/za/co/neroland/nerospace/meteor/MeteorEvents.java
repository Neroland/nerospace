package za.co.neroland.nerospace.meteor;

import java.util.Set;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.registry.ModDimensions;

/**
 * Cross-loader server-side driver for natural meteor events (meteor-events design §3/§6). Each loader
 * calls {@link #tick(MinecraftServer)} once per server tick from its own hook (NeoForge
 * {@code ServerTickEvent.Post}, Fabric {@code ServerTickEvents.END_SERVER_TICK}); this ticks the
 * per-level {@link MeteorEventManager} on the eligible surface dimensions. Cheap when idle — the
 * manager short-circuits with no players online.
 *
 * <p>Cross-loader port note: the tracker push (nearest-site sync to Meteor Tracker holders) is a
 * deferred follow-up — it needs the Meteor Tracker item + a clientbound sync payload + a client HUD.
 * This batch is the server-side scheduler only; the {@link MeteorEventManager#nearestSite} hook is
 * already in place for it.</p>
 */
public final class MeteorEvents {

    /** Surface worlds meteors fall on (the void station is excluded — nothing to crater). */
    public static final Set<ResourceKey<Level>> METEOR_DIMENSIONS = Set.of(
            Level.OVERWORLD,
            ModDimensions.GREENXERTZ_LEVEL,
            ModDimensions.CINDARA_LEVEL,
            ModDimensions.GLACIRA_LEVEL);

    private MeteorEvents() {
    }

    /** Ticks the meteor scheduler on every eligible loaded dimension. */
    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            if (METEOR_DIMENSIONS.contains(level.dimension())) {
                MeteorEventManager.get(level).tick(level);
            }
        }
    }
}
