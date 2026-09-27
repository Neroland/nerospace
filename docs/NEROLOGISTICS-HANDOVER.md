# Handover: replacing NeroLogistics' stub `RouteProvider` with Nerospace's route API

For the NeroLogistics maintainer. Nerospace **1.3.0** ships `za.co.neroland.nerospace.api.route`, a
semver-stable, interface-only surface built to the shapes `ship/RouteProvider`, `ship/ShipmentManager`
and `compat/NerospaceRouteProvider` already have. The dimension-level `api.NerospaceRoutes` facade you
bind today is unchanged; this is additive.

## Dependency

Move from reflection to a compile-time dependency (keep it `compileOnly` / optional at runtime — the
loader manifests stay `optional`):

```groovy
// per Stonecutter node, <loader> ∈ {neoforge, forge, fabric}, <mc> ∈ {26.1.2, 26.2, 26.3}
compileOnly "za.co.neroland.nerospace:nerospace-<loader>-<mc>:1.3.0"
```

Resolved from the same maven Core comes from (GitHub Packages once the tag publishes; `mavenLocal()`
after `./gradlew publishToMavenLocal` in `../nerospace` meanwhile). Keep the `isModLoaded("nerospace")`
guard in `NerospaceCompat` before touching any `api.route` class.

## What replaces what

| NeroLogistics seam | Nerospace call |
|---|---|
| `RouteProvider.destinations(server)` — "destinations for this origin" | `RouteApi.instance().padsVisibleTo(server, ownerUuid)` filtered by `id != originPadId`. Endpoints are **pads and stations**, not dimensions: a `PadHandle` has `id()`, `name()`, `dimension()`, `position()`, `isStation()`. Build your `RouteDestination` from it, or carry the pad id directly. |
| `RouteProvider.isAvailable(...)` | `RouteApi.pad(server, id).isPresent()`; liveness of the destination chunk is Nerospace's problem (arrival is deferred, never dropped, when it is unloaded). |
| `RouteProvider.transitTicks(...)` / `fuelPerLaunch(...)` | `RouteApi.quote(server, originPadId, destinationPadId, manifest, returnEmpty)` → `RouteQuote.travelTicks()` and `.fuelMb()` (rocket fuel in mB, already scaled by Nerospace's `fuelCostMultiplier`). Query per shipment. |
| `ShipmentManager.ship(...)` — "ship this manifest" | `RouteApi.requestFlight(server, ownerUuid, new FlightRequest(originPadId, destinationPadId, items, returnEmpty))`. On `accepted()` the items are aboard: remove them from your port. On denial, `denial()` tells you why (`NO_ROCKET`, `NOT_ENOUGH_FUEL`, `PAD_TOO_SMALL`, `NOT_PERMITTED`, …) — surface it on the dashboard. |
| `ShipmentManager` departure / arrival callbacks | `RouteEvents.subscribe(new RouteEvents.Listener() { onFlightDeparted / onFlightArrived / onFlightHeld / onFlightDropped })`. Handles are snapshots; `FlightHandle.id()` is safe to log. |
| `ShipmentState` (your own SavedData of in-flight manifests) | No longer needed for rocket cargo — Nerospace persists the flight (and restores it across restarts). Keep it for train hauls. |

## Model differences to design around

- **The origin needs a docked, fuelled Cargo Rocket on a Cargo Pad.** Nerospace never conjures a rocket:
  `requestFlight` draws fuel from the rocket standing on the origin pad and requires a 3×3 pad under it.
  Your `RocketCargoPortBlock` therefore either *is* adjacent to / feeds a Cargo Pad, or you point players
  at building one. The pad's own hold is untouched by an API flight — you supply the manifest.
- **Ownership.** Every call takes the acting player's UUID; `padsVisibleTo` returns only that player's
  pads, pads shared with them, public pads and stations they manage. A port with no owner (NeroLogistics
  records none) has nobody to ship *as* — pass the UUID of whoever configured the port, or route through
  a public pad. Never cache another player's pad positions.
- **Holding, not dropping.** A flight whose destination is missing/occupied/full enters `HOLDING`, retries
  on `cargoHoldRetrySeconds`, and after `cargoHoldTimeoutMinutes` becomes a Cargo Crate item at the
  destination (`onFlightDropped`). `RouteApi.cancel` only works while holding and also crates.
- **Stations** are endpoints with negative ids (`-(slot + 1)`); delivery lands on a Cargo Pad within six
  blocks of the station's landing pad.

## Minimal binding

```java
RouteApi api = RouteApi.instance();
List<PadHandle> targets = api.padsVisibleTo(server, owner);
RouteQuote quote = api.quote(server, origin, target.id(), items, false).orElseThrow();
FlightRequestResult r = api.requestFlight(server, owner, new FlightRequest(origin, target.id(), items, false));
if (r.accepted()) { port.remove(items); LogisticsMetrics.recordShipmentLaunched(...); }
RouteEvents.subscribe(new RouteEvents.Listener() {
    @Override public void onFlightArrived(FlightHandle f) { LogisticsMetrics.recordShipmentDelivered(...); }
    @Override public void onFlightDropped(FlightHandle f) { /* dashboard warning: crated at destination */ }
});
```

The cost model itself is in `docs/CARGO-ROCKETS.md` §4 if you want to pre-compute quotes in the
dashboard without a server call.
