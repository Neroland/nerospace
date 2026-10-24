# 垃圾桶

> 本页为机器翻译草稿，仅供参考，以英文版本为准：[English](../../Trash-Can.md)。

> **已迁移到 Neroland Core。**垃圾桶现在作为 **`nerolandcore:trash_can`** 由共享的 [Neroland Core](Neroland-Core.md) 库提供，因此所有 Neroland 模组都使用同一套存储方块。合成和使用方式与之前完全相同——完整详情请参阅 **Neroland Core 维基**（*Trash Can* 页面）。

一个无底虚空：用管道或漏斗输入的物品、流体或气体都会被销毁（仅输入，无法提取），带有只有单个投放槽位的原版箱子式界面。

Nerospace 的[通用管道](Universal-Pipe.md)仍然可以通过兼容桥接向其销毁物品，因此同时安装两个模组时，游戏内行为保持不变。

> **更新已有世界：**你以 `nerospace:trash_can` 放置的方块会在加载时通过 Core 的方块 ID 别名机制重新映射为 `nerolandcore:trash_can`（Forge `MissingMappingsEvent`；NeoForge/Fabric 通过注册表重映射 mixin），因此已放置的方块会保留。参见[更新日志](https://github.com/Neroland/nerospace/blob/main/CHANGELOG.md)。

另请参阅：[Neroland Core](Neroland-Core.md)、[通用管道](Universal-Pipe.md)、[创造源方块](Creative-Source-Blocks.md)。
