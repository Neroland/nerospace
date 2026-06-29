# Creative Source Blocks

> **Moved to Neroland Core.** The Creative storage blocks now ship in the shared
> [Neroland Core](Neroland-Core) library as **`nerolandcore:creative_battery`**,
> **`nerolandcore:creative_fluid_tank`**, and **`nerolandcore:creative_gas_tank`** (alongside the
> regular [Battery](Battery), [Fluid Tank](Fluid-Tank), [Gas Tank](Gas-Tank), and
> [Item Store](Item-Store)). See the **Neroland Core wiki** (the *Creative Source Blocks* page)
> for the full details.

Endless sources (and voids) for testing pipe networks — creative tab only, unbreakable in
survival, no recipes. Because Neroland Core ships no specific fluids or gases, two of them now
configure their resource in-world:

- **Creative Fluid Tank** starts **empty** — right-click with a filled bucket (e.g. a Rocket
  Fuel Bucket) to set the endless fluid.
- **Creative Gas Tank** **learns its gas from the first gas piped into it** (pipe in some Oxygen
  to set it) — it is no longer hard-wired to Oxygen.
- **Creative Battery** is an endless energy source/sink as before.

Nerospace's [Universal Pipe](Universal-Pipe) still connects to all of them, so the
`/nerospace gallery` demo rows behave as before when both mods are installed.

See also: [Neroland Core](Neroland-Core), [Universal Pipe](Universal-Pipe),
[Battery](Battery), [Fluid Tank](Fluid-Tank), [Gas Tank](Gas-Tank), [Item Store](Item-Store).
