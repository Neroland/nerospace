# 补水模块

<!-- nerospace:render -->

<p align="right"><img src="images/hydration_module.gif" alt="Hydration Module" width="150" align="right"></p>
<!-- /nerospace:render -->

深度地形改造的 Glacite 进料口：将 Glacite 融化为**补水单位**，为[地形改造器](Terraformer.md)的**湿润**阶段（水）提供原料。

## 概述

Deeper terraforming advances in stages — Rooted → Hydrated → Living. The Hydrated stage fills
basins below the Terraformer's water table with real water, and every water source placed costs
**1 hydration unit**. The Hydration Module is where those units come from: it melts glacite
(crystallised water-ice mined on [Glacira](Home)) into the Terraformer's hydration buffer.

## Obtaining

**合成**（有序）：

```text
G N G
N F N
G N G
```

`G` = Glacite · `N` = Nerosteel锭 · `F` = [流体储罐](Fluid-Tank.md)

## How it works

- **Must touch the Terraformer:** place it directly against any face of a Terraformer block.

  即使只隔一格的模块也不会供给任何东西。

- \*\*为其供给 Glacite：\*\*将 Glacite（每个 16 单位）或 Glacite块（144 单位）放入其输入

  槽位——手动、漏斗或物品管道均可（该槽位对自动化开放）。

- **每次脉冲融化一个物品**到所连接地形改造器的补水缓存中（上限 1,024 单位）；

  它从不融化缓存无法完全容纳的物品，因此不会损失任何单位。

- \*\*没有 Glacite = 水阶段停滞。\*\*地形改造器界面（以及

  [地形改造监视器](Terraform-Monitor.md)）会在第 2 阶段等待时显示“需要 Glacite”。

## 详细信息

- ID：`nerospace:hydration_module` · 工具：镐，铁级 · 掉落：自身
- 输入物品由标签驱动（`nerospace:hydration_input`）——默认仅限 Glacite；整合包可以

  扩展它（原版冰被有意排除，以保持前往 Glacira 的意义）。
