# 存档数据备份

Nerospace 将其世界状态——氧气源、地形改造器进度、陨石地点、[发射台](Rocket-Launch-Pad.md)和[空间站](Station-Charter.md)注册表，以及[货运航线](Cargo-Pad.md)注册表（平台、航线和正在飞行的航班）——保存在世界的存档数据文件中，与原版自身的数据放在一起。从 1.0.0 之后开始，本模组还会保护这些状态免受磁盘问题的影响。 Starting after 1.0.0, the mod also protects that state against disk
problems.

## 它的作用

- \*\*备份文件。\*\*Nerospace 会定期将其每个存档数据存储的一份紧凑的最近可用副本写入你世界存档中对应维度文件夹内的 `data/nerospace_<name>_backup.dat`。只有在内容确实发生变化时才会重写备份，并且以原子方式写入，因此即使在不恰当的时机崩溃也绝不会损坏备份本身。 Backups are only rewritten when something actually changed, and are written
  atomically so an ill-timed crash can never corrupt the backup itself.
- \*\*自动恢复。\*\*如果 Minecraft 无法读取 Nerospace 的某个主存档数据文件（磁盘损坏、断电后文件被截断），游戏不再崩溃。Nerospace 会改为恢复最近可用的备份；如果备份也无法读取，则回退为一个全新的空存储，使世界仍可游玩。无论哪种情况，都会在下一次世界保存时重写一个干净的文件，并在日志中记录该问题。 Nerospace
  restores the last-known-good backup instead; if the backup is unreadable too, it falls back to a
  fresh, empty store so the world stays playable. Either way a clean file is rewritten at the next
  world save, and the problem is noted in the log.

## 恢复在实际中意味着什么

Oxygen fields, terraforming radii, and meteor state largely rebuild themselves from the blocks in
the world within seconds. The pad, station and cargo-route registries are the valuable part — with a
backup they roll back to the last backup point instead of being lost. Cargo Pads re-register
themselves as their chunks load, so even a fresh store heals; a pad that lost its owner that way can be
re-claimed by sneak-right-clicking it, and a flight that was in the air is delivered from the backup.

## 隐私

备份包含与主文件完全相同的数据，并且从不离开你的世界存档。[数据删除请求](Neroland-Core.md)会立即应用到备份上，而不只是在下一次定期处理时。 A
[data-erasure request](Neroland-Core) is applied to the backup immediately, not just at the next
periodic pass.
