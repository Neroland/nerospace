# Fluid Tank

> **Moved to Neroland Core.** The Fluid Tank now ships in the shared
> [Neroland Core](Neroland-Core) library as **`nerolandcore:fluid_tank`**, so every Neroland
> mod uses one set of storage blocks. Craft and use it exactly as before — see the **Neroland
> Core wiki** (the *Fluid Tank* page) for the full details. (Not to be confused with the
> rocket-fuelling [Fuel Tank](Fuel-Tank) machine, which stays in Nerospace.)

A passive single-fluid store: right-click with a bucket to fill/empty, or pipe fluid in and
out on every side. A **Creative Fluid Tank** variant supplies an endless fluid.

Nerospace's [Universal Pipe](Universal-Pipe) still moves rocket fuel and other fluids in and
out of it, so in-game behaviour is unchanged when both mods are installed.

> **Updating an existing world:** blocks you placed as `nerospace:fluid_tank` are automatically
> remapped to `nerolandcore:fluid_tank` on load — Forge via its missing-mappings event, NeoForge
> and Fabric via a built-in registry alias; their items and stored contents are preserved. See the
> [changelog](https://github.com/Neroland/nerospace/blob/main/CHANGELOG.md).

## Other mods' fluid pipes

**The tank is no longer a Nerospace-only endpoint.** It answers the platform's standard fluid
handler on every loader, so **another mod's fluid pipe can drain it into a larger container, or
fill it**, with no Nerospace pipe anywhere in the line. It works the other way too: a
[Universal Pipe](Universal-Pipe) will happily pull from or push into another mod's tank, so you
can buffer here and move the fluid onward with whatever pipes you already have.

Per-face I/O modes still decide what a side does — a face you set to **Out** stays an output for a
foreign pipe just as it does for a Nerospace one. Whether a given mod connects depends on that mod
using the standard handlers too; see [Universal Pipe](Universal-Pipe) for the full picture.

See also: [Neroland Core](Neroland-Core), [Universal Pipe](Universal-Pipe),
[Fuel Tank](Fuel-Tank), [Creative Source Blocks](Creative-Source-Blocks).
