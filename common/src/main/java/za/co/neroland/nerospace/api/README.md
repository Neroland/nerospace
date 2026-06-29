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

## Design notes

- Thin facade: no duplicated state; wraps the single internal `StationRegistry` and the gravity/oxygen
  managers.
- All returned collections and records are immutable; no mutable internal collection or block entity leaks.
- Lives in the shared `common` module, so it compiles into every loader cell (Fabric / Forge / NeoForge).
