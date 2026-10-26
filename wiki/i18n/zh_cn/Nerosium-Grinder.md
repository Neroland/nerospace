# Nerosium研磨机

<!-- nerospace:render -->

<p align="right"><img src="images/nerosium_grinder.gif" alt="Nerosium Grinder" width="150" align="right"></p>
<!-- /nerospace:render -->

你的第一台机器——将矿石研磨成粉，使金属产量翻倍。

## 概述

The Nerosium Grinder processes nerosium inputs into **Nerosium Dust** over time. Nerosium研磨机会随时间将 Nerosium 原料加工成 **Nerosium粉**。由于每份粉都能冶炼回一个 Nerosium锭，在冶炼前先研磨矿石可使你的锭产量**翻倍**。

## Obtaining

**合成**（有序）：

```text
I I I
I F I
C C C
```

`I` = Nerosium锭 · `F` = 熔炉 · `C` = 圆石

## How it works

- **Slots:** an **input** (top/sides) and a **four-slot output buffer** (extracted from the bottom).
  **槽位：**一个**输入**槽（顶部／侧面）和一个**四格输出缓存**（从底部提取）。缓存为管道留出了跟上的余地；如果缓存满了，研磨机会**暂停**而不是溢出。

- **研磨配方：**

  - Nerosium矿石／深层Nerosium矿石／粗Nerosium → **2 份 Nerosium粉**
  - Nerosium锭 → **1 份 Nerosium粉**
  - Nerosteel矿石 → **2 个粗Nerosteel**
  - Cindrite矿石 → **2 个Cindrite** · Glacite矿石 → **2 个Glacite** · Xertz石英矿石 → **2 个Xertz石英**

  矿石方块配方意味着**精准采集得到的矿石**（手动挖掘或来自采石场的精准采集模块）永远不会成为死胡同——可以将其研磨回原材料或宝石。

- **界面：**显示电量表和研磨进度。如果电量条一直停在 **0%**，说明没有任何东西在为它供电——在它旁边放一台发电机。**电力：**研磨机**不会**自己发电——在你连接电源之前它会显示 **0%**。在它**正旁边**放置一台 **[被动发电机](Passive-Generator.md)**（供给粗Nerosium）或 **[燃烧发电机](Combustion-Generator.md)**（供给煤炭）：发电机会自行向接触的机器推送电力，无需线缆。距离更远时，用 **[通用管道](Universal-Pipe.md)** 或 Neroland Core 的 **[电池](Battery.md)** 连接它们。参见 **[为机器供电](Powering-Machines.md)**。 Further away, link them with
  **[Universal Pipes](Universal-Pipe)** or a Neroland Core **[Battery](Battery)**. See
  **[Powering Machines](Powering-Machines)**.

- \*\*数据：\*\*20,000 FE 缓存，每刻最多接收 500 FE，研磨时消耗 **20 FE/刻**，每个物品需要 **200 刻（10 秒）**——因此每次研磨消耗 4,000 FE。

- \*\*自动化：**物品栏通过物品能力暴露——漏斗／管道可以**从顶部或侧面向输入槽放入物品，

  并从底部提取粉\*\*。

- **GUI:** shows a power gauge and grind progress. If the power bar stays at **0%**, nothing is
  feeding it — put a generator next to it.

## 研磨陨石岩——随机太空材料

向研磨机投入\*\*[陨石岩](Meteor-Rock.md)\*\*，它会走一条不同的随机路线：它不使用固定配方，而是从 **[Neroland Core](Neroland-Core.md) 的陨石材料注册表**中按权重随机产出一种材料。每个 Nero 模组都会一次性地将其可研磨材料注册到 Core 中，因此材料池会随着已安装的模组自动扩充——研磨机无需了解它们。 Every Nero mod registers its grindable materials into Core once, so the pool grows
automatically with whatever mods are installed — the grinder never needs to know about them.

- **你会得到什么：**每块岩石产出一种**主要**材料，偶尔还会额外获得一个**奇异**物品。Nerospace 向材料池贡献其原材料和标志性行星矿石。
  Nerospace contributes its raw materials and signature planet ores to the pool.
- **Planet-locked signature ores:** each planet's **signature ore** only enters the pool when you grind
  _on that planet_ — and is heavily weighted there (the planet-bias multiplier). Grind on Greenxertz for
  Xertz Quartz and Raw Nerosteel, on Cindara for Cindrite, on Glacira for Glacite. Off a planet (e.g. on
  Earth) those materials drop out entirely. Being on the planet _is_ the gate — there's no separate
  progression lock, so you can never be stranded on a world unable to grind its own ore.
- **Progression-gated alien tech:** the **alien materials** aren't tied to a planet, so they can roll on any
  world — and are instead paced by **Core gates**. **受进度限制的外星科技：** **外星材料**不与行星绑定，因此可以在任何世界上随机出现——它们改由 **Core 门槛**控制节奏。外星碎片在**抵达轨道**时解锁；外星科技废料和奇异的**外星核心**在**第一个殖民地**时解锁。这使稀有的终局掉落物不会出现在早期主世界的陨石研磨中。 This keeps the rare end-game drops out of early
  overworld meteor grinds.
- **Operator context:** the gate/planet check uses the **operator** — the last player to open the grinder,
  or the owner of the station it sits in. With no operator online (and no eligible material) the grind
  simply waits, so an unattended grinder never wastes a rock. If the **output buffer is full**, the grinder
  **pauses** with the result held and the meteor rock kept — it never drops items into the world; grinding
  resumes the moment a slot frees up.
- \*\*配方书：\*\*可以在 **JEI** 或 **EMI** 中浏览随机材料池——查找研磨机，陨石条目会显示完整的 `neroland:meteor/grindable` 材料集。

隐私：注册表**不储存任何玩家数据**——只有物品元数据。操作者仅以 UUID 形式保存，用于门槛上下文，从不记录（POPIA/GDPR）。 The operator is held by UUID for
gate context only, never logged (POPIA/GDPR).

## 提示

Feed it raw nerosium or ore, pipe the dust into a furnace array, and you get 2 ingots per ore instead
of 1. 为它供给粗Nerosium 或矿石，将粉通过管道送入熔炉阵列，你每个矿石就能获得 2 个锭而不是 1 个。把你的**陨石岩**留着做随机材料研磨——&#x5E76;_&#x5728;对应的行星&#x4E0A;_&#x7814;磨，使结果偏向该行星的标志性矿石。

## 详细信息

- ID：`nerospace:nerosium_grinder` · 工具：镐，铁级 · 掉落：自身
