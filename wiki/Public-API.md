# Public Integration API

Nerospace exposes one supported integration surface for other mods: the Java package
`za.co.neroland.nerospace.api`. Everything outside that package is internal and may change without
notice.

This page is for **mod developers**. If you are a player, nothing here needs any setup — the API is
dormant until another mod uses it.

## Stability

Within a major version, existing public types, methods, record components and enum constants are never
removed, renamed or given a new meaning. New members may be added in minor versions. Consume the API as
a `compileOnly` dependency and keep the types behind your own compat adapter so your mod still runs when
Nerospace is absent.

Nerospace publishes all six loader cells to GitHub Packages:

```
za.co.neroland.nerospace:nerospace-<loader>-<mc>:<version>
```

## Planets, stations and routes

- `NerospacePlanets` — planet identity (`PlanetId`), the `currentPlanet(Entity)` hook, every planet's
  read-only `PlanetTraits` (default gravity, airless flag, `Hazard`), and exact per-position gravity.
- `NerospaceStations` — station destinations as immutable `StationInfo` records.
- `NerospaceRoutes` — the cargo-rocket route catalogue: `RouteEndpoint`s and directed `CargoRoute`s
  with minimum tier, fuel cost and transit duration.

## Environment queries

`NerospaceEnvironment.at(ServerLevel, BlockPos)` returns an immutable `EnvironmentSnapshot`:

| Field | Meaning |
|---|---|
| `loaded` | `false` when the chunk is not loaded — every other field is then a fail-closed default, not a reading |
| `planet` | the Nerospace planet, or empty on Earth and any non-Nerospace dimension |
| `atmosphere` | `VACUUM`, `PRESSURIZED`, `TERRAFORMING` or `BREATHABLE` |
| `hazard` | the planet's ambient hazard |
| `gravity` | gravity factor, `1.0` = Earth-normal |
| `oxygen` | 0-15, the oxygen field plus any external contributions |
| `terraformStage` | 0-3 |
| `breathable` | whether a player can breathe here without a suit |

Two behaviours worth knowing:

- **It never forces chunk loading.** The unloaded check runs before any chunk-backed lookup, so a probe
  at an arbitrary position cannot make the server generate terrain.
- **Stage 1 already means breathable.** That is when the Terraformer flags the chunk, so `TERRAFORMING`
  describes a region that is under way but has not got there yet.

## Planet visits

`NerospaceVisits.hasVisited(player, planet)` and `visitedPlanets(server, uuid)` answer historically —
including visits made before your mod was installed. `PlanetVisitEvents.onVisit(listener)` fires once,
on the server thread, the first time a player reaches a planet.

The event hands you a live player, because listeners usually need to grant that player something. If
you persist anything keyed to them as a result, **that copy is yours to erase** — Nerospace's erasure
hook reaches Nerospace's own store, not yours. Register with Neroland Core's `PlayerDataErasure` so a
single request still purges everything.

## Contributing oxygen

`NerospaceOxygen.contribute(level, source, center, radius, strength, durationTicks)` registers a
bounded, expiring oxygen source that Nerospace folds into breathability and environment answers.
Limits: radius ≤ 64, strength ≤ 15, duration ≤ one hour. Re-contributing with the same source id
replaces the previous value, so the natural "this machine currently produces N" shape is idempotent.
`remove(level, source)` withdraws it.

Contributions must be refreshed before they expire. That is deliberate: a machine that is destroyed
while its chunk is unloaded cannot leave permanent free air behind.

## Terraforming overlays

`NerospaceTerraforming` applies and rolls back reversible regional overlays. An overlay changes what
Nerospace *reports* about a region without rewriting a single block, which is what makes rollback
instant and lossless. Coverage is **horizontal** — a region reaches from bedrock to build limit like a
column, matching how physical terraforming flags a whole chunk rather than a bubble. Each region records the physical chunk stage underneath it, so removing an
overlay restores that baseline instead of erasing real progress.

**Every mutation is denied until the server installs a claim policy:**

```java
NerospaceTerraforming.setClaimPolicy((actor, level, request) -> yourClaimMod.mayBuild(actor, request.center()));
```

This is fail-closed on purpose — an unauthenticated regional environment override is a griefing tool.

## Privacy

Two API surfaces touch player data, and both are covered by Neroland Core's shared data-erasure hook:

- **Station ownership** is never exposed as a raw UUID — only as the per-player boolean
  `NerospaceStations.ownedBy`.
- **Planet visits** store a UUID and a set of planet ids. No timestamps, no coordinates, no movement
  trail. An erasure request drops the row and rewrites the saved-data backup immediately.

Oxygen contributions and terraforming overlays store **no player at all**. Their source and region ids
are yours to choose, are persisted verbatim, and **must never encode a player UUID or name**.
