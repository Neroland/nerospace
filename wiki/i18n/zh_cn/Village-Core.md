# 村庄核心

<!-- nerospace:render -->

<p align="right"><img src="images/village_core.png" alt="Village Core" width="150" align="right"></p>
<!-- /nerospace:render -->

外星村庄中心的控制方块——认领它、为它储备物资，并教会村庄自行建设。

## 概述

**村庄核心**是外星村庄系统的中枢：它储存所有者、建筑物资库存、建造队列、被动产出以及当前任务。它出现在生成的\*\*[建筑](Alien-Structures.md)\*\*中，也可以手动放置。 It appears in generated
**[structures](Alien-Structures)** and can be placed by hand.

## Obtaining

- 位于每个\*\*[小村落、遗迹和巨型城市](Alien-Structures.md)\*\*的中心。
- 可在 Nerospace 创造模式物品栏中获得。

## 认领与发展村庄

1. **认领**——右键点击未被认领的核心，成为其所有者。

2. **储备物资**——手持 **Nerosteel块**右键点击，填充建筑物资库。

3. **Teach the next building** — right-click as the owner. **教授下一座建筑**——以所有者身份右键点击。如果附近的\*\*[村民](Alien-Villagers.md)\*\*

   足够信任&#x4F60;_&#x5E76;&#x4E14;_&#x7269;资库中有足够的材料，核心就会扣除成本并开始建造。

4. **看着它拔地而起**——建筑会伴随粒子效果**按实际时间逐块**放置。

   Right-click again to read the % progress.

核心将**附近村民中最高的声望等级**视为你的地位，因此与当地人交易就是解锁教授的方式。

### 建筑目录

| 建筑 | 要求      | 成本            |
| -- | ------- | ------------- |
| 小屋 | 信任度 2 级 | 32 个Nerosteel |
| 工坊 | 信任度 3 级 | 48 个Nerosteel |

随着村庄发展，建筑会按顺序教授。

## 运转中的村庄

一旦有了建筑，核心就会变成一台小型引擎：

- **产出**——已完成的建筑会定期产出物品（小屋 → 面包，工坊 → Nerosteel）。

  **潜行右键点击**核心可收取产出并查看当前任务。

- **任务**——村庄会发布一项收集任务（例&#x5982;_&#x5E26;来 8 个Xertz石英_）。手持所需物品右键点击核心 Right-click the core with

  the requested item to hand it in for **emeralds + a village-wide trust bump**; a new task rolls.

- **袭击**——夜间，玩家附近已被认领的村庄偶尔会遭到敌对生物袭击。

  用 `alienRaidsEnabled` 开关（参见\*\*[配置](Configuration.md)\*\*）。

## 交互速览

| 输入                | 结果               |
| ----------------- | ---------------- |
| 右键点击（空手，未被认领）     | 认领村庄             |
| 手持 Nerosteel块右键点击 | 存入建筑物资库          |
| 右键点击（所有者，空手）      | 教授／建造下一座建筑，或查看进度 |
| 潜行右键点击            | 收取产出 + 查看当前任务    |
| 手持任务物品右键点击        | 上交任务，换取绿宝石 + 信任度 |

## 详细信息

- ID：`nerospace:village_core` · 工具：镐 · 掉落：自身
- 保存所有者、库存、已建数量、当前的建造任务、产出缓存以及当前任务。
