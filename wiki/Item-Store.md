# Item Store

> **Moved to Neroland Core.** The Item Store now ships in the shared
> [Neroland Core](Neroland-Core) library as **`nerolandcore:item_store`**, so every Neroland mod
> uses one set of storage blocks. Craft and use it exactly as before — see the **Neroland Core
> wiki** (the *Item Store* page) for the full details.

A 27-slot container with a chest-style GUI, built for pipe automation: hoppers and pipes can
insert/extract on every side, including the top face. A **Creative Item Store** variant supplies
an endless stream of one configured item.

Nerospace's [Universal Pipe](Universal-Pipe) still feeds and drains it, so in-game behaviour is
unchanged when both mods are installed.

> **Updating an existing world:** blocks you placed as `nerospace:item_store` are automatically
> remapped to `nerolandcore:item_store` on load — Forge via its missing-mappings event, NeoForge
> and Fabric via a built-in registry alias; their items and stored contents are preserved. See the
> [changelog](https://github.com/Neroland/nerospace/blob/main/CHANGELOG.md).

See also: [Neroland Core](Neroland-Core), [Universal Pipe](Universal-Pipe),
[Creative Source Blocks](Creative-Source-Blocks).
