# 外星村民

Nerospace 行星上的群居外星人——你需要赢得这些警惕的流浪者的信任，才能解锁交易并发展村庄。

## 概述

**外星村民**（`nerospace:alien_villager`）是一种和平的、**警惕中立**的 NPC：它在自己的家园生物群系中漫步，注视着你，如果你靠得太近它会慢慢退开，但从不攻击。赢得它的信任后，它会成为商人，也是整个村庄系统的关键——交易、**[村庄核心](Village-Core.md)**以及外星**[建筑](Alien-Structures.md)**。 Earn its trust and it
becomes a merchant and the key to the whole village system — trading, the
**[Village Core](Village-Core)**, and the alien **[structures](Alien-Structures)**.

循环：_发现聚落 → 赢得信任 → 解锁交易 → 教村庄建设 → 它发展成一台引擎 → 更深层的建筑（以及一个 Boss）开放。_

## Where they live

- 在 **Greenxertz**（以及成熟的 `terraformed_meadow`）上自然生成。
- 在 **Cindara** 和 **Glacira** 上也会生成稀疏的群体。
- 创造模式：**外星村民刷怪蛋**（Nerospace 标签页）。

## How they look

每个村民在视觉上都独一无二，而无需为每种外观单独设置实体——采用分层、由种子驱动的外观：

- **个体色调**——储存的颜色种子让每个村民的色调略有不同，因此没有

  two are identical. 两个完全相同。这是确定性的：同一个村民的外观始终不变。

- **行星皮肤**——Greenxertz 为绿色／钢色，Cindara 为余烬色／红色，Glacira 为霜白色／浅色。在 Within

  Greenxertz 中，成熟草甸上的村民佩戴更浅色的配饰。

- **心情暖色**——随着你在某个村庄的声望提升，其村民的色调会变暖，因此你可以

  一眼看出信任度。

- **发光的眼睛和肩部水晶**——自发光，在黑暗中可见。

## 赢得信任（声望）

每个村民都会追踪**每位玩家的声望分数（0–100）**，对应 **6 个等级**：

| Tier | 名称         | 解锁                   |
| ---- | ---------- | -------------------- |
| T0   | 陌生人        | 无——拒绝交易（警惕地摇头）       |
| T1   | Acquainted | 基础交易                 |
| T2   | 信任         | 中级交易 · 可教授 **1 级建筑** |
| T3   | 盟友         | 稀有交易 · **2 级建筑**     |
| T4   | 尊贵         | 专属装备交易               |
| T5   | Kin        | 最佳交易                 |

Reputation rises through:

- **礼物**——手持一件珍贵物品（**Xertz石英**、**Nerosium锭**、

  **Alien Fragment**, or **Emeralds**). It takes one, with happy-villager particles.

- **交易量**——每完成一次交易都会小幅提升信任度。

- **任务**——向\*\*[村庄核心](Village-Core.md)\*\*上交村庄任务，会提升附近每个

  nearby villager.

声望仅以 Minecraft 玩家 UUID 为键——不会向任何地方发送名称或互动记录（符合 POPIA/GDPR）。

## 交易

Right-click a villager at **tier 1+** to open the **vanilla trading screen**. Offers are tier-gated and
**cumulative** (a higher-trust villager also offers the lower tiers). Emeralds are the currency, so the
goods are useful in any modpack.

| Tier | 交易示例                                                                 |
| ---- | -------------------------------------------------------------------- |
| T1   | 出售 12 个Xertz石英 → 1 颗绿宝石 · 1 颗绿宝石 → 3 个铁 · 2 颗绿宝石 → 6 个面包             |
| T2   | 4 颗绿宝石 → Nerosium锭 · 1 颗绿宝石 + 8 个粗Nerosteel → 4 个Nerosteel锭 · 出售外星碎片 |
| T3   | 8 颗绿宝石 → 钻石 · 5 颗绿宝石 → 火箭燃料罐                                         |
| T4   | 12 颗绿宝石 + 2 个外星科技废料 → 外星核心 · 16 颗绿宝石 → **[Xertz共振器](Alien-Gear.md)** |
| T5   | 18 颗绿宝石 → 3 颗钻石 · 外星核心 + 24 颗绿宝石 → **[重力行者靴](Alien-Gear.md)**        |

最高等级是专属\*\*[工匠装备](Alien-Gear.md)\*\*唯一的交易来源。

## 详细信息

- ID：`nerospace:alien_villager` · 类别 CREATURE · 尺寸 0.6 × 1.95
- 变体（行星／家园生物群系／颜色种子）会同步并保存；声望按村民分别保存。
- 声音目前沿用原版村民的音效。

另请参阅：**[村庄核心](Village-Core.md)** · **[外星建筑](Alien-Structures.md)** · **[外星装备](Alien-Gear.md)** · **[生物](Creatures.md)**。
