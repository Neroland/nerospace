# Multiple Player-Built Stations — Design (RELEASE_CHECKLIST §1)

Status: **SIGNED OFF (2026-06-06)** — charter + FOUND-node founding with Core anchor; single
`nerospace:station` dimension with `x = 4096·(i+1)` slots (cap 64); all tiers + all players, no
ownership data (POPIA/GDPR-clean); Core only obtainable by founding.

**Slice 1 IMPLEMENTED (2026-06-06)** — verified via the gradle MCP: `runData` + `build` green,
`ecjCheck` 0 errors (baseline warnings only), gametest suite **30/30 green** (new:
`station_registry_roundtrip`, `station_core_break_unregisters`, `rocket_station_selection`).
Rocket UI reworked: compact planet row, station cycler + FOUND row, Launch moved down, fuel slot
beside the gauge. Also fixed in passing: the rocket's physical tank was still sized to Tier 3's
capacity (T4's 24k would have capped at 12k). Open for runClient: the new UI layout, a real
founding flight, Core look, charter icon.

## 1. Concept & founding flow

Players found additional orbital stations with a **Station Charter** item and manage them through a
**Station Core** block:

1. Craft a **Station Charter** (8× Station Wall ring around 1× Station Floor → 1 charter).
2. *(Optional)* Rename the charter in an **anvil** — that text becomes the station's name
   (zero new GUI; unnamed charters auto-name "Station N").
3. Board any rocket on a valid pad. With a charter in your inventory, the trajectory row shows an
   extra **FOUND** node. Select it and launch: the charter is consumed, a fresh station slot is
   allocated, the landing platform is built with a **Station Core** at its centre, the station is
   registered, and you arrive on it.
4. The Core is the station's anchor: it shows the name on inspection, and **breaking it
   unregisters the station** and pops a charter named after it (re-foundable elsewhere; the
   abandoned platform simply remains as scrap in the void).

Why charter-then-launch (the founding trade-off): a "place a Station Core to register" model can't
bootstrap — there is no way to stand in an empty void slot to place anything, and detecting a
"formed station multiblock" in the void is ambiguous and scan-heavy. The charter makes founding an
explicit, paid, single action that happens exactly where founding is physically possible: at the
end of a rocket flight. The Core then gives the station a persistent, breakable, inspectable
anchor (the registerable-block idea), so both models' strengths are kept.

## 2. Where stations live — single dimension, offset slots

**All stations share the existing `nerospace:station` void dimension at well-separated offsets.**
Station slot *i* sits at `(4096 * (i + 1), 64, 0)` — far beyond any render/simulation distance, on
the X axis so coordinates stay debuggable. The origin platform `(0, 64, 0)` remains the shared
public "Orbital Station" exactly as today (no migration; existing worlds unaffected).

Per-station dimensions were rejected: level stems are datagen'd datapack entries (the Glacira
pattern) — they cannot be minted at runtime without dynamic-dimension machinery that 26.1 NeoForge
makes expensive and the gametest harness can't see at all. Offsets in one void dimension cost
nothing, persist trivially, and the void guarantees no terrain collisions between slots.

Cap: 64 stations (internalised constant — a full X-row of 262k blocks; lift later if anyone hits
it).

## 3. Persistence — `StationRegistry` (server-global SavedData)

A single `StationRegistry extends SavedData` stored on the **overworld** (always loaded), using
the same `SavedDataType` + codec pattern as `OxygenFieldManager` (the known 26.1 gotcha is already
solved there). One record per station:

```java
record StationEntry(int slot, String name, BlockPos center) {}
```

