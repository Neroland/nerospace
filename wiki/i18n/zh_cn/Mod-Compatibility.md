# 模组兼容性

> 本页为机器翻译草稿，仅供参考，以英文版本为准：[English](../../Mod-Compatibility.md)。

Nerospace 与你整合包中的其他模组共享什么，又保留什么。这里的一切都无需设置——这就是安装两个模组后方块的行为方式。（面向开发者的集成接口参见[公共集成 API](Public-API.md)。）

## 跨越模组边界的内容

| 层 | 能否跨越？ | 方式 |
| --- | --- | --- |
| 能量（FE） | **能** | 通过 [Neroland Core](Neroland-Core.md) 的共享电网。 |
| 流体 | **能** | Nerospace 在 NeoForge、Forge 和 Fabric 上提供并读取平台的标准流体处理器。 |
| 物品 | **能** | 同上，使用平台的标准物品处理器——以及一如既往的原版容器。 |
| 气体 | **仅限 Neroland 模组** | 通过 [Neroland Core](Neroland-Core.md) 的共享气体能力。除此之外没有跨模组的气体标准。 |

实际上，[通用管道](Universal-Pipe.md)可以连接其他模组的储罐、机器和管道，从中抽取并向其推送——而其他模组的管道也可以直接抽取或注入 Nerospace 的[流体储罐](Fluid-Tank.md)、[燃料储罐](Fuel-Tank.md)、[燃料精炼厂](Fuel-Refinery.md)、[采石场控制器](Quarry-Controller.md)、[发射控制器](Launch-Controller.md)或通用管道，整条管线中完全不需要 Nerospace 管道。[采石场](Quarry-Controller.md)可以将挖到的物品和吸入的水排出到你已有的任何存储中。

## 唯一的例外：火箭发射台

[火箭发射台](Rocket-Launch-Pad.md)的流体连接是单向的——它为停靠的火箭供油，且永远无法被抽回。标准流体处理器是事务性的：其他模组可能会先插入再撤销，这是它询问“你能接收多少？”的常规方式，而单向汇无法如实回答这个问题。因此发射台只接受来自 Nerospace 自身管道的燃料。要从其他模组的系统为火箭加注燃料，请将燃料输入发射台旁的[燃料储罐](Fuel-Tank.md)，让它像往常一样为火箭加注。

## 你的面设置依然有效

逐面输入／输出模式和[管道过滤器](Pipe-Filters-and-Upgrades.md)对待外来的相邻方块与对待 Nerospace 方块完全相同。用[配置器](Configurator.md)设为**关闭**的面，对其他模组的管道来说也是关闭的；带过滤器的面只放行你指定的内容——跨模组连接绝不会让你失去对什么东西流向哪里的控制。

## 气体只在 Neroland 模组内部流通

管道的气体层通过 Core 的共享气体能力，在 Nerospace、[Neroland Core](Neroland-Core.md) 和其他 Neroland 模组（例如 NeroTech）之间输送气体。它可以输送它们的任何气体：来自[氧气发生器](Oxygen-Generator.md)或电解槽的氧气、来自电解槽的氢气，输送到[气体储罐](Gas-Tank.md)或化学处理器中。在每个 Neroland 模组中，氧气都是同一种气体 `nerospace:oxygen`。Neroland 模组之外没有气体标准，因此不要指望其他模组的气体系统能读取它，或者 Nerospace 能读取它们的。

## 配方

Nerospace 的配方全部基于标签，因此其他模组的等效材料——它们的玻璃、红石、矿石掉落物以及通用的 `c:` 材料标签——只要标签匹配就可以在配方中使用。

## 任务（可选——NeroQuests）

Nerospace 附带一个任务章节 **星旅日志**（19 个任务），在安装了同系列的 **NeroQuests** 模组时会出现在任务书中。它遵循本模组自身的进度——第一块 Nerosium、电力、发射、轨道、各行星、氧气服、殖民地、货运和地形改造——外加一个累计一百次货运交付的共享世界日志任务。NeroQuests 不是必需的：没有它时，这些内容只是 jar 中闲置的数据。

这些任务背后的里程碑是普通的 Neroland Core 进度门槛和阈值事件，因此任何基于 Core 的模组都可以读取它们：玩家作用范围的记录门槛 `nerospace:first_freight`、`nerospace:terraformed_ground` 和 `nerospace:living_world`，以及世界频道 `nerospace:cargo_delivered`（作用范围 = 目的地维度 ID；档位 1 / 10 / 100 / 1000）。两个*钩子*门槛 `nerospace:hook/reveal_star_guide_page` 和 `nerospace:hook/oxygen_top_up` 的作用方向相反：为某位玩家开启其中一个，Nerospace 就会向其发送一条星际指南提示或为其补满一次氧气，然后再次关闭该门槛。这些门槛都不会锁定任何东西。

## 值得一读的注意事项

以上都是 Nerospace 这一方的职责：它使用标准的、平台原生的 API。任何*特定的*其他模组能否连接，取决于该模组是否也使用这些 API。如果其他模组的某个方块拒绝连接，首先应该检查这一点。

---

另请参阅：[通用管道](Universal-Pipe.md)、[Neroland Core](Neroland-Core.md)、[公共集成 API](Public-API.md)、[流体储罐](Fluid-Tank.md)。
