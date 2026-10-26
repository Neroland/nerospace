# 模组兼容性

Nerospace 与你整合包中的其他模组共享什么，又保留什么。这里的一切都无需设置——这就是安装两个模组后方块的行为方式。（面向开发者的集成接口参见[公共集成 API](Public-API.md)。） Nothing here
needs setting up — it is how the blocks behave once both mods are installed. (For the developer-facing
integration surface, see [Public Integration API](Public-API).)

## What crosses the mod boundary

| 层      | Crosses?           | 方式                                                                                                                               |
| ------ | ------------------ | -------------------------------------------------------------------------------------------------------------------------------- |
| 能量（FE） | **能**              | 通过 [Neroland Core](Neroland-Core.md) 的共享电网。                                                                                      |
| 流体     | **能**              | Nerospace 在 NeoForge、Forge 和 Fabric 上提供并读取平台的标准流体处理器。                                                                            |
| 物品     | **能**              | The same, on the platform's standard item handler — plus vanilla containers as always.                           |
| 气体     | **仅限 Neroland 模组** | 通过 [Neroland Core](Neroland-Core.md) 的共享气体能力。除此之外没有跨模组的气体标准。 There is no cross-mod standard for gas beyond that. |

实际上，[通用管道](Universal-Pipe.md)可以连接其他模组的储罐、机器和管道，从中抽取并向其推送——而其他模组的管道也可以直接抽取或注入 Nerospace 的[流体储罐](Fluid-Tank.md)、[燃料储罐](Fuel-Tank.md)、[燃料精炼厂](Fuel-Refinery.md)、[采石场控制器](Quarry-Controller.md)、[发射控制器](Launch-Controller.md)或通用管道，整条管线中完全不需要 Nerospace 管道。[采石场](Quarry-Controller.md)可以将挖到的物品和吸入的水排出到你已有的任何存储中。 A [quarry](Quarry-Controller) can eject its
mined items and the water it swallows into whatever storage you already run.

## 唯一的例外：火箭发射台

The [Rocket Launch Pad](Rocket-Launch-Pad)'s fluid connection is one-way — it feeds a docked rocket and
can never be drained back out. The standard fluid handlers are transactional: another mod may insert and
then undo it, as its ordinary way of asking "how much would you take?", and a one-way sink cannot answer
that honestly. So the pad accepts fuel from Nerospace's own pipes only. [火箭发射台](Rocket-Launch-Pad.md)的流体连接是单向的——它为停靠的火箭供油，且永远无法被抽回。标准流体处理器是事务性的：其他模组可能会先插入再撤销，这是它询问“你能接收多少？”的常规方式，而单向汇无法如实回答这个问题。因此发射台只接受来自 Nerospace 自身管道的燃料。要从其他模组的系统为火箭加注燃料，请将燃料输入发射台旁的[燃料储罐](Fuel-Tank.md)，让它像往常一样为火箭加注。

## 你的面设置依然有效

Per-face I/O modes and [pipe filters](Pipe-Filters-and-Upgrades) treat a foreign neighbour exactly like
a Nerospace one. A face set to **Off** with the [Configurator](Configurator) looks shut to another mod's
pipe too, and a filtered face passes only what you told it to pass — cross-mod reach never costs you
control over what moves where.

## 气体只在 Neroland 模组内部流通

管道的气体层通过 Core 的共享气体能力，在 Nerospace、[Neroland Core](Neroland-Core.md) 和其他 Neroland 模组（例如 NeroTech）之间输送气体。它可以输送它们的任何气体：来自[氧气发生器](Oxygen-Generator.md)或电解槽的氧气、来自电解槽的氢气，输送到[气体储罐](Gas-Tank.md)或化学处理器中。在每个 Neroland 模组中，氧气都是同一种气体 `nerospace:oxygen`。Neroland 模组之外没有气体标准，因此不要指望其他模组的气体系统能读取它，或者 Nerospace 能读取它们的。 It carries any of their gases: oxygen from
an [Oxygen Generator](Oxygen-Generator) or an Electrolyzer, hydrogen from an Electrolyzer, into a
[Gas Tank](Gas-Tank) or a Chemical Processor. Oxygen is one gas, `nerospace:oxygen`, in every Neroland
mod. There is no gas standard beyond the Neroland mods, so do not expect another mod's gas system to read
it, or Nerospace to read theirs.

## 配方

Nerospace 的配方全部基于标签，因此其他模组的等效材料——它们的玻璃、红石、矿石掉落物以及通用的 `c:` 材料标签——只要标签匹配就可以在配方中使用。

## 任务（可选——NeroQuests）

Nerospace 附带一个任务章节 **星旅日志**（19 个任务），在安装了同系列的 **NeroQuests** 模组时会出现在任务书中。它遵循本模组自身的进度——第一块 Nerosium、电力、发射、轨道、各行星、氧气服、殖民地、货运和地形改造——外加一个累计一百次货运交付的共享世界日志任务。NeroQuests 不是必需的：没有它时，这些内容只是 jar 中闲置的数据。 It follows the mod's own progression — first Nerosium, power,
launch, orbit, the planets, suits, colonies, freight and terraforming — plus one shared world-log quest for
a hundred cargo deliveries. NeroQuests is not required: without it the pack is inert data in the jar.

这些任务背后的里程碑是普通的 Neroland Core 进度门槛和阈值事件，因此任何基于 Core 的模组都可以读取它们：玩家作用范围的记录门槛 `nerospace:first_freight`、`nerospace:terraformed_ground` 和 `nerospace:living_world`，以及世界频道 `nerospace:cargo_delivered`（作用范围 = 目的地维度 ID；档位 1 / 10 / 100 / 1000）。两&#x4E2A;_&#x94A9;&#x5B50;_&#x95E8;槛 `nerospace:hook/reveal_star_guide_page` 和 `nerospace:hook/oxygen_top_up` 的作用方向相反：为某位玩家开启其中一个，Nerospace 就会向其发送一条星际指南提示或为其补满一次氧气，然后再次关闭该门槛。这些门槛都不会锁定任何东西。 Two _hook_ gates,
`nerospace:hook/reveal_star_guide_page` and `nerospace:hook/oxygen_top_up`, work the other way round: open
one for a player and Nerospace sends them a Star Guide hint or refills their oxygen once, then closes the
gate again. None of these gates lock anything.

## 值得一读的注意事项

All of the above is Nerospace's half of the deal: it speaks the standard, platform-native APIs. Whether
any _particular_ other mod connects depends on that mod using them as well. If a block from another mod
refuses to link up, that is what to check first.

---

另请参阅：[通用管道](Universal-Pipe.md)、[Neroland Core](Neroland-Core.md)、[公共集成 API](Public-API.md)、[流体储罐](Fluid-Tank.md)。
