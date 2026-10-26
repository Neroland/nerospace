# 地形改造器

<!-- nerospace:render -->

<p align="right"><img src="images/terraformer.gif" alt="Terraformer" width="150" align="right"></p>
<!-- /nerospace:render -->

终局机器：缓慢地将一颗荒芜的行星改造为宜居、可呼吸、**生机勃勃**的土地——分为三个可见的阶段。

## 概述

Place a Terraformer on a barren world, power it, and it advances an **ever-expanding circular frontier**
outward from itself. 在一个贫瘠的世界上放置地形改造器并为其供电，它会以自身为中心向外推进一道**不断扩张的圆形边界**。在深度地形改造中，机器会运行**三道边界**，每一道都紧随前一道，因此一个长期运行的世界总是呈现渐变——边缘是原始的化学反应，中心是生机勃勃的生态系统：

| 阶段 | 名称     | What it does                                                                 |
| -- | ------ | ---------------------------------------------------------------------------- |
| 1  | **扎根** | 草方块 + 泥土，永久可呼吸，鲜艳的霓虹色**改造生物群系**，零星植物                                         |
| 2  | **湿润** | 低于机器地下水位的洼地会被真正的水填满——通过[补水模块](Hydration-Module.md)消耗 **Glacite**             |
| 3  | **生机** | 生物群系稳定为自然的**行星专属配色**（草甸／热带草原／苔原），长出树木，出现**雨或雪**，以及该行星[牲畜](Creatures.md)的初始畜群 |

## Obtaining

**合成**（有序）：

```text
N D N
D O D
N B N
```

`N` = Nerosteel锭 · `D` = 泥土 · `O` = [氧气发生器](Oxygen-Generator.md) · `B` = Nerosteel块

## How it works

- \*\*电网电力：\*\*只使用管道输送的能量——连接一条输送 FE 的通用管道。其 Its

  100,000 FE 缓存会在工作时消耗，每刻最多接收 **2,000 FE**（等级越高／电力越多 =
  越快）。它每刻最多改造 **48 列**（为保护服务器性能而设定的固定上限）。 It converts at most **48 columns per tick** (a fixed limit to protect server speed).

- \*\*扩张的边界：\*\*每个工作周期，它会为每个阶段改造一圈地表列，然后扩大

  that stage's radius — **uncapped**, but energy-throttled. Stage 1 keeps priority (breathable ground
  never waits); the trailing stages take smaller shares and always stay inside the stage ahead.
  Stage-2 columns cost **2×** energy plus **1 hydration unit per water source**; stage-3 columns cost
  **4×** energy.

- \*\*水（湿润）：\*\*地下水位位于机器底部下方一格。其下方的洼地会被填满到 Basins under it fill

  flush with the ground; hills stay dry; chasms deeper than the cap are skipped. No glacite in the
  buffer = stage 2 stalls (the GUI says "Needs glacite"). On Glacira the lakes refreeze naturally.

- **生命（生机）：**成熟的生物群系会开启真实的**天气**（下雨；Glacira 上会积雪），

  零星**长成的树木**（按行星分别为橡树／白桦、金合欢、云杉），并放置该行星牲畜的初始配对
  （有数量上限）。

- **重力：**改造后的地面会恢复**正常的、类似地球的[重力](Planet-Gravity.md)**——因此

  改造一颗低重力卫星会让你转化的土地感觉像家一样（如果整合包设置了显式的生物群系
  重力标签，仍以其为准）。

- \*\*装饰性演变：\*\*已稳定的土地会自行不断长出零星的地被植物——纯粹的点缀，

  只占用很小的固定预算。

- \*\*等级：\*\*将升级物品放入升级槽位——**Nerosteel锭 → 2 级**，\*\*Cindrite →

  Tier 3\*\*. Higher tiers convert more columns per cycle; **Tier 3** also seeds a low rate of ore into
  the converted subsurface (Nerospace ores).

- \*\*延迟／补完：\*\*未加载区块中的列会被跳过，待你探索到那里时再改造——

  所有阶段都会在区块加载时重放，因此一颗经过改造的行星会随着你的行走而完成改造。默认情况下
  它不会强制加载区块。设置 `terraformerForceLoadEnabled = true` 可在其运行时保持一小片
  区块处于加载状态（会消耗服务器内存和性能）。 By default
  it does not force-load chunks. Set `terraformerForceLoadEnabled = true` to keep a small window of
  chunks loaded while it runs (costs server memory and speed).

- \*\*旧世界：\*\*现有的地形改造器会照常工作——它们的土地算作阶段 1，

  一旦你建造了新方块，新的阶段就会直接覆盖过去。无需迁移。 No migration.

## 详细信息

- ID：`nerospace:terraformer` · 工具：镐，铁级 · 掉落：自身

- 配套设备：[补水模块](Hydration-Module.md)（Glacite 进料口，必须接触）、

  [地形改造监视器](Terraform-Monitor.md)（阶段读数 + 比较器）

- 配置：只有 `terraformerForceLoadEnabled`（默认 `false`）。速度和能量也遵循 Speed and energy also follow the

  global multipliers. See [Configuration](Configuration).
