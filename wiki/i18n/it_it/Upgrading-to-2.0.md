# Upgrading to 2.0

> **Alpha:** 2.0 is under active testing. Back up your save before upgrading; planet performance and some sky effects are still being investigated.

Nerospace 2.0 adds three worlds ([Luna](Luna), [Mars](Mars) and [Nyxara](Nyxara)), gives every planet its own
blocks, ores, sky, clock and weather, and turns orbital stations into habitats with classes, members and modules.
The major version reflects the scale of the content changes. **1.x world compatibility is intended, but needs further
runtime verification.** This page describes what is intended to carry over and what to do before updating.

## Before you update

1. **Back up your world.** Copy the whole save folder (single-player: `saves/<world>`; server: the
   world directory). Nerospace never rewrites old terrain or deletes anything, but a backup is the
   only way back to 1.x (see _Going back_ below).
2. Update [Neroland Core](Neroland-Core) to **1.17.0 or newer**. Nerospace 2.0 will not start on an older Core.
3. Update the server **and every client** together. 2.0 adds network messages and Rocket screen data that a 1.x
   client does not understand, so client and server must run the same Nerospace version.
4. If you run other Neroland mods that talk to Nerospace (NeroColonies, NeroLogistics, NeroSecurity,
   NeroEvents, NeroAgriculture, Nerotech), update them too. Older versions keep working (the parts
   of Nerospace they use are unchanged), but they will not know about the new planets until updated.

## What carries over unchanged

- **Stations.** Every station keeps its slot, name, owner and position. It becomes a _Waystation_
  (the default class) with no members and no modules; its original room and pad stay exactly as they
  are and are never rebuilt. Open the Station Core to reach the new Station Console and build onward
  from there. Unowned stations stay manageable by anyone, as before.
  Your existing station room gets breathable air the first time it loads in 2.0, unless something already supplies it.
  A Waystation gets one extra cargo route, so every old station can hold one route more than before.
- **Landing pads** keep their names and positions. Rockets now land on the pad's centre, facing its front; a pad
  whose name tag was used on a corner block still works.
- **Rockets**, including one in flight when you saved, keep their tier, fuel, oxygen, destination
  and return pad. Rocket tiers keep every destination they had: Tier 2 still reaches Greenxertz, Tier 3
  Cindara, Tier 4 Glacira. New destinations are added (Tier 1: Luna; Tier 2: Mars; Tier 4: Nyxara), and each
  tier also reaches every destination of the tiers below it, so Tiers 2–4 fly to Luna and Tiers 3–4 to Mars;
  the pre-selected destination for each tier is the same as before. A rocket saved in 1.x on its way to found a new
  station is honoured once without a charter.
- **Terraformers** continue from the stage they reached. Terraformed ground stays terraformed and keeps its old
  biome; only ground a Terraformer reaches from now on takes the new per-world biomes.
- **Oxygen fields, planet visits, cargo routes and flights, meteor sites, Star Guide progress**: all
  kept. Completed Star Guide chapters stay completed; six new _Worlds_ chapters appear after them.
- **Alien villagers** keep their species and their reputation with you; Village Cores keep their owner. Hamlets,
  Ruins and Mega-Cities on Greenxertz keep generating where they always did.
- **Config.** `config/nerospace.properties` loads as is. All 24 existing keys keep their names and defaults; new
  settings are added with their defaults the next time the file is written.
- **Recipes, advancements, item, block and dimension ids.** Nothing was renamed.

## What looks different

### The seam

Cindara, Glacira and Greenxertz generate new terrain with their own blocks and biomes **only in chunks
you have not visited yet**. Chunks that already exist are left exactly as they were: old
ground, old ores, your buildings, terraformed areas. Where an old chunk meets a new one there is a
**visible seam**, a straight edge between the old stone and the new surface. This is deliberate: a
migration that rewrote old chunks would risk the very builds it was trying to preserve. If you want a
planet to be fully "new", explore outward or start a new world; the old area is not going to change.

Glacira and Greenxertz keep their 1.x terrain shape, so the land lines up across the seam. New Cindara land has its
own shape, so its hills do not continue smoothly from old Cindara chunks.

One small exception at the seam itself: a few ore blocks may appear at the very edge of old chunks next to
new land, where an ore vein that starts in the new chunk reaches one to three blocks across. Ores only ever
replace natural stone, never anything you built.

### Greenxertz keeps its day and night

