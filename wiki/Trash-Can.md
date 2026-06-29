# Trash Can

> **Moved to Neroland Core.** The Trash Can now ships in the shared
> [Neroland Core](Neroland-Core) library as **`nerolandcore:trash_can`**, so every Neroland mod
> uses one set of storage blocks. Craft and use it exactly as before — see the **Neroland Core
> wiki** (the *Trash Can* page) for the full details.

A bottomless void sink: pipe or hopper items, fluid, or gas into it and they are destroyed
(input only, no extraction), via a vanilla chest-style GUI with a single drop slot.

Nerospace's [Universal Pipe](Universal-Pipe) still voids into it through the compat bridge, so
in-game behaviour is unchanged when both mods are installed.

> **Updating an existing world:** blocks you placed as `nerospace:trash_can` are remapped to
> `nerolandcore:trash_can` on load by Core's block-id alias mechanism (Forge `MissingMappingsEvent`;
> NeoForge/Fabric via the registry-remap mixin), so existing placements carry over. See the
> [changelog](https://github.com/Neroland/nerospace/blob/main/CHANGELOG.md).

See also: [Neroland Core](Neroland-Core), [Universal Pipe](Universal-Pipe),
[Creative Source Blocks](Creative-Source-Blocks).
