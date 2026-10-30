package za.co.neroland.nerospace.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.api.PlanetId;
import za.co.neroland.nerospace.progression.PlanetVisitState;
import za.co.neroland.nerospace.rocket.StationRegistry;
import za.co.neroland.nerospace.route.RouteRegistry;
import za.co.neroland.nerospace.village.VillageCoreLedger;

/** {@code /nerospace privacy export}: the requester's own memberships, stations and cores — nothing about others. */
class NerospacePrivacyExportTest {

    @Test
    void exportCoversMembershipsOwnedStationsAndCoresButNoOtherPlayer() {
        UUID me = UUID.randomUUID();
        UUID friend = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        StationRegistry stations = new StationRegistry();
        int mine = stations.found("Mine", me.toString()).slot();
        stations.addMember(mine, friend.toString());
        int theirs = stations.found("Theirs", stranger.toString()).slot();
        stations.addMember(theirs, me.toString());
        stations.addMember(theirs, friend.toString());
        stations.found("Unrelated", stranger.toString());

        PlanetVisitState visits = new PlanetVisitState();
        visits.record(me, new PlanetId(Identifier.fromNamespaceAndPath("nerospace", "luna")));

        RouteRegistry routes = new RouteRegistry();
        int pad = routes.registerPad(ResourceKey.create(Registries.DIMENSION, Identifier.parse("minecraft:overworld")), new BlockPos(1, 64, 1), me, "Dock").id();
        routes.grantAccess(pad, friend);
        int foreignPad = routes.registerPad(ResourceKey.create(Registries.DIMENSION, Identifier.parse("minecraft:overworld")), new BlockPos(9, 64, 9),
                stranger, "Theirs").id();
        routes.grantAccess(foreignPad, me);

        VillageCoreLedger.CoreLoc core = new VillageCoreLedger.CoreLoc("nerospace:greenxertz", new BlockPos(4, 70, 4).asLong());
        JsonObject doc = NerospacePrivacyExport.build(me, visits.export(me), stations.all(), "nerospace:station",
                Set.of(core), routes.allPads(), routes.routesOwnedBy(me), 300, List.of(1, 0), List.of());

        assertEquals(1, doc.getAsJsonArray("planets_visited").size());
        assertEquals(1, doc.getAsJsonArray("stations_owned").size());
        assertEquals(1, doc.getAsJsonArray("stations_owned").get(0).getAsJsonObject().get("member_count").getAsInt());
        assertEquals(1, doc.getAsJsonArray("station_memberships").size());
        assertEquals("Theirs", doc.getAsJsonArray("station_memberships").get(0).getAsJsonObject().get("name").getAsString());
        assertEquals(1, doc.getAsJsonArray("village_cores_owned").size());
        assertEquals("[4,70,4]", doc.getAsJsonArray("village_cores_owned").get(0).getAsJsonObject().get("pos").toString());
        assertEquals(1, doc.getAsJsonArray("cargo_pads_owned").size());
        assertEquals(1, doc.get("cargo_pads_shared_with_you").getAsInt());

        String json = doc.toString();
        assertFalse(json.contains(friend.toString()), "no member ids");
        assertFalse(json.contains(stranger.toString()), "no owner ids");
        assertFalse(json.contains("Unrelated"), "no unrelated stations");
    }
}
