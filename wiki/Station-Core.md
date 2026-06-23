# Station Core

The anchor block of a player-founded station.

## Overview

Every station founded with a **[Station Charter](Station-Charter)** is built around a Station Core.
It marks the station's registration: as long as the Core stands, the station is listed as a rocket
destination.

## Obtaining

**Not craftable.** A Station Core is placed automatically by the founding flow (FOUND node in the
rocket UI). Breaking one pops the installed **Station Charter** back out — name preserved — so a
station can be dissolved and re-founded elsewhere.

## How it works

- **Anchor:** the Core binds the platform to its registry slot; it survives restarts and chunk
  unloads.
- **Unregister:** break the Core and the station disappears from every rocket's destination list
  (rockets already heading there fall back gracefully).
- **No ownership:** stations don't record who founded them — any player can use or dissolve any
  station.

## Details

- ID: `nerospace:station_core` · Dimension: the station dimension
