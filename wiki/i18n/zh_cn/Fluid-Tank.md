# 流体储罐

> \*\*已迁移到 Neroland Core。\*\*流体储罐现在作为 **`nerolandcore:fluid_tank`** 由共享的 [Neroland Core](Neroland-Core.md) 库提供，因此所有 Neroland 模组都使用同一套存储方块。合成和使用方式与之前完全相同——完整详情请参阅 **Neroland Core 维基**（_Fluid Tank_ 页面）。（不要与用于为火箭加注燃料的[燃料储罐](Fuel-Tank.md)机器混淆，后者仍保留在 Nerospace 中。） Craft and use it exactly as before — see the **Neroland
> Core wiki** (the _Fluid Tank_ page) for the full details. (Not to be confused with the
> rocket-fuelling [Fuel Tank](Fuel-Tank) machine, which stays in Nerospace.)

A passive single-fluid store: right-click with a bucket to fill/empty, or pipe fluid in and
out on every side. A **Creative Fluid Tank** variant supplies an endless fluid.

Nerospace 的[通用管道](Universal-Pipe.md)仍然可以向其输入和从中输出火箭燃料及其他流体，因此同时安装两个模组时，游戏内行为保持不变。

> \*\*更新已有世界：\*\*你以 `nerospace:fluid_tank` 放置的方块会在加载时自动重新映射为 `nerolandcore:fluid_tank`——Forge 通过其缺失映射事件，NeoForge 和 Fabric 通过内置的注册表别名；其物品和储存的内容都会保留。参见[更新日志](https://github.com/Neroland/nerospace/blob/main/CHANGELOG.md)。 See the
> [changelog](https://github.com/Neroland/nerospace/blob/main/CHANGELOG.md).

## 其他模组的流体管道

**该储罐不再是 Nerospace 专用的端点。**它在每个加载器上都响应平台的标准流体处理器，因此**其他模组的流体管道可以将其抽入更大的容器，或为其注入**，整条管线中无需任何 Nerospace 管道。反过来也可以：[通用管道](Universal-Pipe.md)也能从其他模组的储罐中抽取或向其推送，因此你可以在这里缓存，再用你已有的任何管道将流体继续输送。 It works the other way too: a
[Universal Pipe](Universal-Pipe) will happily pull from or push into another mod's tank, so you
can buffer here and move the fluid onward with whatever pipes you already have.

逐面输入／输出模式仍然决定某个面的作用——你设为**输出**的面，对外来管道和对 Nerospace 管道一样都保持为输出。某个模组能否连接，取决于该模组是否也使用标准处理器；完整说明参见[通用管道](Universal-Pipe.md)。 Whether a given mod connects depends on that mod
using the standard handlers too; see [Universal Pipe](Universal-Pipe) for the full picture.

另请参阅：[Neroland Core](Neroland-Core.md)、[通用管道](Universal-Pipe.md)、[燃料储罐](Fuel-Tank.md)、[创造源方块](Creative-Source-Blocks.md)。
