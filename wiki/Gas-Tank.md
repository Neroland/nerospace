# Gas Tank

> **Moved to Neroland Core.** The Gas Tank now ships in the shared
> [Neroland Core](Neroland-Core) library as **`nerolandcore:gas_tank`**, so every Neroland mod
> uses one set of storage blocks. Craft and use it exactly as before — see the **Neroland Core
> wiki** (the *Gas Tank* page) for the full details.

A pressurised store for one gas, filled and drained through the gas layer of the pipe network.
In Nerospace it still buffers **Oxygen** from an [Oxygen Generator](Oxygen-Generator) and works
as an **airlock** that refills a worn [Oxygen Suit](Oxygen-Suit) nearby. A **Creative Gas
Tank** variant supplies an endless gas.

Nerospace's [Universal Pipe](Universal-Pipe) still moves oxygen in and out of it, so in-game
behaviour is unchanged when both mods are installed.

> **Updating an existing world:** blocks you placed as `nerospace:gas_tank` are automatically remapped to `nerolandcore:gas_tank` on load — Forge via its missing-mappings event, NeoForge and Fabric via a built-in registry alias; their items and stored contents are preserved. See the
> [changelog](https://github.com/Neroland/nerospace/blob/main/CHANGELOG.md).

See also: [Neroland Core](Neroland-Core), [Universal Pipe](Universal-Pipe),
[Oxygen Suit](Oxygen-Suit), [Creative Source Blocks](Creative-Source-Blocks).
