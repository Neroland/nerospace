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

See also: [Neroland Core](Neroland-Core), [Universal Pipe](Universal-Pipe),
[Fuel Tank](Fuel-Tank), [Creative Source Blocks](Creative-Source-Blocks).
