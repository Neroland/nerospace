# 电池

> 本页为机器翻译草稿，仅供参考，以英文版本为准：[English](../../Battery.md)。

> **已迁移到 Neroland Core。**电池现在作为 **`nerolandcore:battery`** 由共享的 [Neroland Core](Neroland-Core.md) 库提供，因此所有 Neroland 模组都使用同一套存储方块。合成和使用方式与之前完全相同——完整详情请参阅 **Neroland Core 维基**（*Battery* 页面）。

一种被动的 **Nero 能量**存储方块，为你的电网提供缓冲：发电机为其充能，机器从中取电，每一个面都可以。**创造电池**变体是用于测试的无限源／汇。

Nerospace 的[通用管道](Universal-Pipe.md)仍然可以连接它，因此同时安装两个模组时，游戏内行为保持不变。

> **更新已有世界：**你以 `nerospace:battery` 放置的方块会在加载时自动重新映射为 `nerolandcore:battery`——Forge 通过其缺失映射事件，NeoForge 和 Fabric 通过内置的注册表别名；其物品和储存的内容都会保留。参见[更新日志](https://github.com/Neroland/nerospace/blob/main/CHANGELOG.md)。

另请参阅：[Neroland Core](Neroland-Core.md)、[通用管道](Universal-Pipe.md)、[创造源方块](Creative-Source-Blocks.md)。
