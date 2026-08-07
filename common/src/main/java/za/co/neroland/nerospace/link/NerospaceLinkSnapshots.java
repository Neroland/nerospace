package za.co.neroland.nerospace.link;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import za.co.neroland.nerolandcore.link.LinkSnapshotProvider;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.api.NerospacePlanets;
import za.co.neroland.nerospace.api.PlanetId;
import za.co.neroland.nerospace.api.PlanetTraits;
import za.co.neroland.nerospace.platform.Services;
import za.co.neroland.nerospace.progression.StarGuide;
import za.co.neroland.nerospace.progression.StarGuideProgress;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.rocket.LaunchPadMultiblock;
import za.co.neroland.nerospace.rocket.PadRegistry;
import za.co.neroland.nerospace.rocket.RocketEntity;
import za.co.neroland.nerospace.rocket.RocketTier;
import za.co.neroland.nerospace.rocket.StationRegistry;
import za.co.neroland.nerospace.world.OxygenManager;

/**
 * Nerospace's read side of the NeroLink SPI. Every section is player-scoped by construction — see the
 * visibility rule documented on {@link NerospaceLinkModule} and implemented in
 * {@code NerospaceLinkAccess.stationsOf}.
 *
 * <p>Data-shaping conventions (matching the rest of the ecosystem): {@code snake_case} keys, a
 * {@code schema_version} in every root, enums as {@code .name()}, dimensions as their identifier string,
 * counts instead of rosters, and a section that has nothing to say returns an envelope with an empty
 * array rather than an error. Nothing here loads a chunk to answer a question.</p>
 */
public final class NerospaceLinkSnapshots implements LinkSnapshotProvider {

    private static final List<String> SECTIONS = List.of(
            NerospaceLinkModule.SECTION_ROCKETS,
            NerospaceLinkModule.SECTION_STATIONS,
            NerospaceLinkModule.SECTION_PLANETS,
            NerospaceLinkModule.SECTION_LIFE_SUPPORT,
            NerospaceLinkModule.SECTION_STAR_GUIDE);

    @Override
    public String moduleId() {
        return NerospaceLinkModule.MODULE_ID;
    }

    @Override
    public int schemaVersion() {
        return NerospaceLinkModule.SCHEMA_VERSION;
    }

    @Override
    public List<String> sections() {
        return SECTIONS;
    }

    @Override
    public JsonObject snapshot(UUID playerId, String section, Map<String, String> params) {
        if (playerId == null || section == null) {
            return new JsonObject();
        }
        MinecraftServer server = NerospaceLinkAccess.server();
        if (server == null || !NerospaceLinkAccess.enabled()) {
            return new JsonObject();
        }
        try {
            return switch (section) {
                case NerospaceLinkModule.SECTION_ROCKETS -> rockets(server, playerId);
                case NerospaceLinkModule.SECTION_STATIONS -> stations(server, playerId, params);
                case NerospaceLinkModule.SECTION_PLANETS -> planets(server, playerId);
                case NerospaceLinkModule.SECTION_LIFE_SUPPORT -> lifeSupport(server, playerId);
                case NerospaceLinkModule.SECTION_STAR_GUIDE -> starGuide(server, playerId);
                default -> new JsonObject();   // Unknown section: nothing to say.
            };
        } catch (RuntimeException e) {
            // Section name only — never who asked (POPIA/GDPR).
            NerospaceCommon.LOGGER.warn(
                    "[Nerospace] NeroLink snapshot section '{}' failed; returning nothing for it.",
                    section, e);
            return new JsonObject();
        }
    }

    // --- rockets ------------------------------------------------------------

    /**
     * The rocket the player rides, plus rockets and launch pads within {@code SCAN_RADIUS} blocks of them
     * in their own dimension.
     *
     * <p>Rockets and pads have no owner in Nerospace — they are world content. Rather than invent an
     * ownership model or hand out a server-wide roster (which would leak the position of every base and
     * pad on the server), this answers only what the requester could already see by standing where they
     * are. That makes the section useless when the player is offline, which is the honest trade: the
     * envelope says {@code player_online: false} so a client can explain the empty list.</p>
     */
    private static JsonObject rockets(MinecraftServer server, UUID playerId) {
        JsonObject root = envelope(server, playerId);
        JsonArray rocketRows = new JsonArray();
        JsonArray padRows = new JsonArray();
        root.add("rockets", rocketRows);
        root.add("launch_pads", padRows);
        root.addProperty("scan_radius", NerospaceLinkAccess.SCAN_RADIUS);

        ServerPlayer player = NerospaceLinkAccess.online(server, playerId);
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return root;
        }
        root.addProperty("dimension", level.dimension().identifier().toString());

        RocketEntity ridden = player.getVehicle() instanceof RocketEntity vehicle ? vehicle : null;
        for (RocketEntity rocket : level.getEntitiesOfClass(RocketEntity.class,
                player.getBoundingBox().inflate(NerospaceLinkAccess.SCAN_RADIUS))) {
            rocketRows.add(rocketRow(rocket, rocket == ridden));
        }