- `nextSlot` counter never reuses slots (unregistering frees the name, not the coordinates — no
  risk of founding a new station inside someone's abandoned hull).
- **Privacy (POPIA/GDPR): no player names, no UUIDs, no founder field.** Stations are
  server-global and usable by everyone, so ownership data isn't needed; nothing personal is ever
  written to disk. If a future permissions feature wants a founder, that lands behind its own
  design (UUID + documented basis), not here.
- Multiplayer semantics: one global list, visible to every player in every rocket. Griefing an
  unprotected station is the same risk as any shared base — per-station permissions are
  deliberately deferred.

## 4. Rocket UI integration

- **Reachability: every tier.** Player stations are orbital infrastructure like the public
  Station, which Tier 1 already reaches — gating them by tier would punish exactly the players
  building return-trip networks. (The per-tier planet gating is untouched.)
- The trajectory row appends one node per registered station after the tier's planet list, plus
  the FOUND node when the rider carries a charter. `SpaceButton` row-wraps already (Star Guide
  L-route work), so long lists render; station names are shortened like planet names.
- **Sync — menu-open snapshot:** the station list (slot + name pairs) is written into the menu's
  open buffer (`RegistryFriendlyByteBuf`, where the rocket entity id already travels). Stations
  change rarely, so a snapshot per menu-open is correct enough; reopening refreshes. No new
  payload type, no tick traffic.
- **Selection — by slot id, not list position:** new synced `DATA_STATION` on the rocket
  (−1 = none/planet destination, −2 = FOUND, ≥0 = station slot). Buttons send
  `SELECT_STATION_BASE + slot` through `clickMenuButton`, so a stale client snapshot can never
  launch at the wrong station (the server resolves the slot against its own registry; a vanished
  slot fails the launch with a message and resets to the tier default).
- `Destinations.name()` covers planets; station nodes use the registry name directly.

## 5. Travel & arrival

`completeLaunch()` gains a station branch beside the existing origin-station branch: resolve the
slot → teleport to `center + (0.5, 1, 0.5)`, rebuilding the 7×7 platform via the existing
`buildStationPlatform` if it's missing (mirrors the origin behaviour — never restacked). The
founding branch allocates the slot, builds platform + Core, registers, consumes one charter from
the rider, then arrives the same way. Fuel cost: the tier's normal `fuelPerLaunch` — a station
trip is a station trip.

## 6. New content

- **Station Charter** (item): 8× Station Wall around 1× Station Floor. Anvil-renameable; the
  custom name founds the station's name.
- **Station Core** (block + block entity): placed only by founding (no crafting recipe — the
  charter IS the recipe; flag if you'd rather it craftable). Stores its slot id; `onRemove`
  unregisters and drops a charter named after the station. Comparator output 15 when registered
  (automation hook, mirrors the Star Guide pedestal pattern). Texture: station-family steel panel
  with a cyan beacon core via the panel generator family.
- Lang/tags/models via datagen; textures via the repo generators; bbmodel entries appended.

## 7. Star Guide + advancements

- `rocketry` chapter gains one step: `station_charter` ("Homestead in Orbit") — completed by a
  **custom `founded_station` PlayerTrigger** fired in the founding branch (the
  `terraformed_ground` ModCriteria pattern), parented on `nerospace:station`. Tree 27 → 28 nodes.
- Guide text teaches the whole flow: craft charter → rename in anvil → FOUND node → break Core to
  unregister.

## 8. Slice 1 scope

1. `StationRegistry` SavedData + slot allocation + codec.
2. Station Charter item + recipe; Station Core block/BE (+ unregister-on-break, comparator).
3. Rocket: `DATA_STATION` synced value, menu snapshot payload, FOUND/station trajectory nodes,
   `clickMenuButton` routing, launch resolution + founding + arrival.
4. `founded_station` criterion + advancement + Star Guide step + lang.
5. Textures (charter item, core block) + bbmodels via generators.
6. Gametests: registry register/unregister + slot-allocation uniqueness, codec round-trip
   (the `test_instance_sync_roundtrip` pattern), Core-break unregisters + drops the named
   charter, charter-name → station-name propagation, destination resolution for a missing slot
   (graceful failure). Travel itself can't run in the harness (no datapack dimensions — assert
   registry/parity state, per the Glacira lesson).
7. Verify via gradle MCP (`runData`/`build`/`ecjCheck`/gametests); tick the §1 boxes that fully
   pass; rocket-UI look + an actual founding flight stay open for runClient.

Deferred (explicitly): per-station permissions/ownership (and any personal data), a naming/rename
GUI (anvil rename covers 1.0), station icons/colours in the UI, inter-station fast travel,
station-specific structures beyond the 7×7 + Core.

## 9. Sign-off questions

1. Founding flow: charter (anvil-renameable) consumed at launch via a FOUND trajectory node,
   Core anchors/unregisters — OK?
2. Placement: single `nerospace:station` dimension, slots at `x = 4096·(i+1)`, cap 64, origin
   platform stays the shared public station — OK?
3. Reachability: player stations visible to ALL tiers (and all players, no ownership data) — OK?
4. Station Core obtainable only by founding (not craftable) — OK, or should it also have a recipe?
