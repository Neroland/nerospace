# 为机器供电

大多数 Nerospace 机器都需要电力（显示为 **FE**）。它们**不会**自己发电。如果某台机器的电量条一直停在 **0%**，说明没有任何东西在为它供电。 They do **not** make power themselves. If a
machine's power bar stays at **0%**, nothing is feeding it.

## 简而言之

1. 建造一台**发电机**。
2. 把它放在机器**正旁边**（接触任意一面）。
3. 往发电机里放燃料。

That's it. Generators push power into every machine they touch on their own. 就这么简单。发电机会自行向它接触的每台机器推送电力。与发电机接触的机器不需要线缆。

## 发电机

| 发电机                              | 燃料                             | 功率                                   | 一个物品可持续多久                                     |
| -------------------------------- | ------------------------------ | ------------------------------------ | --------------------------------------------- |
| [燃烧发电机](Combustion-Generator.md) | 煤炭、木炭、煤炭块、烈焰棒、火箭燃料罐            | 20 FE/刻                              | 煤炭：80 秒 · 煤炭块：13 分钟 · 烈焰棒：2 分钟 · 燃料罐：3 分 20 秒 |
| [被动发电机](Passive-Generator.md)    | 粗Nerosium、Nerosium锭或 Nerosium粉 | 8 FE/刻                               | 20 分钟                                         |
| [太阳能板](Solar-Panel.md)           | 阳光（无需供给）                       | 全日照下 1 级：20 · 2 级：100 · 3 级：400 FE/刻 | 永久                                            |

- The **Combustion** and **Passive** generators need **Nerosteel Ingots** to craft. Nerosteel Ore is
  found deep in the Overworld (below about y 16) — see [Nerosteel Ore](Nerosteel-Ore).
- **太阳能板**需要 Xertz石英，你稍后会在 Greenxertz 上获得。
- 在没有大气的世界（空间站和行星）上，太阳能板的发电量会**翻倍**，即使在屋顶下也能工作。

## 机器耗电多少？

| 机器                                 | Uses          | Good starter setup               |
| ---------------------------------- | ------------- | -------------------------------- |
| [Nerosium研磨机](Nerosium-Grinder.md) | 研磨时 20 FE/刻   | 1 台燃烧发电机（全速）或 1 台被动发电机（约 40% 速度） |
| [氧气发生器](Oxygen-Generator.md)       | 制氧时 80 FE/刻   | 4 台燃烧发电机，或太阳能                    |
| [燃料精炼厂](Fuel-Refinery.md)          | 精炼时 40 FE/刻   | 2 台燃烧发电机                         |
| [采石场控制器](Quarry-Controller.md)     | 每挖掘一个方块 40 FE | 1–2 台发电机                         |
| [地形改造器](Terraformer.md)            | 每列 12–48 FE   | 多台发电机或 2／3 级太阳能                  |

获得的电力少于其消耗的机器仍然可以工作——只是会运行得更慢，断断续续。

## 将电力输送到更远处

- **[通用管道](Universal-Pipe.md)**——可输送电力、流体、气体和物品。用它将发电机连接到未与之接触的机器。 Connect a generator to
  machines that are not touching it.
- **[电池](Battery.md)**（来自 Neroland Core）——储存电力，使你的机器在发电机燃料耗尽或太阳落山时仍能继续运行。
- **[Configurator](Configurator)** — right-click a machine face to choose what that side does (power
  in, items in, items out, off).

## 常见问题

- **电量为 0%**——没有发电机与机器接触或通过管道连接。或者发电机没有燃料。 Or the generator has no
  fuel.
- **发电机有燃料但机器仍为 0%**——用[配置器](Configurator.md)检查该面；设&#x4E3A;_&#x5173;&#x95ED;_&#x7684;面不会接收电力。
- **发电机停止燃烧**——它自身的储存已满。这是正常的：它会等待，直到有东西消耗电力。 That is normal: it waits until something uses
  the power.
