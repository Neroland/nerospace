# 气体储罐

> 本页为机器翻译草稿，仅供参考，以英文版本为准：[English](../../Gas-Tank.md)。

> **已迁移到 Neroland Core。**气体储罐现在作为 **`nerolandcore:gas_tank`** 由共享的 [Neroland Core](Neroland-Core.md) 库提供，因此所有 Neroland 模组都使用同一套存储方块。合成和使用方式与之前完全相同——完整详情请参阅 **Neroland Core 维基**（*Gas Tank* 页面）。

一种储存单一气体的加压存储方块，通过管道网络的气体层注入和抽出。在 Nerospace 中，它仍然可以缓存来自[氧气发生器](Oxygen-Generator.md)的**氧气**，并作为**气闸**为附近穿着的[氧气服](Oxygen-Suit.md)补充氧气。**创造气体储罐**变体可无限供应气体。

Nerospace 的[通用管道](Universal-Pipe.md)仍然可以向其输入和从中输出氧气，因此同时安装两个模组时，游戏内行为保持不变。

> **更新已有世界：**你以 `nerospace:gas_tank` 放置的方块会在加载时自动重新映射为 `nerolandcore:gas_tank`——Forge 通过其缺失映射事件，NeoForge 和 Fabric 通过内置的注册表别名；其物品和储存的内容都会保留。参见[更新日志](https://github.com/Neroland/nerospace/blob/main/CHANGELOG.md)。

另请参阅：[Neroland Core](Neroland-Core.md)、[通用管道](Universal-Pipe.md)、[氧气服](Oxygen-Suit.md)、[创造源方块](Creative-Source-Blocks.md)。
