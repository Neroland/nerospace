# 物品存储器

> \*\*已迁移到 Neroland Core。\*\*物品存储器现在作为 **`nerolandcore:item_store`** 由共享的 [Neroland Core](Neroland-Core.md) 库提供，因此所有 Neroland 模组都使用同一套存储方块。合成和使用方式与之前完全相同——完整详情请参阅 **Neroland Core 维基**（_Item Store_ 页面）。 Craft and use it exactly as before — see the **Neroland Core
> wiki** (the _Item Store_ page) for the full details.

一个带箱子式界面的 27 格容器，专为管道自动化打造：漏斗和管道可以在每一个面（包括顶面）输入／输出。**创造物品存储器**变体可无限供应一种已配置的物品。 A **Creative Item Store** variant supplies
an endless stream of one configured item.

Nerospace 的[通用管道](Universal-Pipe.md)仍然可以向其输入和从中抽出，因此同时安装两个模组时，游戏内行为保持不变。

> \*\*更新已有世界：\*\*你以 `nerospace:item_store` 放置的方块会在加载时自动重新映射为 `nerolandcore:item_store`——Forge 通过其缺失映射事件，NeoForge 和 Fabric 通过内置的注册表别名；其物品和储存的内容都会保留。参见[更新日志](https://github.com/Neroland/nerospace/blob/main/CHANGELOG.md)。 See the
> [changelog](https://github.com/Neroland/nerospace/blob/main/CHANGELOG.md).

另请参阅：[Neroland Core](Neroland-Core.md)、[通用管道](Universal-Pipe.md)、[创造源方块](Creative-Source-Blocks.md)。