Greenxertz has always had a real 20-minute day with sleeping, clouds and night raids on village cores.
**That does not change.** It gets its own twin-sun sky, a clock dial on the HUD and spore-bloom
weather, but beds, `/time set` and nightfall behave exactly as before.

### Clocks on the other planets

Luna, Mars, Cindara and Glacira now have their own clocks: Luna's day is about 28 Overworld days, a Martian sol is
24 600 ticks, Cindara's day is two Overworld days with an eclipse, and Glacira's is three with long twilights.
Nyxara and the orbital station have fixed clocks. These clocks are **visual and mechanical**: they drive the sky, the
sun's position, solar panel output and when weather may start. They do **not** change block or sky light: it is as
bright underfoot at "night" as it was in 1.x, and mobs spawn by light exactly as before. See
[Planet Clock and Weather](Planet-Clock-and-Weather).

### Solar panels on planets

In 1.x a solar panel on any Nero planet produced a flat double rate at all times. In 2.0 it follows the
planet's clock and weather: full output at that planet's noon, less toward its dusk, much less in a
Mars dust storm. Over a full day every planet gives at least as much energy as before (peaks are
higher to make up for the nights), and the orbital station stays at a constant double rate. Bases that
relied on solar at night now need a battery. Server owners who prefer the old constant output can set
`solar.clockDriven=false` in `config/nerospace.properties`.

### Weather

Each planet has one weather event (dust storms on Mars, ash fall on Cindara, diamond dust on
Glacira, spore blooms on Greenxertz, flares on Nyxara; none on Luna or the station). Storm
radiation reaches you only outdoors and outside an oxygen field; spore nausea only without a full suit or a breathable
zone. Server owners can tune or disable it
(`weather.enabled`, `weather.<planet>.enabled`, frequency and duration multipliers); see
[Planet Clock and Weather](Planet-Clock-and-Weather).

### Hazards and suits

Your Thermal and Cryo Suits work as before on Cindara and Glacira. The new worlds add **radiation**: Mars dust storms
and all of Nyxara. The **Rad Lining** is a smithing upgrade for Tier 2, Thermal and Cryo suit pieces that keeps it
out; the piece keeps its id, tier and heat or cold shield. Mars is also cold, but less so than Glacira: any full
Tier 2, Thermal or Cryo suit keeps its cold out, except on the frigid Polar Cap, where only the Cryo Suit does.
Nyxara's Dayside is hot and its Nightside freezing (Cryo Suit). See [Oxygen Suit](Oxygen-Suit).

### Landing

Rockets no longer land at the coordinates you launched from on another world. A rocket picks the pad you chose, else
the pad it launched from, else a pad with the same name, else the nearest pad, else it places a **Landing Pod**, and
always comes down centred on it. A Landing Pod now holds the rocket you flew (with its leftover fuel), ready to fly
again, instead of packing it into the pod as an item. See [Landing Pads](Landing-Pads).

### New Station Console

The Station Charter's screen still founds and renames stations. Managing an existing station (class,
members, modules, docking) now happens at the Station Core's console. Founding still costs one
charter, including when you choose _New Station_ in the rocket: the charter is taken when you arrive, never at
launch. See [Orbital Stations](Orbital-Stations).

### Village core owner names

Village cores no longer store the owner's name, only the owner's account id. Chat shows the owner's name only while
they are online ("another player" otherwise). Existing cores drop the stored name the next time their chunk saves.

### Privacy

`/neroland data eraseme` now also removes you from every station's member list and drops your launch cooldown, and
any player can run `/nerospace privacy export` to see everything Nerospace stores about them. A world saved by 1.x
needs nothing done to it. See the mod's `PRIVACY.md` for the full list.

## Going back

Loading a 2.0 world in Nerospace 1.x is **not supported**. Chunks generated on Luna, Mars or
Nyxara, and new-terrain chunks on the old planets, contain blocks 1.x does not know; Minecraft
replaces unknown blocks with air and logs a warning for each, and the three new dimensions become
unreachable. The same goes for everything Nerospace saves: stations, pads, rockets and the rest are written in a
form 1.x was never made to read. The only safe way back is the backup you took before updating. Take the backup.

## If something is wrong

Open an issue on the mod's repository with your log and, if you can, the version you updated from.
Do not delete Nerospace's files in your world's `data` folders: Nerospace keeps a last-known-good copy of
its saved data next to them (see [Saved-Data Backups](Saved-Data-Backups)) and can usually recover.
