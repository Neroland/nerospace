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
| Docking | where rockets arrive, pads on other worlds named after the station, routes and inbound cargo |
| Build | the seven module tiles, their cost and state |

Anyone may open the console; a player who is not the owner or a member sees a read-only *Visitor* view with
the member count only.

## Who can do what

- **Owner** (or anyone, on an unowned station): rename, change class, set docking, add and remove members,
  build modules, abandon the station.
- **Members**: use the station — dock when docking is limited to members, switch module power, leave.
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
  Xertz Soil.

## The Observatory

A glazed cupola on the core room's roof with a **Telescope** at its top, reached by a truss ladder inside the
room. On a Research station, players who may use the station see the next locked world in the rocket's
destination list as a full planet card while they are aboard. Each Void Crystal placed in the column above the
telescope shows one more world, up to three.

## Privacy

A station stores its owner's id and its members' ids, only to decide who may manage and use it. Erasing your
data removes you from every member list and clears your ownership; the station itself stays. See the mod's
privacy notice for details.
