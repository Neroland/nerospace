# Mod Compatibility

What Nerospace shares with the other mods in your pack, and what it keeps to itself. Nothing here
needs setting up — it is how the blocks behave once both mods are installed. (For the developer-facing
integration surface, see [Public Integration API](Public-API).)

## What crosses the mod boundary

| Layer | Crosses? | How |
| --- | --- | --- |
| Energy (FE) | **yes** | Through [Neroland Core](Neroland-Core)'s shared power network. |
| Fluid | **yes** | Nerospace offers and reads the platform's standard fluid handler on NeoForge, Forge and Fabric. |
| Items | **yes** | The same, on the platform's standard item handler — plus vanilla containers as always. |
| Gas | **no** | There is no cross-mod standard for gas, and Nerospace's oxygen is not a fluid. |

In practice a [Universal Pipe](Universal-Pipe) connects to, pulls from and pushes into another mod's
tanks, machines and pipes — and another mod's pipes can drain or fill a Nerospace
[Fluid Tank](Fluid-Tank), [Fuel Tank](Fuel-Tank), [Fuel Refinery](Fuel-Refinery),
[Quarry Controller](Quarry-Controller), [Launch Controller](Launch-Controller) or Universal Pipe
directly, with no Nerospace pipe in the line at all. A [quarry](Quarry-Controller) can eject its
mined items and the water it swallows into whatever storage you already run.

## The one exception: the Rocket Launch Pad

The [Rocket Launch Pad](Rocket-Launch-Pad)'s fluid connection is one-way — it feeds a docked rocket and
can never be drained back out. The standard fluid handlers are transactional: another mod may insert and
then undo it, as its ordinary way of asking "how much would you take?", and a one-way sink cannot answer
that honestly. So the pad accepts fuel from Nerospace's own pipes only. To fuel a rocket from another
mod's system, pipe into a [Fuel Tank](Fuel-Tank) beside the pad and let it fuel the rocket as usual.

## Your face settings still hold

Per-face I/O modes and [pipe filters](Pipe-Filters-and-Upgrades) treat a foreign neighbour exactly like
a Nerospace one. A face set to **Off** with the [Configurator](Configurator) looks shut to another mod's
pipe too, and a filtered face passes only what you told it to pass — cross-mod reach never costs you
control over what moves where.

## Gas stays Nerospace-side

Oxygen and the pipe's green layer move between Nerospace (and Neroland) blocks only. Pipe oxygen from an
[Oxygen Generator](Oxygen-Generator) to a [Gas Tank](Gas-Tank) as you always have; do not expect another
mod's gas system to read it, or Nerospace to read theirs.

## Recipes

Nerospace's recipes are tag-based throughout, so another mod's equivalent ingredients — its glass,
redstone, ore drops and the common `c:` material tags — work in them wherever the tag matches.

## The caveat worth reading

All of the above is Nerospace's half of the deal: it speaks the standard, platform-native APIs. Whether
any *particular* other mod connects depends on that mod using them as well. If a block from another mod
refuses to link up, that is what to check first.

---

See also: [Universal Pipe](Universal-Pipe), [Neroland Core](Neroland-Core),
[Public Integration API](Public-API), [Fluid Tank](Fluid-Tank).
