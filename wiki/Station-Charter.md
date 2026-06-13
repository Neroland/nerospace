# Station Charter

Found your own orbital station — name included.

## Overview
The Station Charter is how players create **additional stations** beyond the shared
[Orbital Station](Home): carry one aboard a rocket, pick the **FOUND** node in the rocket UI, and
launch. The charter is consumed and a brand-new station platform is built in orbit, anchored by a
**[Station Core](Station-Core)** — and from then on it appears as a destination in every rocket's UI,
for every player and every rocket tier.

## Obtaining
**Craft** (shaped):
```
W W W
W F W
W W W
```
`W` = [Station Wall](Station-Wall) · `F` = [Station Floor](Station-Floor)

## How it works
- **Naming:** rename the charter in an **anvil** before flying — that becomes the station's name.
  An unnamed charter founds "Station N".
- **Founding:** board any rocket with the charter in your inventory; the rocket UI shows the
  **FOUND** node next to the station selector. Select it and launch. The charter is consumed, the
  platform + bound Station Core are placed, and the station registers as a destination.
- **Capacity:** up to **64** founded stations per world. Founding also grants the *founded station*
  advancement and ticks the Star Guide's rocketry chapter.
- **Unregistering:** breaking the station's [Station Core](Station-Core) removes it from the
  destination list.
- **Privacy note:** stations store no owner data — any player may fly to (or unregister) any
  station.

## Details
- ID: `nerospace:station_charter`
