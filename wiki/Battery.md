# Battery

> **Moved to Neroland Core.** The Battery now ships in the shared
> [Neroland Core](Neroland-Core) library as **`nerolandcore:battery`**, so every Neroland mod
> uses one set of storage blocks. Craft and use it exactly as before — see the **Neroland Core
> wiki** (the *Battery* page) for the full details.

A passive **Nero energy** store that buffers your grid: generators fill it, machines drain it,
on every side. A **Creative Battery** variant is an endless source/sink for testing.

Nerospace's [Universal Pipe](Universal-Pipe) still connects to it, so in-game behaviour is
unchanged when both mods are installed.

> **Updating an existing world:** blocks you placed as `nerospace:battery` are automatically
> remapped to `nerolandcore:battery` on load — Forge via its missing-mappings event, NeoForge
> and Fabric via a built-in registry alias; their items and stored contents are preserved. See the
> [changelog](https://github.com/Neroland/nerospace/blob/main/CHANGELOG.md).

See also: [Neroland Core](Neroland-Core), [Universal Pipe](Universal-Pipe),
[Creative Source Blocks](Creative-Source-Blocks).