        BlockPos here = player.blockPosition();
        for (PadRegistry.PadNode pad : PadRegistry.get(server).inDimension(level.dimension())) {
            if (!pad.pos().closerThan(here, NerospaceLinkAccess.SCAN_RADIUS)) {
                continue;
            }
            padRows.add(padRow(level, pad));
        }
        return root;
    }

    private static JsonObject rocketRow(RocketEntity rocket, boolean ridden) {
        JsonObject row = new JsonObject();
        // The entity's own id — world content, not a person.
        row.addProperty("id", rocket.getUUID().toString());
        RocketTier tier = rocket.getTier();
        row.addProperty("tier", tier.name());
        row.addProperty("tier_level", tier.level());
        row.addProperty("fuel", rocket.getFuel());
        row.addProperty("fuel_capacity", tier.fuelCapacity());
        row.addProperty("fuel_percent", rocket.getFuelPercent());
        row.addProperty("fuel_per_launch", rocket.currentFuelCost());
        row.addProperty("oxygen", rocket.getOxygen());
        row.addProperty("oxygen_capacity", tier.oxygenCapacity());
        row.addProperty("oxygen_percent", rocket.getOxygenPercent());
        row.addProperty("power", rocket.getPower());
        row.addProperty("power_capacity", tier.powerCapacity());
        row.addProperty("power_percent", rocket.getPowerPercent());
        row.addProperty("launching", rocket.isLaunching());
        row.addProperty("launch_ready", rocket.isLaunchReady());
        row.addProperty("on_valid_pad", rocket.isOnValidPad());
        row.addProperty("ridden", ridden);
        ResourceKey<Level> destination = rocket.selectedDestination();
        if (destination != null) {
            row.addProperty("destination", destination.identifier().toString());
        }
        row.add("position", position(rocket.blockPosition()));
        return row;
    }

    private static JsonObject padRow(ServerLevel level, PadRegistry.PadNode pad) {
        JsonObject row = new JsonObject();
        row.addProperty("id", pad.id());
        row.addProperty("name", pad.name());
        row.addProperty("dimension", pad.dim());
        row.add("position", position(pad.pos()));
        // Readiness is a block scan, so it is only answerable for a loaded pad. An unloaded pad simply
        // omits the field rather than loading a chunk to satisfy a phone.
        if (level.hasChunkAt(pad.pos())) {
            Set<BlockPos> cluster = LaunchPadMultiblock.connectedPads(level, pad.pos());
            int padTier = LaunchPadMultiblock.padTierContaining(level, cluster, pad.pos());
            row.addProperty("pad_tier", padTier);
            row.addProperty("max_rocket_tier", padTier);
            row.addProperty("loaded", true);
        } else {
            row.addProperty("loaded", false);
        }
        return row;
    }

    // --- stations -----------------------------------------------------------

    /**
     * Only the requester's own orbital stations. Every other station on the server — including unowned
     * legacy entries and entries anonymised by an erasure request — is invisible here, and an unowned
     * slot is indistinguishable from a slot that does not exist.
     */
    private static JsonObject stations(MinecraftServer server, UUID playerId, Map<String, String> params) {
        JsonObject root = envelope(server, playerId);
        JsonArray rows = new JsonArray();
        StationRegistry registry = StationRegistry.get(server);
        for (StationRegistry.StationEntry entry : NerospaceLinkAccess.requestedStations(server, playerId, params)) {
            JsonObject row = new JsonObject();
            row.addProperty("slot", entry.slot());
            row.addProperty("name", entry.name());
            row.addProperty("dimension", ModDimensions.STATION_LEVEL.identifier().toString());
            row.add("center", position(entry.center()));
            // A boolean about the REQUESTER, which is the only ownership question this module answers.
            row.addProperty("is_owner", true);
            rows.add(row);
        }
        root.add("stations", rows);
        // Registry capacity as plain counts — never a roster, never another player's slot or name.
        root.addProperty("registry_used", registry.count());
        root.addProperty("registry_capacity", StationRegistry.MAX_STATIONS);
        root.addProperty("registry_full", registry.isFull());
        return root;
    }

    // --- planets ------------------------------------------------------------

    /**
     * Planet traits are public world data; the per-planet {@code reached} flag is the requester's own
     * advancement state and is therefore present only while they are online (vanilla loads a player's
     * advancements with the player).
     */
    private static JsonObject planets(MinecraftServer server, UUID playerId) {
        JsonObject root = envelope(server, playerId);
        ServerPlayer player = NerospaceLinkAccess.online(server, playerId);
        ServerAdvancementManager advancements = server.getAdvancements();

        JsonArray rows = new JsonArray();
        for (PlanetId planet : NerospacePlanets.all()) {
            PlanetTraits traits = NerospacePlanets.traits(planet);
            JsonObject row = new JsonObject();
            row.addProperty("id", planet.asString());
            row.addProperty("dimension", planet.dimension().identifier().toString());
            row.addProperty("default_gravity", traits.defaultGravity());
            row.addProperty("airless", traits.airless());
            row.addProperty("hazard", traits.hazard().name());
            if (player != null) {
                // The arrival advancement id is the planet id (see StarGuide's new_worlds chapter).
                row.addProperty("reached", hasAdvancement(advancements, player, planet));
            }
            rows.add(row);
        }
        root.add("planets", rows);

        if (player != null) {
            Optional<PlanetId> current = NerospacePlanets.currentPlanet(player);
            root.addProperty("current", current.map(PlanetId::asString).orElse(null));
            root.addProperty("dimension", player.level().dimension().identifier().toString());
            if (player.level() instanceof ServerLevel level) {
                root.addProperty("gravity_here",
                        NerospacePlanets.gravityAt(level, player.blockPosition()));
            }
        }
        return root;
    }

    /** Whether the player holds the arrival advancement for a planet. Unresolvable ids read as "not reached". */
    private static boolean hasAdvancement(ServerAdvancementManager advancements, ServerPlayer player,
            PlanetId planet) {
        AdvancementHolder holder = advancements.get(planet.id());
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    // --- life_support -------------------------------------------------------

    /**
     * The requester's own oxygen tank, suit and hazard shield. Read from a live {@code ServerPlayer}
     * handle, so there is no path to another player's attachment data; offline the section carries only
     * the envelope.
     */
    private static JsonObject lifeSupport(MinecraftServer server, UUID playerId) {
        JsonObject root = envelope(server, playerId);
        ServerPlayer player = NerospaceLinkAccess.online(server, playerId);
        if (player == null) {
            return root;
        }
        int oxygen = Services.PLATFORM.getOxygen(player);
        int max = OxygenManager.maxOxygenFor(player);
        root.addProperty("oxygen", oxygen);
        root.addProperty("oxygen_max", max);
        root.addProperty("oxygen_percent", max <= 0 ? 0 : Math.min(100, oxygen * 100 / max));
        root.addProperty("suit_tier", OxygenManager.suitTier(player).name());
        root.addProperty("hazard_shield", OxygenManager.hazardShield(player).name());

        ResourceKey<Level> dimension = player.level().dimension();
        root.addProperty("dimension", dimension.identifier().toString());
        root.addProperty("airless", OxygenManager.isAirless(dimension));
        root.addProperty("dimension_hazard", OxygenManager.hazardFor(dimension).name());
        root.addProperty("hazard_countered",
                OxygenManager.hazardFor(dimension) == OxygenManager.HazardShield.NONE
                        || OxygenManager.hazardShield(player) == OxygenManager.hazardFor(dimension));
        return root;
    }

    // --- star_guide ---------------------------------------------------------

    /**
     * The requester's own Star Guide progress. Completion lives in vanilla advancements (the guide keeps
     * no store of its own), which are loaded with the player — so this is online-only too.
     */
    private static JsonObject starGuide(MinecraftServer server, UUID playerId) {
        JsonObject root = envelope(server, playerId);
        root.addProperty("chapters_total", StarGuide.CHAPTER_COUNT);
        root.addProperty("steps_total", StarGuide.totalSteps());

        ServerPlayer player = NerospaceLinkAccess.online(server, playerId);
        if (player == null) {
            return root;
        }

        JsonArray rows = new JsonArray();
        int done = 0;
        String nextStep = null;
        String nextChapter = null;
        for (int index = 0; index < StarGuide.CHAPTER_COUNT; index++) {
            StarGuide.Chapter chapter = StarGuide.CHAPTERS.get(index);
            int mask = StarGuideProgress.chapterMask(player, index);
            int total = chapter.steps().size();
            int complete = Integer.bitCount(mask);
            done += complete;

            JsonObject row = new JsonObject();
            row.addProperty("index", index);
            row.addProperty("id", chapter.id());
            row.addProperty("steps_total", total);
            row.addProperty("steps_complete", complete);
            row.addProperty("complete", complete >= total);
            rows.add(row);

            if (nextStep == null) {
                for (int step = 0; step < total; step++) {
                    if ((mask & (1 << step)) == 0) {
                        nextStep = chapter.steps().get(step).id();
                        nextChapter = chapter.id();
                        break;
                    }
                }
            }
        }
        root.add("chapters", rows);
        root.addProperty("steps_complete", done);
        if (nextStep != null) {
            root.addProperty("next_step", nextStep);
            root.addProperty("next_chapter", nextChapter);
        }
        root.addProperty("complete", nextStep == null);
        return root;
    }

    // --- helpers ------------------------------------------------------------

    /** The two fields every Nerospace snapshot root starts with. */
    private static JsonObject envelope(MinecraftServer server, UUID playerId) {
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", NerospaceLinkModule.SCHEMA_VERSION);
        root.addProperty("player_online", NerospaceLinkAccess.isOnline(server, playerId));
        return root;
    }

    private static JsonObject position(BlockPos pos) {
        JsonObject at = new JsonObject();
        at.addProperty("x", pos.getX());
        at.addProperty("y", pos.getY());
        at.addProperty("z", pos.getZ());
        return at;
    }
}
