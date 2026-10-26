# 陨石事件

坠落的陨石是一种**世界事件**：罕见的撞击会为主世界（以及各行星）带来外星材料和外星球矿石的起步资源——无需火箭就能初次体验外面的世界。

## 概述

Every so often, near an active player, a meteor is scheduled to fall. 每隔一段时间，就会在某位活跃玩家附近安排一颗陨石坠落。在短暂的预警期后，它会沿着炽热的弧线从天而降，撞入地面，在一个发光的\*\*[陨石核心](Meteor-Core.md)**——装有随机战利品的“箱子”——周围留下一个由**[陨石岩](Meteor-Rock.md)\*\*构成的小型陨石坑。撞击被有意设计得很温和（几格大小的陨石坑，没有大范围爆炸，也不会起火），因此不会破坏你的建筑。 The crash is deliberately modest (a few-block crater, no wide explosion, no fire) so it
won't grief your builds.

Impacts happen **as you get close** to the site: meteors are scheduled far out and only fall once the
area is loaded, so following the warning toward the site is part of the hunt.

## 陨石追踪器

**陨石追踪器**是一个预警指南针。手持它时，它会在动作栏中显示最近的被追踪陨石： While you hold it, it shows the nearest tracked
meteor in the action bar:

- **状态**——_来袭中_（仍在坠落／尚未着陆）&#x6216;_&#x5DF2;着陆_（陨石坑正等着你）。
- **方位**——指向撞击点的罗盘方向（N、NE、E……）。 toward the site.
- **距离**——以米为单位的距离。

你可以在生存模式中合成它（有序）：

```text
. N .
R C R
. N .
```

`N` = Nerosteel锭 · `R` = 红石 · `C` = 指南针

## Loot

Breaking the [Meteor Core](Meteor-Core) spills the meteor's contents (break-to-loot). The loot is
rolled **once** when the meteor lands and then fixed, so it's the same for everyone who reaches it —
no re-roll exploit. A meteor always carries a handful of **Alien Fragments**, plus weighted bonus
rolls of existing raw ores (Raw Nerosium, Raw Nerosteel, Xertz Quartz) and the rarer
**Alien Tech Scrap** and **Alien Core**.

| 物品         | Rarity   | 作用            |
| ---------- | -------- | ------------- |
| **外星碎片**   | 常见（必定掉落） | 未来**扫描仪**的原料。 |
| **外星科技废料** | 稀有度      | 未来的升级合成。      |
| **外星核心**   | 稀有       | 高价值的扫描仪／升级门槛。 |

The three are grouped under the `nerospace:alien_materials` item tag for future recipes. 这三种物品归入 `nerospace:alien_materials` 物品标签，供未来的配方使用。将它们转化为升级的扫描仪列在\*\*[路线图](Roadmap.md)**／**[未来功能](Future-Features.md)\*\*中。

## Calling a meteor (creative)

**陨石召唤器**是仅限创造模式的工具：右键点击任意方块，即可立即将一颗带有全新随机战利品的陨石召唤到该处——与自然生成走的是同一流程。它在生存模式中没有作用。 It does nothing in
survival.

## 配置

Meteor pacing is fixed. There are no meteor config keys. The fixed values are:

- 大约每 **9,000 秒**（2.5 小时）在某位活跃玩家附近自然出现一颗陨石；
- 坠落前有 **30 秒**预警；
- 着陆点距离 **200 到 500 格**；
- 每个维度同时最多 **4** 个陨石地点。

实际存在的配置键参见\*\*[配置](Configuration.md)\*\*。

## 详细信息

- 触发：玩家附近的稀有计时器（大约每 9,000 秒一次；固定）。
- 维度：主世界、Greenxertz、Cindara、Glacira（不包括虚空空间站）。
- 重力：在低[重力](Planet-Gravity.md)世界中，陨石下降得更慢。
- 方块：[陨石岩](Meteor-Rock.md)、[陨石核心](Meteor-Core.md) · 实体：`nerospace:falling_meteor`。
- 物品：`alien_fragment`、`alien_tech_scrap`、`alien_core`、`meteor_tracker`、`meteor_caller`。
