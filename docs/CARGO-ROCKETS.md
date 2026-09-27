# Cargo Rockets & Route API — design note

> Living document for the cargo-rocket feature (Nerospace 1.3.0). Kept current as the feature is
> built. Everything here is server-authoritative; the client only renders the pad GUI and the rocket.

## 1. What is reused, what is new

| Concern | Reused (unchanged) | New |
|---|---|---|
| Pad footprint / tier validation | `rocket.LaunchPadMultiblock` (flood fill, `padTierContaining`) — the Cargo Pad **is a** `RocketLaunchPadBlock`, so it is a cell of an ordinary 3×3 pad | `route.CargoPadBlock`, `route.CargoPadBlockEntity` |
| Rocket visuals + launch ascent | `client.RocketRenderer` / `RocketT2Model` (tier-2 livery, scale 2.0), `RocketEntity` launch particles | `route.CargoRocketEntity extends RocketEntity` (uncrewed; the pad drives it) |
| Fuel | `fluid.FluidTank`, `machine.FuelTankBlockEntity` (an adjacent Fuel Tank already pumps into any `RocketEntity` on an adjacent pad), Universal Pipe fluid transfer, `NerospaceConfig.fuelCostMultiplier` | a fuel-only tank on the pad (pipe-fed) that is pumped into the docked cargo rocket; no new fuel item |
| Item transfer | vanilla `WorldlyContainer` + Core `SideConfig` (ITEM channel, STORAGE preset) — the Universal Pipe and its basic/advanced filters work on every face unchanged | — |
| Planet data | `api.NerospacePlanets.traits(...).defaultGravity()` (the planet registry) | `route.CargoFormulas` (pure, unit-tested) |
| Ownership model | the station rule: owner UUID string, `""` = unowned, erasure anonymises | pad access list (UUID strings) + `public` flag |
| Saved-data guard | Core `data.SavedDataRecovery` (1.13.0) | `route.RouteRegistry` — the 6th Nerospace store |
| Erasure | `data.NerospaceErasure` → Core `PlayerDataErasure` | `RouteRegistry.forgetPlayer` |
| Menus / screens | non-extended menus, `clickMenuButton`, `MenuOpener`, `TexturedContainerScreen`, `SpaceButton` | `CargoPadMenu`, `client.CargoPadScreen`, `network.CargoPadSyncPayload` (destination names) |
| Link module | `link.NerospaceLinkModule` scoping rules | `route` section (owner's pads + flights only) |
| Public API | `api` facade conventions (immutable records, no owner UUIDs) | `api.route.*` |

Not built (see `docs/IDEAS.md`): fluid cargo, space elevators, NeroEvents weather hooks, satellites.

## 2. Glossary

- **Cargo Pad** — block entity; the only endpoint type. Placing one registers it (owner = placer).
- **Station endpoint** — a founded orbital station counts as a pad for routing. It is a *virtual*
  pad with a negative id (`-(slot + 1)`) whose position is the station's Tier-2 landing pad
  (`StationStructure.padCenter`). Delivery into a station lands on a Cargo Pad within 6 blocks of
  that position; if none exists the flight holds, then crates. A station endpoint is visible to
  whoever can manage the station (owner, or anyone for an unowned station).
- **Route** — `(originPadId → destinationPadId)` owned by the origin pad's owner, with a schedule
  (`MANUAL`, `WHEN_FULL`, `EVERY_N_MINUTES`) and a `return_empty` flag.
- **Flight** — one rocket, one manifest. Created at launch, ends `DELIVERED` or `DROPPED`.
- **Crate** — `nerospace:cargo_crate` item holding the manifest (a `CONTAINER` component). Dropped
  at the destination when a held flight times out, so cargo is never deleted.

## 3. Saved data — `nerospace:cargo_routes` (overworld, via Core `SavedDataRecovery`)

```text
RouteRegistry
  pads:    [ { id, name, dim, pos, owner:"uuid|''", public:bool, access:[uuid...] } ]
  routes:  [ { id, origin, destination, owner, schedule:{mode, interval_ticks}, return_empty } ]
  flights: [ { id, route (-1 for API flights), origin, destination, owner, return_leg,
               items:[ItemStack...], departed_at, arrives_at, state, hold_attempts,
               next_retry_at, completed_at, tier } ]
  next_pad_id, next_route_id, next_flight_id
```

- All ticks are **overworld game time** (persists across restarts, like NeroLogistics' shipments).
- `owner` is the only personal data. `access` entries are UUIDs too and are purged by erasure.
- **Retention:** `DELIVERED`/`DROPPED` records are pruned once `completed_at` is older than
  `cargoFlightRetentionDays` in-game days (default 7). Live flights are never pruned.
- **Erasure** (`forgetPlayer(uuid)`): routes owned by the player are removed; pads owned by the
  player are anonymised (`owner = ""`, the pad stays as shared world content — the station rule);
  the player is removed from every access list; in-flight cargo keeps flying with `owner = ""`
  (items are not personal data, the ownership record is). `SavedDataRecovery.backupNow` is called
  straight after so the backup file never outlives the request.
- Pads re-register on block-entity load (`dim,pos` match) so a store recovered "fresh" heals as
  chunks load; a recovered pad is unowned until its placer re-claims it (sneak + empty hand).

## 4. Formulas — `route.CargoFormulas` (unit-tested)

```text
gravity(dim)      = 1.0 for the Overworld / non-Nerospace dims, else planet defaultGravity()
travelTicks       = TIME_BASE(1200) + (sameDim ? 0 : TIME_CROSS_DIM(2400)) + TIME_PER_G(1800) * (gO + gD) / 2
                    clamped to [600, 24000], then × cargoTravelTimeMultiplier
massUnits         = ceil(total item count / 64)
fuelMb(leg)       = round((FUEL_BASE(1000) + FUEL_PER_UNIT(100) × massUnits) × max(0.1, gOrigin))
                    then × fuelCostMultiplier (Core config), min 1
quote.fuelMb      = fuelMb(out) + (return_empty ? fuelMb(back, mass 0, gDestination) : 0)
```

Quotes are exposed through `RouteApi.quote(...)` so NeroLogistics can price a shipment before
committing it and NeroEvents can reason about durations.

## 5. Config (Core `ConfigManager`, all server-authoritative)

| Key | Default | Range | Meaning |
|---|---|---|---|
| `cargoPadSlots` | 18 | 1..27 | usable cargo slots per pad (payload capacity) |
| `cargoFuelCapacity` | 16000 | 1000..64000 | mB the pad's fuel buffer holds |
| `cargoTravelTimeMultiplier` | 1.0 | 0.1..10 | scales travel time |
| `cargoHoldRetrySeconds` | 30 | 5..600 | retry interval while a flight is holding |
| `cargoHoldTimeoutMinutes` | 30 | 1..1440 | holding time before the cargo is crated |
| `cargoFlightRetentionDays` | 7 | 1..90 | in-game days a completed flight record is kept |
| `cargoMaxFlightsPerOwner` | 16 | 1..128 | concurrent live flights per owner |
| `cargoMaxPads` | 256 | 8..4096 | pad registry cap |

## 6. Flight lifecycle

1. **Launch** (`CargoLaunch.launch`): requires a selected route whose origin is this pad, a docked
   `CargoRocketEntity` that is not launching, a pad formation of tier ≥ 2 containing the Cargo Pad
   (the same `padTierContaining` gate crewed rockets use), a non-empty manifest within the slot cap,
   and rocket fuel ≥ quote. Fuel is drained, items move from the pad into the flight record, the
   rocket plays the ascent and is discarded. `FlightDeparted` fires.
2. **In flight** — a server-side timer only; nothing is rendered or loaded.
3. **Arrival** (`RouteRegistry.tick`, every 20 ticks): resolve the destination pad.
   - dimension not loaded / chunk not loaded → `AWAITING_CHUNK` (no timeout, never loads anything);
   - no Cargo Pad at the position, or a rocket already docked there, or the pad refuses every stack
     → `HOLDING`, retried every `cargoHoldRetrySeconds`; `FlightHeld` fires once;
   - after `cargoHoldTimeoutMinutes` of holding → the remaining manifest becomes a Cargo Crate item
     at the destination position, state `DROPPED`, `FlightDropped` fires;
   - otherwise → `UNLOADING`: a cargo rocket materialises on the pad and one stack per 2 ticks moves
     into the pad inventory (so pipes keep up), then `DELIVERED` + `FlightArrived` + arrival effect.
4. **Return leg** — with `return_empty`, arrival at the destination does not leave the rocket
   there: an empty return flight (mass 0, fuel already paid at launch) flies back and the rocket
   materialises on the origin pad. Without it the rocket stays on the destination pad.
5. **Cancel** — only a `HOLDING`/`AWAITING_CHUNK` flight can be cancelled, and cancelling crates the
   cargo at the destination immediately (never deletes it).

## 7. Public API — `za.co.neroland.nerospace.api.route`

Interfaces and records only; obtain the implementation with `RouteApi.instance()`.

- `RouteApi` — `padsVisibleTo(server, uuid)`, `pad(server, id)`, `quote(server, origin, dest,
  manifest, returnEmpty)`, `requestFlight(server, requester, FlightRequest)`, `flight(server, id)`,
  `flightsOwnedBy(server, uuid)`, `routesOwnedBy(server, uuid)`, `cancel(server, requester, id)`.
- `PadHandle`, `RouteHandle`, `FlightHandle` — immutable snapshots; ownership is queryable **only**
  as `ownedBy(UUID)` (never a raw owner UUID, same rule as `StationInfo`).
- `FlightState`, `ScheduleMode`, `RouteQuote`, `FlightRequest`, `FlightRequestResult`.
- `RouteEvents` — `subscribe(Listener)`; `PadRegistered`, `PadUnregistered`, `FlightDeparted`,
  `FlightArrived`, `FlightHeld`, `FlightDropped`.

Shape match with NeroLogistics: its `RouteProvider` needs "destinations for this origin" →
`padsVisibleTo(uuid)` filtered by `id != origin`; "ship this manifest" → `requestFlight`; its
`ShipmentManager` departure/arrival callbacks → `RouteEvents`. The existing dimension-level
`api.NerospaceRoutes` facade is untouched.

## 8. Link module

New section `route` (owner-scoped): the requester's own pads (id, name, dimension, position,
public flag, docked rocket, fuel) and their live flights (id, state, origin/destination pad ids,
arrival tick). Never another player's pad, never a server-wide roster. No new actions.

## 9. Compliance (POPIA / GDPR)

- Player data introduced: pad owner UUID, pad access-list UUIDs, route owner UUID, flight owner UUID
  — all in one store, all erased by `RouteRegistry.forgetPlayer`, backup rewritten immediately.
- Logs and Sentry breadcrumbs carry flight/pad **ids only** (`cargo.depart flight=12`), never a UUID,
  and never a UUID paired with a position. The pad GUI shows the viewer their own pads only; the
  destination list contains only pads the viewer may access.
- Erasure conformance: `common/src/test/.../route/RouteRegistryErasureTest` runs Core's
  `ErasureConformance` against the store (result recorded in §11).
- Telemetry stays opt-out and unchanged.

## 10. Content

- Blocks/items: `cargo_pad` (block + item), `cargo_hull` (item), `cargo_rocket` (item), `cargo_crate`
  (item, not craftable). Recipes sit on the Greenxertz ladder: nerosteel + a Tier-2 rocket for the
  cargo rocket, nerosteel + chests for the hull, a launch pad + hull + nerosium block for the pad.
- Star Guide chapter **Freight**: Cargo Pad → Cargo Rocket → first automated run (code-granted on
  the owner's first `DELIVERED` flight).
- Lang: `en_us` + `en_za` (the en_za file carries the new strings; Minecraft falls back to `en_us`).

## 11. Verification log

- Baseline nine-cell build before any change: **BUILD SUCCESSFUL** (2026-09-27, incremental).
- 2026-09-27, after the feature: **all nine cells BUILD SUCCESSFUL** (`:neoforge/forge/fabric:26.1.2|26.2|26.3:build`),
  `CargoFormulasTest` (5) + `RouteRegistryTest` (7, including Core's `ErasureConformance` against
  `nerospace:cargo_routes`) pass on every test-owning node (neoforge 26.1.2 / 26.2 / 26.3), `ecjCheck`
  0 errors. Compile fixes needed after the first pass: `ItemStack.isSameItemSameComponents`,
  `ItemContainerContents.nonEmptyItemCopyStream()`, no `@Override` on `Block.codec()` for 26.3, and
  the 26.3 `Player.drop(..., Prediction)` Stonecutter block.
- Erasure conformance: PASSED (`RouteRegistryTest.erasure` — probe `nerospace:cargo_routes` reports data
  before, none after; the failing-eraser isolation canary passes; the erasure survives a codec reload).
- Client checklist (`docs/RUNTIME-VERIFICATION.md`): **not yet run** — needs a dev client.
