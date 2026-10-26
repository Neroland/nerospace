# 发射控制器

<!-- nerospace:render -->

<p align="right"><img src="images/launch_controller.png" alt="Launch Controller" width="150" align="right"></p>
<!-- /nerospace:render -->

一台能建造发射台并驾驶火箭的任务控制台。

## 概述

The Launch Controller is a solid **3-wide × 2-tall** console — a dark gunmetal chassis with a glowing,
animated "computer brain" face (a live screen, projector arms, and a pulsing light band). It does two
jobs from one block: **build** a launch pad from loaded materials, and **fuel + launch** a rocket parked
on that pad. Place it facing an open area; it builds and oversees the pad in front of it.

## Obtaining

**合成**（有序）：

```text
W R W
P C P
W R W
```

`W` = [空间站墙壁](Station-Wall.md) · `P` = [火箭发射台](Rocket-Launch-Pad.md) ·
`R` = 红石 · `C` = 红石比较器

Placing it forms the full 3×2 structure (it needs the room — placement is cancelled if blocked).
Breaking any part removes the whole console and drops one controller.

## How it works

界面有两种模式（在右上角切换）：

**建造模式**

- 将建筑方块装入三个槽位：**发射台**、**空间站墙壁**、**发射塔架**。
- Pick a target **tier (T1–T4)**. The footprint grows per tier — T1 single pad, T2 a 3×3, T3 a
  3×3 ringed with Station Wall, T4 a Heavy Launch Complex with a gantry.
- **切换全息投影**会从控制台的投影臂投射出仍需放置&#x7684;_&#x5B9E;际方&#x5757;_&#x7684;虚影。
- **建造发射台**以**累加**方式在世界中铺设结构——将已有的低等级发射台升级为更高等级而无需拆除。它只会放置你已装载的方块。 It only places blocks you've loaded.
- 实时读数显示仍需多少发射台／墙壁／塔架，以及控制器机载的**燃料／O₂／电力**水平。

**发射模式**

- The controller carries onboard **Fuel**, **Oxygen**, and **Power** buffers — feed them with
  [Universal Pipes](Universal-Pipe) (fluid / gas / energy layers). It pumps them into a rocket docked
  on the pad.
- 读取所停靠火箭的燃料／氧气／电力，选择其目的地，然后直接从控制台**发射**（火箭起飞时你会登舱）。

## 详细信息

- ID：`nerospace:launch_controller`
- 一个 3×2 多方块结构；填充方块是实心的（你无法穿过控制台）。
- 能力（流体／气体／能量）暴露在核心方块上，供管道自动化使用。
