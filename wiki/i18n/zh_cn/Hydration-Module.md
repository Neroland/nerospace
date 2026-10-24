# 补水模块

> 本页为机器翻译草稿，仅供参考，以英文版本为准：[English](../../Hydration-Module.md)。

<!-- nerospace:render -->
<p align="right"><img src="../../images/hydration_module.gif" alt="Hydration Module" width="150" align="right"></p>
<!-- /nerospace:render -->

深度地形改造的 Glacite 进料口：将 Glacite 融化为**补水单位**，为[地形改造器](Terraformer.md)的**湿润**阶段（水）提供原料。

## 概述

深度地形改造分阶段推进——扎根 → 湿润 → 生机。湿润阶段会用真正的水填满地形改造器地下水位以下的洼地，每放置一个水源都要消耗 **1 个补水单位**。补水模块就是这些单位的来源：它将 Glacite（在 [Glacira](Home.md) 上开采的结晶水冰）融化进地形改造器的补水缓存中。

## 获取方式

**合成**（有序）：

```text
G N G
N F N
G N G
```

`G` = Glacite · `N` = Nerosteel锭 · `F` = [流体储罐](Fluid-Tank.md)

## 工作原理

- **必须与地形改造器接触：**将其直接贴在地形改造器方块的任意一面上。

  即使只隔一格的模块也不会供给任何东西。

- **为其供给 Glacite：**将 Glacite（每个 16 单位）或 Glacite块（144 单位）放入其输入

  槽位——手动、漏斗或物品管道均可（该槽位对自动化开放）。

- **每次脉冲融化一个物品**到所连接地形改造器的补水缓存中（上限 1,024 单位）；

  它从不融化缓存无法完全容纳的物品，因此不会损失任何单位。

- **没有 Glacite = 水阶段停滞。**地形改造器界面（以及

  [地形改造监视器](Terraform-Monitor.md)）会在第 2 阶段等待时显示“需要 Glacite”。

## 详细信息

- ID：`nerospace:hydration_module` · 工具：镐，铁级 · 掉落：自身
- 输入物品由标签驱动（`nerospace:hydration_input`）——默认仅限 Glacite；整合包可以

  扩展它（原版冰被有意排除，以保持前往 Glacira 的意义）。
