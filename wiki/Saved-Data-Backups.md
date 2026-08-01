# Saved-Data Backups

Nerospace keeps its world state — oxygen sources, terraformer progress, meteor sites, and the
[launch pad](Rocket-Launch-Pad) and [station](Station-Charter) registries — in the world's saved-data
files, alongside vanilla's own. Starting after 1.0.0, the mod also protects that state against disk
problems.

## What it does

- **Backup files.** Nerospace periodically writes a compact last-known-good copy of each of its
  saved-data stores to `data/nerospace_<name>_backup.dat` inside the matching dimension folder of
  your world save. Backups are only rewritten when something actually changed, and are written
  atomically so an ill-timed crash can never corrupt the backup itself.
- **Automatic recovery.** If Minecraft cannot read one of Nerospace's primary saved-data files
  (a corrupted disk, a truncated file after a power loss), the game no longer crashes. Nerospace
  restores the last-known-good backup instead; if the backup is unreadable too, it falls back to a
  fresh, empty store so the world stays playable. Either way a clean file is rewritten at the next
  world save, and the problem is noted in the log.

## What recovery means in practice

Oxygen fields, terraforming radii, and meteor state largely rebuild themselves from the blocks in
the world within seconds. The pad and station registries are the valuable part — with a backup they
roll back to the last backup point instead of being lost.

## Privacy

Backups contain exactly the same data as the primary files and never leave your world save. A
[data-erasure request](Neroland-Core) is applied to the backup immediately, not just at the next
periodic pass.
