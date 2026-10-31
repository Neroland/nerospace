# Orbital Stations

Player stations in orbit: founded with a [Station Charter](Station-Charter), run from the **Station Console**,
and grown one module at a time.

## Founding

Use a **Station Charter** (name it in the charter screen) or pick *New Station* in a rocket while carrying a
charter. A new station is a 7×7 room with an airlock and a **Docking Ring** where rockets arrive. Stations
founded before 2.0 keep their original room and their 3×3 landing pad exactly as they were.

## The Station Console

Right-click the [Station Core](Station-Core) to open the console. It has four tabs:

| Tab | What it shows |
|---|---|
| Overview | name, class, modules, Solar Wing power, how many sealed modules have air, members, routes |
| Members | the owner and up to 16 members (only the owner, members and operators see the list) |
| Docking | who may dock, where rockets arrive, cards for the pads on other worlds named after the station, routes and inbound cargo |
| Build | one row per module: its cost (missing materials in red), Build or Repair, and an On/Off switch for Solar Wings and the Gravity Deck |

Anyone may open the console; a player who is not the owner or a member sees a read-only *Visitor* view with
the member count only.

## Who can do what

- **Owner** (or anyone, on an unowned station): rename, change class, set docking, add and remove members,
  build modules, abandon the station.
- **Members**: use the station — dock when docking is limited to members, switch module power, send a
  visitor's rocket home, leave. A station whose docking is limited to members is not listed in other players'
  Rocket screens.
- **Visitor rockets**: a rocket that arrived with a rider who was neither the owner nor a member is a visitor.
  Once it has stood riderless on the dock for ten minutes, the owner or a member can send it back to its home
  pad from the Docking tab. Rockets the owner or a member flew in are never sent away. Nothing is sent while a
  player who is not the owner, a member or an operator is anywhere on the station (within 48 blocks of its
  core), so a visitor is never left without a ride; everyone on the station is told when rockets are sent.
- **Unowned stations** (stations from before owners were recorded) can be managed, and removed with
  `/nerospace station remove`, by anyone. Ask an operator to claim one to protect it.
- **Operators**: may manage any station. Each operator action is written to the server log with the station's
  slot number and the action only.
- Members are added only from the players currently online. Nobody's name is stored by the mod.

## Classes

Every station has one class, shown in the console header. Change it from the Overview tab, once per in-game day.

| Class | Perk |
|---|---|
| Waystation | +1 route capacity (every station from before 2.0 starts here) |
| Research | timed progress 25 % faster for members aboard; unlocks the Observatory |
| Industrial | Solar Wings produce 50 % more; hydroponics grow at half speed |
| Trade | +2 route capacity; economy mods may treat it as a market |

Changing class never removes a module: an Observatory on a station that leaves Research simply goes dormant.

## Modules

Built from the Build tab with materials taken from your inventory. Up to eight modules (the core room does not
count). Building a module that already exists only **repairs** its missing blocks, for free. A build never
replaces a block you placed: if anything is in the way, nothing is built and the console tells you where.

| Module | Needs | Cost |
|---|---|---|
| Habitat Ring | — | 64 Hull Plating, 24 Hull Window, 32 Station Floor, 8 Titanium Ingot |
| Docking Ring | — | 24 Hull Plating, 12 Titanium Ingot, 4 Nerosteel Ingot |
| Solar Wing (×2) | — | 8 Truss, 12 Solar Wing |
| Hydroponics Bay | Habitat Ring | 32 Hull Plating, 16 Hull Window, 12 Hydroponics Tray, 8 Station Floor |
| Gravity Deck | Habitat Ring | 24 Hull Plating, 8 Hull Window, 9 Gravity Plate, 4 Gravity Shard |
| Observatory | Research class | 16 Hull Window, 8 Hull Plating, 4 Truss, 8 Titanium Ingot, 1 Voidglass Lens, 2 Void Crystal |

Modules grow west, north, south and up from the core room; the airlock side stays free for docking. Building the
Docking Ring on an older station replaces its 3×3 launch pad.

## Life support, power and gravity

- **Air:** each sealed module (core, Habitat Ring, Hydroponics Bay, Gravity Deck, Observatory) gets its own
  oxygen supply when built. Hull Plating and Hull Window are airtight; break the hull and the air leaks into the
  void. An existing station's room gets breathable air the first time it loads in 2.0, unless something
  already supplies it.
- **Power:** each Solar Wing block makes 200 FE/t (300 on an Industrial station) while it faces open space,
  and pushes it into adjacent machines, pipes and batteries. Wings only work on the station.
- **Gravity:** a Gravity Plate gives normal gravity in the six blocks above it; the Gravity Deck has nine.
  Switching the deck off returns it to station gravity.
- **Hydroponics:** a crop on a Hydroponics Tray grows when a Hull Window is within three blocks above the tray and
  the crop is in breathable air. No water is needed. Trays grow Earth crops; Xertz plants only root in
  Xertz Soil. Crops on a tray grow at the speed of unwatered farmland.

## The Observatory

A glazed cupola on the core room's roof with a **Telescope** at its top, reached by a truss ladder inside the
room. On a Research station, players who may use the station see the next locked world in the rocket's
destination list as a full planet card while they are aboard. Each Void Crystal placed in the column above the
telescope shows one more world, up to three.

## Station blocks

| Block | What it does |
|---|---|
| Hull Plating | Airtight hull, made with titanium from [Luna](Luna) |
| Hull Window | Airtight glazing |
| Truss | Climbable frame; sneak-place to lay a run of up to eight |
| Solar Wing | Twice a Tier 2 solar panel; works only on the station |
| Gravity Plate | Normal gravity in the six blocks above it |
| Hydroponics Tray | Grows crops under a Hull Window inside breathable air, no water needed |
| Telescope | The Observatory's eyepiece (see above) |
| Docking Ring | 3×3 landing plate; see [Landing Pads](Landing-Pads) |

## Sky, clock and weather

The station's clock is fixed in permanent daylight and it has no weather. The homeworld fills the lower sky to the
east, turning slowly, with a blue rim, a day side and a night side sprinkled with city lights; the sun stands high
in a black, star-filled sky with the galaxy and two nebulae behind it, and an operator can show the sunrise through
the homeworld's rim with `/nerospace gallery sky station sunrise` (the full sky is drawn on NeoForge for Minecraft
26.3; elsewhere the station shows its colours with the vanilla sky shapes). Station gravity is about
a tenth of the Overworld's, except above Gravity Plates. See [Planet Clock and Weather](Planet-Clock-and-Weather).

**Beds** never let you sleep on the station ("The station's lights never dim"), but a bed there still sets your
respawn point and never explodes.

**Open water freezes:** the station is cold, so water under the open sky turns to ice unless the block light at the
water is 10 or more, and sugar cane planted by it breaks. Keep a light source within a few blocks of the water, or
farm under the station roof.

## Server settings

| Key | Default | Range | Effect |
|---|---|---|---|
| `stations.maxMembers` | `16` | 0–16 | members an owner may add |
| `stations.maxModules` | `8` | 0–8 | modules a station may have |

## Privacy

A station stores its owner's id and its members' ids, only to decide who may manage and use it. Erasing your
data removes you from every member list and clears your ownership; the station itself stays. See the mod's
privacy notice for details.
