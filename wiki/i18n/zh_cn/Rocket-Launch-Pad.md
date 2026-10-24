# 火箭发射台

<!-- nerospace:render -->

<p align="right"><img src="images/rocket_launch_pad.png" alt="Rocket Launch Pad" width="150" align="right"></p>
<!-- /nerospace:render -->

火箭发射前部署的底座——同时也是安全的着陆区。

## 概述

Deploy a **Rocket** (right-click a launch pad with a rocket item) and it appears on the pad, ready to
fuel and fly. The pad also acts as a small **breathable safe zone** so arrivals aren't immediately
punished by the airless atmosphere.

## Obtaining

**合成**（有序）：

```text
N N N
N B N
N N N
```

`N` = Nerosteel锭 · `B` = [Nerosium块](Block-of-Nerosium.md)

## How it works

- \*\*部署：\*\*手持火箭物品右键点击任意发射台方块，即可将火箭放在上面。发射台尺寸会 The pad size is

  checked **at launch**. If the pad is too small for the rocket's tier, the launch is blocked with a
  clear message. Breaking pad blocks under a deployed rocket can ground it.

- **各等级所需的发射台尺寸**（更大的发射台总是适用于更小的火箭）：
  - **1 级**——一个发射台方块。

  - **Tier 2** — a full, aligned 3×3 pad.

  - **3 级**——一个**由[空间站墙壁](Station-Wall.md)围绕**的 3×3 发射台（在发射台高度上，

    周围 5×5 的 16 格边框），或一个[重型发射综合设施](Launch-Gantry.md)。

  - **4 级**——仅限重型发射综合设施。

- **各等级火箭的数据：**

  | Tier | 燃料箱       | 机载氧气      | Onboard power |
  | ---- | --------- | --------- | ------------- |
  | 1    | 3,000 mB  | 2,000 mB  | 4,000 FE      |
  | 2    | 6,000 mB  | 4,000 mB  | 8,000 FE      |
  | 3    | 12,000 mB | 8,000 mB  | 16,000 FE     |
  | 4    | 24,000 mB | 16,000 mB | 30,000 FE     |

- **Fuel per launch** is the same for every tier. It depends on the trip:

  - 前往**另一个维度**的行程消耗 **2,500 mB**（500 基础 + 2,000）；
  - a hop **inside the same dimension** costs **500 mB + 60 mB per 100 blocks**.

  Both scale with `fuelCostMultiplier`. See [Configuration](Configuration).

- \*\*提示：\*\*1 级火箭抵达空间站时只剩约 **500 mB**。请至少携带 Carry at least

  **2 个火箭燃料罐**，以便飞回家。

- \*\*重型发射综合设施：\*\*一个完整、对齐的 **5×5 发射台**，外加至少一个在发射台高度相邻的

  **[Launch Gantry](Launch-Gantry)** module adjacent at pad level. Right-click the gantry to board
  the rocket; empty-hand right-click any pad block for a **formation report** (cluster size, largest
  square, modules present, next missing piece). See [Launch Gantry](Launch-Gantry).

- **Multiblock:** pads placed adjacently form a cluster. A full, aligned **3×3 pad** lets a

  [燃料储罐](Fuel-Tank.md)的自动加注速度快 **4 倍**——重型综合设施则快 **12 倍**（480 mB/t）。

- \*\*自动化代理：\*\*当火箭停在这组发射台上时，每个发射台方块都会将火箭的

  **燃料进口槽位**暴露为物品能力——将漏斗或[通用管道](Universal-Pipe.md)
  （物品层）对准任意发射台方块，即可送入火箭燃料桶／燃料罐（并取出空
  桶）。在 3×3 发射台上使用燃料储罐 + 管道即可让发射准备完全自动化。 A Fuel Tank + pipes on a 3×3 pad fully automates launch prep.

- \*\*安全区：\*\*在发射台几格范围内你可以呼吸，因此不会在着陆的瞬间

  land. 窒息。着陆区之外的区域请建造[氧气发生器](Oxygen-Generator.md)基地。

> **想建造一个？**[发射台建造指南](Launch-Pad-Build-Guide.md)提供了每个等级的布局规划和
> 材料清单。

## Pad-to-pad travel

Launch pads double as the waypoints of rocket travel.

- **Register a pad:** right-click a pad with a **Name Tag** to commission it as a named travel node
  (the tag's name labels it, or "Pad N"). Registered pads become landing targets.
- **选择着陆点：**在火箭界面中选择目的地（家园／某颗行星／轨道空间站），然后切换**发射台选择器**，降落在该处某个特定的已注册发射台上——或选&#x62E9;_&#x6700;&#x8FD1;_&#x81EA;动挑选。你也可以在**同一维度内的发射台之间**短途飞行。 You can also hop **pad-to-pad within the same dimension**.
- **计算燃料：**一次发射会消耗基础成本加上**距离**成本（同一世界内的短途飞行）或固定的**跨维度附加费**。成本会在控制台中实时显示；在燃料箱足以支付之前，发射会被阻止。 The cost is shown live in the console; the launch is blocked until the
  tank covers it.
- **返回家园：**火箭会记住它从哪个发射台出发，因此飞回时会降落在**那个确切的发射台**上。只有当目的地确实没有发射台时，才会自动建造一个**着陆舱**。 A **Landing Pod** is only auto-built when the destination genuinely has no launch pad.

## 详细信息

- ID：`nerospace:rocket_launch_pad` · 工具：镐，铁级 · 掉落：自身
