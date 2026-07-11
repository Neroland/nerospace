# Nerospace Public API (`za.co.neroland.nerospace.api`)

The **only supported integration surface** for other Neroland mods. Everything else in Nerospace is
internal and may change at any time — code against this package (or Core tags), never against
`registry.*`, `rocket.*`, `world.*`.

## Stability contract

Types here are **semver-stable**. Within a major version: no public type, method, record component, or enum
constant is removed, renamed, or has its meaning changed. Minor versions may add members. Internals behind
the facade may be refactored freely.

## Planets — `NerospacePlanets`

```java
// Identity
PlanetId greenxertz = NerospacePlanets.GREENXERTZ;          // also CINDARA, GLACIRA, STATION
List<PlanetId> all  = NerospacePlanets.all();
Optional<PlanetId> p = NerospacePlanets.byId("nerospace:cindara");
Optional<PlanetId> q = NerospacePlanets.byDimension(level.dimension());

// The Core meteor-grinder hook: which planet is this entity on? (empty on Earth / non-Nerospace dims)
Optional<PlanetId> here = NerospacePlanets.currentPlanet(player);

// Read-only traits
PlanetTraits t = NerospacePlanets.traits(NerospacePlanets.GLACIRA);
double g       = t.defaultGravity();   // flat dimension default (1.0 = Earth-normal), config-scaled
boolean airless = t.airless();
Hazard hazard  = t.hazard();           // NONE / HEAT / COLD

// Exact per-position gravity (biome tags + terraforming applied) — server-side
double exact = NerospacePlanets.gravityAt(serverLevel, pos);
```

`PlanetId` wraps a dimension `Identifier` and maps to/from `ResourceKey<Level>` via `dimension()`.

## Historical visits — `NerospaceVisits`

`NerospaceVisits.hasVisited(player, planet)` queries server-authoritative historical state;
`visitedPlanets(server, uuid)` returns an immutable subject export. `PlanetVisitEvents.onVisit` fires
once on the first observed arrival. Storage contains only UUID plus planet ids—no names, timestamps,
route, or location history—and Core `PlayerDataErasure` removes the UUID row.

## Environment and oxygen

`NerospaceEnvironment.at(level, pos)` returns an immutable `EnvironmentSnapshot`: loaded state, optional
planet, coarse `Atmosphere`, hazard, gravity, oxygen pressure (`0..15`), terraforming stage (`0..3`),
and breathability. Unloaded positions fail closed with vacuum and zero oxygen.

Optional providers add bounded, expiring pressure through
`NerospaceOxygen.contribute(level, sourceId, center, radius, strength, durationTicks)` and remove it with
the same source id. Contributions never force-load chunks and carry no player attribution.

## Reversible terraforming overlays

`NerospaceTerraforming` stores bounded `TerraformRequest`s as immutable `TerraformRegion` snapshots.
Install a `TerraformClaimPolicy` before mutation; the default denies all requests. Apply and rollback
both pass through that policy. Overlays persist but do not destructively rewrite chunks, expose no owner
UUID, and can be removed cleanly.

## Stations — `NerospaceStations`

```java
List<StationInfo> stations = NerospaceStations.all(server);          // founding order, immutable
Optional<StationInfo> s    = NerospaceStations.byId(server, slot);
List<StationInfo> onStation = NerospaceStations.byPlanet(server, NerospacePlanets.STATION);

StationInfo info = stations.get(0);
int id            = info.id();             // stable slot, never reused — a stable routing key
String name       = info.name();
PlanetId planet   = info.planet();         // always NerospacePlanets.STATION
BlockPos pos      = info.position();
int routeCapacity = info.routeCapacity();  // for NeroLogistics route provisioning
```

### Privacy (POPIA/GDPR)

`StationInfo` **does not expose owner UUIDs**. Ownership is a per-player boolean only:

```java
boolean mine = NerospaceStations.ownedBy(server, id, playerUuid);   // false if unowned/unknown/erased
boolean mine2 = NerospaceStations.ownedBy(id, serverPlayer);        // convenience overload
```

The facade reads the live registry, so it automatically honours Neroland Core's `PlayerDataErasure` hook
that Nerospace registers: once a player is purged, their station ownership is anonymised at the source and
`ownedBy` returns `false` for them. No erasure logic runs through this API — it simply reflects the source
of truth.

## Cargo-rocket routes — `NerospaceRoutes`

The lookup entry point for logistics consumers (e.g. NeroLogistics' `RouteProvider` seam). Routes are
directed dimension pairs over the five endpoints (Home + Station + three moons); per-station addressing
inside the orbital void stays with `NerospaceStations`.

```java
// Catalog (static data — order is stable: Home, Orbital Station, Greenxertz, Cindara, Glacira)
List<RouteEndpoint> endpoints = NerospaceRoutes.endpoints();
RouteEndpoint e = endpoints.get(0);
ResourceKey<Level> dim = e.dimension();   // interned key
String label          = e.name();         // "Home", "Orbital Station", ...
Optional<PlanetId> p  = e.planet();       // empty only for Home (Overworld)

// One directed route (empty if either dim isn't an endpoint, or from == to)
Optional<CargoRoute> r = NerospaceRoutes.route(Level.OVERWORLD, NerospacePlanets.GLACIRA.dimension());
CargoRoute route = r.orElseThrow();
int tier    = route.minimumRocketTier();  // 1–4; lowest rocket tier that can fly it
int fuelMb  = route.fuelCostMb();         // per-launch fuel, config-scaled at query time — query per shipment
int transit = route.transitTicks();       // canonical duration: TRANSIT_TICKS_PER_STEP per step of separation

// All routes departing a dimension
List<CargoRoute> out = NerospaceRoutes.routesFrom(Level.OVERWORLD);

// Liveness: both endpoint dimensions loaded on this server
boolean open = NerospaceRoutes.isOpen(server, route);
boolean open2 = NerospaceRoutes.isOpen(server, fromKey, toKey);   // also false when no such route
```

No player data crosses this surface — endpoints, costs and durations only.

## Design notes

- Thin facades wrap the owning state/manager and return snapshots; no internal manager escapes.
- All returned collections and records are immutable; no mutable internal collection or block entity leaks.
- Lives in the shared `common` module, so it compiles into every loader cell (Fabric / Forge / NeoForge).
